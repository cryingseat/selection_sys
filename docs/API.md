# 接口文档

基础地址：`http://127.0.0.1:8080/api`  
除登录、公开接口和健康检查外，均需请求头：

```http
Authorization: Bearer <token>
Content-Type: application/json
```

## 统一响应

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1794215478063
}
```

- `code = 0` 表示成功；非 0 为业务错误码；
- HTTP 状态码同时表达语义：200 成功、400 业务/参数错误、401 未登录、403 无权限、404 不存在、409 冲突、500 未处理异常；
- 时间格式统一为 `yyyy-MM-dd HH:mm:ss`。

## 错误码

| code | HTTP | 含义 |
| --- | --- | --- |
| 40001 | 400 | 请求参数不合法 |
| 40010 | 400 | 当前阶段不允许该操作 |
| 40011 | 400 | 不在规定时间窗口 |
| 40012 | 400 | 不符合本批次报名资格 |
| 40013 | 400 | 超过 3 个平行意向 |
| 40014 | 400 | 重复意向 |
| 40015 | 400 | 导师/实训室名额不足 |
| 40016 | 400 | 缺少已完成的面试记录 |
| 40017 | 400 | 已确认其他实训室 |
| 40018 | 400 | 确认状态冲突 |
| 40019 | 400 | 数据状态不允许该操作 |
| 40020 | 400 | 公示锁定 |
| 40021 | 400 | 调剂未授权/拒绝实训室 |
| 40022 | 400 | 确认逾期 |
| 40023 | 400 | 重复报名 |
| 40024 | 400 | 拟录取记录失效 |
| 40025 | 400 | 补录轮次未开放 |
| 40027 | 400 | 账号或密码错误 |
| 40101 | 401 | 未登录/登录过期 |
| 40301 | 403 | 无权限 |
| 40401 | 404 | 数据不存在 |
| 50001 | 500 | 服务器内部错误 |

---

## 一、认证

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| POST | `/auth/login` | 公开 | 登录，返回 token 与用户信息 |
| POST | `/auth/logout` | 登录 | 撤销当前 token |
| GET | `/auth/me` | 登录 | 获取当前用户信息 |

登录请求：

```json
{
  "account": "S2023001",
  "password": "123456"
}
```

登录响应 `data`：

```json
{
  "token": "rL5ljoRm...",
  "user": {
    "id": 8,
    "account": "S2023001",
    "displayName": "林晓",
    "role": "STUDENT",
    "profile": { "id": 1, "studentNo": "S2023001", "major": "计算机科学与技术" }
  }
}
```

---

## 二、公开接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/public/home` | 当前批次、8 步流程、实训室与导师、公告、统计、公示 |
| GET | `/public/labs` | 当前批次实训室列表（含导师与名额统计） |
| GET | `/public/labs/{id}` | 实训室详情 |
| GET | `/public/announcements` | 已发布且在展示期内的公告 |
| GET | `/public/publicity?batchId=` | 已发布/已结束的公示与最终名单 |
| GET | `/health` | 健康检查 |

`/public/home` 响应包含：

- `batch`：批次与阶段时间；
- `stages`：8 步流程及 `done/active/upcoming` 状态；
- `labs`：实训室、导师、名额、报名数；
- `announcements`：公告；
- `publicity` / `publicAssignments`：仅在公示已发布/结束时返回；
- `stats`：实训室数、导师数、总名额、已录取数。

---

## 三、学生端

角色：`STUDENT`。所有“本人”数据由 token 推导，不接受前端传学生 ID。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/student/dashboard` | 工作台聚合：批次、阶段、报名、面试、拟录取、最终去向、公示 |
| GET | `/student/application` | 当前批次报名（含意向） |
| POST | `/student/application/draft` | 保存草稿 |
| POST | `/student/application/submit` | 提交/重新提交报名 |
| POST | `/student/application/withdraw` | 撤回报名（开放期内） |
| GET | `/student/interviews` | 本人面试记录 |
| GET | `/student/offers` | 本人拟录取记录（确认阶段会触发唯一拟录取自动确认） |
| POST | `/student/offers/{id}/confirm?reason=` | 确认某个拟录取 |
| POST | `/student/offers/{id}/decline?reason=` | 放弃某个拟录取 |
| POST | `/student/offers/decline-all?reason=` | 放弃全部拟录取 |
| GET | `/student/result` | 最终结果、公示、调剂记录 |
| GET | `/student/history` | 历史批次报名与最终去向 |
| GET | `/student/supplement` | 补录轮次、补录意向、补录拟录取 |
| POST | `/student/supplement/intents` | 提交补录意向 |

报名请求：

```json
{
  "mentorIds": [1, 3, 5],
  "intro": "个人简介",
  "skills": "技术基础",
  "projectExperience": "项目经历",
  "adjustmentPreference": "CROSS",
  "excludedLabIds": [2],
  "version": 0
}
```

- `mentorIds`：1—3 个，不能重复；
- `adjustmentPreference`：`INTERNAL` / `CROSS` / `NONE`；
- `excludedLabIds`：明确拒绝调剂的实训室；
- `version`：更新时传当前版本，用于乐观锁。

补录意向：

```json
{
  "roundId": 1,
  "mentorIds": [2, 4]
}
```

---

## 四、导师端

角色：`MENTOR`。服务端会校验数据是否属于当前导师/本实训室；导师端不返回学生其他平行意向的具体导师。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/mentor/dashboard` | 工作台统计、待处理报名、阶段 |
| GET | `/mentor/applicants?status=&keyword=&page=&size=` | 报名学生分页 |
| GET | `/mentor/applicants/{applicationId}` | 学生详情（仅本人意向 + 其他意向数量） |
| GET | `/mentor/interviews` | 本实训室面试记录 |
| POST | `/mentor/interviews` | 新建/更新面试记录 |
| GET | `/mentor/decisions` | 初选总览与名额 |
| POST | `/mentor/decisions` | 提交拟录取/候补/不录取 |
| GET | `/mentor/offers` | 本人发放的拟录取 |
| GET | `/mentor/students` | 最终学生名单 |
| GET | `/mentor/supplement` | 补录阶段填报本人的学生与轮次 |
| POST | `/mentor/supplement/decisions` | 补录结论 |

