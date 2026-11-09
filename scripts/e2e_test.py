# -*- coding: utf-8 -*-
"""
端到端业务闭环验证脚本（针对隔离测试库 selection_test，默认端口 8081）。
覆盖：意向填报、资格校验、面试录入、初选、名额、学生确认、补录、递补、调剂、公示锁定、审计与权限。
运行前请确保后端以 DB_NAME=selection_test SERVER_PORT=8081 启动。
"""
import json
import sys
import urllib.error
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8081"
PASSWORD = "123456"
passed = 0


def call(method, path, token=None, body=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        payload = exc.read().decode("utf-8")
        try:
            return exc.code, json.loads(payload)
        except Exception:
            return exc.code, {"message": payload}


def ok(method, path, token=None, body=None):
    status, data = call(method, path, token, body)
    assert status == 200 and data.get("code") == 0, f"{method} {path} -> {status} {data}"
    return data.get("data")


def expect_error(method, path, token=None, body=None, code=None):
    status, data = call(method, path, token, body)
    assert status >= 400, f"{method} {path} expected error, got {status} {data}"
    if code is not None:
        assert data.get("code") == code, f"{method} {path} expected code {code}, got {data}"
    return data


def login(account):
    data = ok("POST", "/api/auth/login", body={"account": account, "password": PASSWORD})
    return data["token"]


def step(name):
    global passed
    passed += 1
    print(f"[{passed:02d}] {name}")


print("== 登录演示账号 ==")
admin = login("admin")
student1 = login("S2023001")
student7 = login("S2023007")
student8 = login("S2023008")
student9 = login("S2023009")
student10 = login("S2023010")
student13 = login("S2023013")
mentor1 = login("t1001")
mentor2 = login("t2002")  # 王强? t2002 是赵敏；见下方注释
mentor3 = login("t2001")
mentor6 = login("t3002")
step("学生 / 导师 / 管理员账号登录成功")

home = ok("GET", "/api/public/home")
assert home["batch"]["status"] == "INTENT_OPEN"
assert len(home["labs"]) == 3
assert home["announcements"], "公开公告应可见"
step("公开首页返回批次、3 个实训室与公告")

# 资格校验：2024 级学生不在本批次
expect_error("POST", "/api/student/application/submit", student13,
             {"mentorIds": [1], "intro": "test", "adjustmentPreference": "NONE"}, 40012)
step("非本批次年级学生被资格校验拦截")

# 最多 3 个意向
expect_error("POST", "/api/student/application/submit", student10,
             {"mentorIds": [1, 2, 3, 4], "intro": "test", "adjustmentPreference": "CROSS"}, 40013)
step("超过 3 个平行意向被拒绝")

# 学生 8 提交 3 个意向
app8 = ok("POST", "/api/student/application/submit", student8, {
    "mentorIds": [1, 3, 5],
    "intro": "人工智能专业，关注边缘智能与大模型应用。",
    "skills": "Python、PyTorch、Linux",
    "projectExperience": "课程目标检测项目",
    "adjustmentPreference": "CROSS"
})
assert app8["status"] == "SUBMITTED" and len(app8["intents"]) == 3
app8_id = app8["id"]
step(f"学生王思远提交 3 个平行意向（application #{app8_id}）")

# 导师端数据隔离：t1001 只看到自己的意向
applicant = ok("GET", f"/api/mentor/applicants/{app8_id}", mentor1)
assert applicant["intent"]["mentorId"] == 1
assert applicant["otherIntentCount"] == 2
assert "intents" not in applicant or applicant.get("intents") is None
step("导师详情只返回本人意向，其他平行意向仅显示数量")

# 进入面试阶段
ok("POST", "/api/admin/batches/1/stage", admin, {"status": "INTERVIEW", "reason": "意向填报截止，进入面试"})
step("管理员将批次推进到面试与初选阶段")

# 面试录入：t1001 为王思远、林晓、陈雨桐记录 lab1 面试
for app_id, score in [(app8_id, 92), (1, 88), (3, 80)]:
    ok("POST", "/api/mentor/interviews", mentor1, {
        "applicationId": app_id, "scheduledAt": "2026-11-13 09:00:00",
        "location": "信息楼 A301", "score": score,
        "comment": "面试表现良好", "conclusion": "PASS"
    })
# t2001 为林晓记录 lab2 面试；t3002 为郑可记录 lab3 面试
ok("POST", "/api/mentor/interviews", mentor3, {
    "applicationId": 1, "scheduledAt": "2026-11-13 14:00:00",
    "location": "信息楼 B402", "score": 85, "comment": "基础扎实", "conclusion": "PASS"
})
ok("POST", "/api/mentor/interviews", mentor6, {
    "applicationId": 7, "scheduledAt": "2026-11-14 10:00:00",
    "location": "创意楼 C205", "score": 79, "comment": "作品完整", "conclusion": "PASS"
})
step("三个实训室完成面试记录")

# 初选：王思远 + 林晓获得 lab1 拟录取，林晓再获 lab2 拟录取；郑可候补
ok("POST", "/api/mentor/decisions", mentor1,
   {"intentId": applicant["intent"]["id"], "decision": "OFFER", "comment": "推荐录取"})
ok("POST", "/api/mentor/decisions", mentor1,
   {"intentId": 1, "decision": "OFFER", "comment": "算法基础好"})
ok("POST", "/api/mentor/decisions", mentor3,
   {"intentId": 2, "decision": "OFFER", "comment": "项目经历匹配"})
ok("POST", "/api/mentor/decisions", mentor6,
   {"intentId": 14, "decision": "WAITLIST", "waitlistRank": 1, "comment": "候补第一顺位"})
step("导师提交拟录取与候补顺位")

# 名额限制：把张明名额调到 2，再给陈雨桐发拟录取应失败
mentor_page = ok("GET", "/api/admin/mentors?page=1&size=50", admin)
m1 = next(m for m in mentor_page["records"] if m["id"] == 1)
ok("PUT", "/api/admin/mentors/1", admin, {
    "name": m1["name"], "title": m1["title"], "labId": m1["labId"],
    "researchDirection": m1["researchDirection"], "profile": m1["profile"],
    "quota": 2, "enabled": True
})
expect_error("POST", "/api/mentor/decisions", mentor1,
             {"intentId": 7, "decision": "OFFER", "comment": "尝试超额"}, 40015)
ok("PUT", "/api/admin/mentors/1", admin, {
    "name": m1["name"], "title": m1["title"], "labId": m1["labId"],
    "researchDirection": m1["researchDirection"], "profile": m1["profile"],
    "quota": 3, "enabled": True
})
step("导师名额上限校验生效（超额拟录取被拒绝）")

# 进入确认阶段：唯一拟录取自动确认，多拟录取等待学生选择
ok("POST", "/api/admin/batches/1/stage", admin, {"status": "CONFIRM", "reason": "面试初选完成"})
result8 = ok("GET", "/api/student/result", student8)
assert result8["assignment"] is not None, "唯一拟录取应自动确认"
assert result8["assignment"]["mentorId"] == 1
step("唯一拟录取在进入确认阶段后自动确认")

offers1 = ok("GET", "/api/student/offers", student1)
active1 = [o for o in offers1 if o["status"] == "OFFERED"]
assert len(active1) == 2, f"林晓应有 2 个待确认拟录取，实际 {len(active1)}"
chosen = active1[0]
ok("POST", f"/api/student/offers/{chosen['id']}/confirm", student1)
offers1_after = ok("GET", "/api/student/offers", student1)
assert len([o for o in offers1_after if o["status"] == "CONFIRMED"]) == 1
assert any(o["status"] == "RELEASED" for o in offers1_after)
step("多个拟录取意向由学生自主确认其一，其余自动释放")

# 公示锁尚未发生；逾期处理接口可执行
expired = ok("POST", "/api/admin/review/expire-offers?batchId=1&reason=test-expire", admin)
assert expired["count"] == 0
step("逾期拟录取处理接口正常（当前无逾期记录）")

# 补录阶段：自动建轮次、开放、候补递补、补录填报与确认
ok("POST", "/api/admin/batches/1/stage", admin, {"status": "SUPPLEMENT", "reason": "进入补录"})
rounds = ok("GET", "/api/admin/supplement/rounds?batchId=1", admin)
assert len(rounds) == 1
round_id = rounds[0]["id"]
ok("POST", f"/api/admin/supplement/rounds/{round_id}/open", admin)
promoted = ok("POST", f"/api/admin/supplement/rounds/{round_id}/promote", admin)
assert promoted["promotedCount"] == 1, promoted
offers7 = ok("GET", "/api/student/offers", student7)
offer7 = next(o for o in offers7 if o["status"] == "OFFERED")
ok("POST", f"/api/student/offers/{offer7['id']}/confirm", student7)
step("候补顺位递补：郑可由候补转为拟录取并确认")

state9 = ok("GET", "/api/student/supplement", student9)
supp = ok("POST", "/api/student/supplement/intents", student9,
          {"roundId": round_id, "mentorIds": [2, 4]})
assert len(supp["intents"]) == 2
mentor_supp = ok("GET", "/api/mentor/supplement", mentor2)
target = next(i for i in mentor_supp["intents"] if i["studentId"] == 9)
ok("POST", "/api/mentor/supplement/decisions", mentor2,
   {"supplementIntentId": target["id"], "decision": "OFFER", "comment": "补录面试通过"})
offers9 = ok("GET", "/api/student/offers", student9)
offer9 = next(o for o in offers9 if o["status"] == "OFFERED")
ok("POST", f"/api/student/offers/{offer9['id']}/confirm", student9)
closed = ok("POST", f"/api/admin/supplement/rounds/{round_id}/close", admin)
step("补录轮次：重新填报、导师发拟录取、学生确认、轮次结束")

# 调剂阶段：自动统筹 + 谈话安置
ok("POST", "/api/admin/batches/1/stage", admin, {"status": "ADJUSTMENT", "reason": "进入统筹调剂"})
auto = ok("POST", "/api/admin/adjustments/auto", admin)
assert auto["assignedCount"] + auto["talkedCount"] > 0
workspace = ok("GET", "/api/admin/adjustments/workspace", admin)
if workspace["unadmitted"]:
    row = workspace["unadmitted"][0]
    ok("POST", f"/api/admin/adjustments/{row['application']['studentId']}/talk?reason=test-talk", admin)
step(f"统筹调剂执行成功：分配 {auto['assignedCount']} 人，谈话 {auto['talkedCount']} 人")

# 公示：生成草稿、发布、公开查询、锁定校验、关闭
ok("POST", "/api/admin/batches/1/stage", admin, {"status": "PUBLICITY", "reason": "进入公示"})
publicities = ok("GET", "/api/admin/publicities?batchId=1", admin)
assert publicities, "进入公示阶段应自动生成草稿"
publicity_id = publicities[0]["id"]
published = ok("POST", f"/api/admin/publicities/{publicity_id}/publish", admin)
assert published["status"] == "PUBLISHED"
public_result = ok("GET", "/api/public/publicity?batchId=1")
assert len(public_result["assignments"]) > 0
expect_error("POST", "/api/admin/review/expire-offers?batchId=1", admin, code=40020)
step("公示发布后公开名单可查，结果锁定禁止修改")

closed_pub = ok("POST", f"/api/admin/publicities/{publicity_id}/close", admin)
assert closed_pub["status"] == "CLOSED"
final_batch = ok("GET", f"/api/admin/batches/1", admin)["batch"]
assert final_batch["status"] == "FINISHED"
step("公示结束：批次完成、最终结果锁定")

# 审计与权限
logs = ok("GET", "/api/admin/audit-logs?batchId=1&page=1&size=100", admin)
actions = {log["action"] for log in logs["records"]}
for expected in ["APPLICATION_SUBMIT", "INTERVIEW_SAVE", "OFFER_CREATE", "OFFER_CONFIRM",
                 "ADJUSTMENT_AUTO", "PUBLICITY_PUBLISH"]:
    assert expected in actions, f"审计日志缺少 {expected}: {sorted(actions)}"
expect_error("GET", "/api/admin/dashboard", student1, code=40301)
step("关键操作写入审计日志，学生越权访问管理端被拒绝")

# 批次完成后，学生仍应能查看本批次结果，且结果包含培养方向/培养安排
result8_final = ok("GET", "/api/student/result", student8)
assert result8_final["batch"]["status"] == "FINISHED", result8_final["batch"]
assert result8_final["assignment"] is not None
assert result8_final["assignment"].get("labDirection"), result8_final["assignment"]
step("批次完成后仍可查看本批次结果与培养方向")

print(f"\nALL PASSED: {passed} checks")