面试请求：

```json
{
  "applicationId": 17,
  "scheduledAt": "2026-11-13 09:00:00",
  "location": "信息楼 A301",
  "score": 92,
  "comment": "算法基础扎实，项目描述清晰。",
  "conclusion": "PASS"
}
```

- `conclusion`：`PENDING`（仅安排）/ `PASS` / `FAIL`；
- 提交 `PASS/FAIL` 时必须填写评分；
- 服务端状态：`PENDING` → `SCHEDULED`，`PASS/FAIL` → `COMPLETED`。

初选请求：

```json
{
  "intentId": 1,
  "decision": "OFFER",
  "waitlistRank": null,
  "comment": "推荐录取",
  "reason": "面试表现优秀"
}
```

- `decision`：`OFFER` / `WAITLIST` / `REJECT`；
- `WAITLIST` 必须提供大于 0 的 `waitlistRank`，同一导师下不能重复；
- `OFFER` 要求该实训室面试已完成，且导师/实训室名额未满。

补录结论：

```json
{
  "supplementIntentId": 1,
  "decision": "OFFER",
  "waitlistRank": null,
  "comment": "补录面试通过",
  "reason": "补录考核"
}
```

---

## 五、学院管理端

角色：`ADMIN`。

### 5.1 总览 / 批次 / 审核

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/dashboard` | 学院总览聚合 |
| GET | `/admin/state-machine` | 阶段枚举与顺序 |
| GET | `/admin/batches` | 批次列表（含统计） |
| POST | `/admin/batches` | 创建批次 |
| GET | `/admin/batches/{id}` | 批次详情（阶段、实训室、导师、报名、拟录取、录取、补录、公示、审核） |
| PUT | `/admin/batches/{id}` | 更新批次 |
| POST | `/admin/batches/{id}/stage` | 切换阶段 |
| GET | `/admin/review?batchId=` | 审核问题聚合 |
| POST | `/admin/review/auto-confirm?batchId=` | 自动确认唯一拟录取 |
| POST | `/admin/review/expire-offers?batchId=&reason=` | 处理逾期未确认 |
| GET | `/admin/applications?batchId=&status=&keyword=&page=&size=` | 报名分页 |
| GET | `/admin/applications/{id}` | 报名详情（意向/面试/拟录取/最终结果） |
| GET | `/admin/offers?batchId=` | 拟录取、最终录取、面试总览 |

阶段切换请求：

```json
{
  "status": "INTERVIEW",
  "reason": "意向填报截止，进入面试与初选"
}
```

### 5.2 基础数据

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/labs` | 实训室列表（含导师、名额、报名、录取） |
| POST | `/admin/labs` | 新建实训室 |
| PUT | `/admin/labs/{id}` | 更新实训室 |
| PATCH | `/admin/labs/{id}/status?enabled=` | 启用/停用 |
| GET | `/admin/mentors?keyword=&labId=&page=&size=` | 导师分页 |
| POST | `/admin/mentors` | 新建导师（含账号） |
| PUT | `/admin/mentors/{id}` | 更新导师 |
| PATCH | `/admin/mentors/{id}/status?enabled=` | 启用/停用 |
| POST | `/admin/mentors/{id}/password` | 重置密码 `{ "password": "..." }` |
| GET | `/admin/students?keyword=&grade=&page=&size=` | 学生分页 |
| POST | `/admin/students` | 新建学生（含账号） |
| PUT | `/admin/students/{id}` | 更新学生 |
| PATCH | `/admin/students/{id}/status?enabled=` | 启用/停用 |
| POST | `/admin/students/{id}/password` | 重置密码 |

导师创建请求：

```json
{
  "account": "t1007",
  "password": "123456",
  "name": "周老师",
  "mentorNo": "T1007",
  "title": "副教授",
  "labId": 1,
  "researchDirection": "边缘智能",
  "profile": "导师简介",
  "quota": 2,
  "enabled": true
}
```

学生创建请求：

```json
{
  "account": "S2023015",
  "password": "123456",
  "studentNo": "S2023015",
  "name": "新同学",
  "gender": "女",
  "major": "软件工程",
  "className": "软工2301",
  "grade": "2023",
  "phone": "13800000015",
  "email": "new@example.edu"
}
```

### 5.3 公告

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/announcements?batchId=` | 公告列表 |
| POST | `/admin/announcements` | 新建公告 |
| PUT | `/admin/announcements/{id}` | 更新公告 |
| POST | `/admin/announcements/{id}/publish` | 发布 |
| DELETE | `/admin/announcements/{id}` | 删除 |

公告字段：`batchId`、`title`、`type`（`GENERAL/LECTURE/OPEN_DAY/INTERVIEW/SUPPLEMENT/PUBLICITY`）、`content`、`status`（`DRAFT/PUBLISHED/CLOSED`）、`publishStart`、`publishEnd`。

### 5.4 分轮补录

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/supplement/rounds?batchId=` | 轮次列表（含意向数、拟录取数） |
| POST | `/admin/supplement/rounds` | 创建轮次 |
| POST | `/admin/supplement/rounds/{id}/open` | 开放轮次 |
| POST | `/admin/supplement/rounds/{id}/close` | 结束轮次并处理逾期 |
| POST | `/admin/supplement/rounds/{id}/promote` | 按候补顺位自动递补 |
| GET | `/admin/supplement/waitlist?batchId=` | 按导师分组的候补总览 |

创建轮次：

```json
{
  "roundNo": 1,
  "title": "第一轮候补递补",
  "startAt": "2026-11-16 14:00:00",
  "endAt": "2026-11-18 17:00:00"
}
```

### 5.5 统筹调剂

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/adjustments?batchId=` | 调剂记录 |
| GET | `/admin/adjustments/workspace` | 待调剂学生 + 记录 |
| POST | `/admin/adjustments/auto` | 一键自动调剂 |
| POST | `/admin/adjustments/manual` | 手动调剂 |
| POST | `/admin/adjustments/{studentId}/talk?reason=` | 标记谈话安置 |

手动调剂：

```json
{
  "studentId": 3,
  "toMentorId": 2,
  "reason": "学生授权内部调剂，按剩余名额分配"
}
```

### 5.6 公示

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/publicities?batchId=` | 公示列表 |
| POST | `/admin/publicities` | 保存/更新草稿 |
| GET | `/admin/publicities/{id}` | 公示详情 + 名单 |
| POST | `/admin/publicities/{id}/publish` | 发布公示 |
| POST | `/admin/publicities/{id}/close` | 结束公示并锁定 |
| POST | `/admin/publicities/{id}/revoke?reason=` | 撤回公示并解锁 |

公示草稿：

```json
{
  "batchId": 1,
  "title": "2026年秋季实训室进组选拔拟录取名单公示",
  "content": "公示内容……",
  "startAt": "2026-11-21 09:00:00",
  "endAt": "2026-11-22 09:00:00"
}
```

### 5.7 审计

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/audit-logs?batchId=&action=&page=&size=` | 操作日志分页 |

常见 `action`：

`LOGIN`、`APPLICATION_SAVE`、`APPLICATION_SUBMIT`、`APPLICATION_WITHDRAW`、`INTERVIEW_SAVE`、`DECISION_WAITLIST`、`DECISION_REJECT`、`OFFER_CREATE`、`OFFER_CONFIRM`、`OFFER_AUTO_CONFIRM`、`OFFER_DECLINE`、`OFFER_EXPIRE`、`OFFER_RELEASE`、`SUPPLEMENT_APPLY`、`SUPPLEMENT_OFFER_CREATE`、`SUPPLEMENT_PROMOTE`、`SUPPLEMENT_ROUND_CREATE/OPEN/CLOSE`、`ADJUSTMENT_AUTO/APPLY/TALK`、`PUBLICITY_CREATE/PUBLISH/CLOSE/REVOKE`、`BATCH_STAGE_CHANGE`、`MENTOR_*`、`STUDENT_*`、`LAB_*`、`PASSWORD_RESET`。

---

## 六、数据权限速查

| 角色 | 可见范围 |
| --- | --- |
| 学生 | 仅本人报名、意向、面试、拟录取、调剂、最终结果 |
| 导师 | 本人意向/面试（本实训室共享）/拟录取/最终学生；学生其他平行意向只显示数量 |
| 管理员 | 全部批次与业务数据 |
| 游客 | 公开批次、实训室、公告、已发布公示 |

所有“本人/本室”判断在 Service 层完成；前端只负责展示，不能通过修改请求参数越权。
