# 大纲验收对照表

> 对照《学生实训室进组选拔系统》课程大纲逐项验收。  
> 结论：**必做闭环全部完成**；大纲“不做”项均未引入；构建、单元测试、端到端测试均通过。

---

## 一、技术路线

| 要求 | 实现 | 证据 |
| --- | --- | --- |
| Vue 3 + Vite + Axios 前端 | Vue 3.5、Vite 5、Axios 1.7、Vue Router 4、Pinia | [frontend/package.json](../frontend/package.json) |
| Java 17 + Spring Boot 3 后端 | Java 17 编译目标、Spring Boot 3.2.5 | [backend/pom.xml](../backend/pom.xml) |
| MyBatis | MyBatis 3.0.3 + XML Mapper | [backend/src/main/resources/mapper](../backend/src/main/resources/mapper) |
| MySQL 8 | utf8mb4 / InnoDB，17 张表 | [database/schema.sql](../database/schema.sql) |
| 前后端分离、接口取数 | 所有页面数据来自 REST API | [frontend/src/api](../frontend/src/api) |
| Controller / Service / Mapper 分层 | 明确分层，规则在 Service | [backend/src/main/java/com/labselection](../backend/src/main/java/com/labselection) |

---

## 二、角色

| 角色 | 实现 | 验证 |
| --- | --- | --- |
| 游客 | 公开首页、实训室、公告、已发布公示 | 公开页截图 / `GET /api/public/*` |
| 学生 | 报名、意向、面试、拟录取、补录、结果 | 学生端页面 + 学生接口 |
| 导师 / 实训室负责人 | 报名学生、面试、初选、候补、最终名单 | 导师端页面 + 导师接口 |
| 学院管理员 | 批次、公告、实训室/导师/名额、审核、补录、调剂、公示 | 管理端 12 个页面 + 管理接口 |

账号与学生/导师绑定：`sys_user` 关联 `student` / `mentor`；学生账号使用学号，导师账号使用工号；管理员可重置密码、停用账号。

---

## 三、学生端需求

| 大纲要求 | 实现情况 | 位置 |
| --- | --- | --- |
| 选拔首页：说明、资格、时间、常见问题、公告 | 公开首页含批次说明、8 步时间线、规则速览、常见问题、公告 | `PublicHome.vue` |
| 实训室信息：名称、方向、培养、导师、名额、要求、联系方式 | 实训室卡片 + 详情弹层 | `PublicHome.vue`、`StudentLabs.vue` |
| 意向填报：最多 3 个平行意向、个人简介/技术基础/项目经历 | 独立填报页 + MentorPicker | `StudentApplication.vue` |
| 调剂意愿：内部 / 跨室 / 不接受 | 三选一 + 明确拒绝实训室 | `StudentApplication.vue` |
| 我的选拔进度：提交、面试、结果、确认截止、最终去向 | 工作台聚合 + 8 步进度条 | `StudentDashboard.vue` |
| 拟录取确认：单一自动、多个选一、可全部放弃 | 确认/放弃/全部放弃 + 自动确认 | `StudentOffers.vue`、`SelectionService` |
| 结果查询：实训室、导师、培养方向、入组说明 | 结果页展示录取、方向、培养安排、公示 | `StudentResult.vue` |
| 历史批次 | 我的结果页展示历史报名与去向 | `StudentService.history` |

---

## 四、导师端需求

| 大纲要求 | 实现情况 | 位置 |
| --- | --- | --- |
| 报名学生管理：名单、基本信息、简介、经历 | 分页、筛选、详情 | `MentorApplicants.vue` |
| 统一面试：时间、考核教师、评分、评语、建议结果 | 面试表单 + 本实训室共享记录 | `MentorApplicants.vue`、`MentorInterviews.vue` |
| 初选建议：拟录取 / 候补 / 不录取，受名额限制 | 结论表单 + 名额行锁校验 | `MentorDecisions.vue`、`SelectionService` |
| 候补顺位 | 顺位字段 + 同导师唯一校验 | `MentorDecisions.vue` |
| 最终名单 | 我的学生页 | `MentorStudents.vue` |
| 不显示学生其他平行意向的具体导师 | 仅返回 `otherIntentCount` | `MentorService.applicantDetail` |

---

## 五、管理端需求

| 大纲要求 | 实现情况 | 位置 |
| --- | --- | --- |
| 批次管理：名称、年级、阶段、起止时间 | 批次 CRUD + 8 组阶段时间 | `AdminBatches.vue` |
| 公告与流程：宣讲、开放日、面试、补录、公示 | 公告 CRUD/发布 + 公开页 | `AdminAnnouncements.vue` |
| 实训室与导师管理：介绍、方向、导师、名额、启用 | 实训室/导师 CRUD + 名额校验 | `AdminLabs.vue`、`AdminMentors.vue` |
| 学生资格管理：名单维护、限制非本批次 | 学生 CRUD + 年级/专业资格校验 | `AdminStudents.vue`、`StudentService` |
| 初选审核：超额、重复、缺面试 | 审核问题列表 + 名额表 | `AdminReview.vue`、`AdminService.review` |
| 补录管理：分轮、候补顺位 | 轮次创建/开放/结束/递补 | `AdminSupplement.vue` |
| 统筹调剂：内部/跨室/授权 | 一键+手动+谈话 | `AdminAdjustment.vue`、`AdjustmentService` |
| 公示管理：生成、时间、锁定 | 草稿/发布/结束/撤回 | `AdminPublicity.vue` |
| 审计 | 按批次/操作类型查询 | `AdminAudit.vue` |

---

## 六、数据与初始化

| 要求 | 实现 |
| --- | --- |
| 用户、学生、实训室、导师、批次、报名、意向、面试、录取、调剂、公告等核心表 | 17 张表，见 [database/schema.sql](../database/schema.sql) |
| 3 个以上实训室 | 3 个 |
| 6 名以上导师 | 6 名 |
| 12 名以上学生 | 14 名 |
| 覆盖报名、面试、拟录取、放弃、补录、调剂等状态 | 历史批次完整覆盖；当前批次覆盖填报中 |
| 可重复初始化 | `INSERT IGNORE` + `CREATE TABLE IF NOT EXISTS`；一键脚本 [database/init.sql](../database/init.sql) |

---

## 七、业务规则验收

| 规则 | 实现 | 验证 |
| --- | --- | --- |
| 每人最多 3 个平行意向、不能重复 | Service 去重 + 数量校验 + 唯一键 | E2E [04] |
| 仅开放期可填报/修改/撤回，截止锁定 | `StageGuard.assertIntentWindow` | 代码 + 页面只读 |
| 仅符合年级/专业可报名 | `StudentService.isEligible` | E2E [03] |
| 导师拟录取必须有已完成面试 | `interviewMapper.existsCompleted` | 代码 + 审核问题 |
| 导师不能超额 | `SELECT … FOR UPDATE` + 计数 | E2E [10] |
| 实训室不能超总名额 | 同事务锁定实训室 | 代码 |
| 多个拟录取只能确认一个，其他释放 | 唯一键 + `releaseOtherOffers` | E2E [11][12] |
| 唯一拟录取自动确认 | `autoConfirmSingleOffers` | E2E [11] |
| 逾期未确认视为放弃 | 管理员处理 + 定时任务（生产） | 接口 + 代码 |
| 补录按候补顺位、记录轮次 | `waitlist_rank` + `supplement_round` | E2E [14][15] |
| 内部调剂限同实训室、跨室需授权 | `AdjustmentService` + 偏好/拒绝实训室 | E2E [16] |
| 调剂不绕过名额、不进拒绝实训室 | `assertCapacity` + `excluded_lab_ids` | 代码 + E2E |
| 学生/导师只看本人相关数据 | Service 归属校验 + 导师端屏蔽 | E2E [06] |
| 公示前不公开他人结果 | 公开名单仅在公示发布/结束后返回 | E2E [17] |
| 公示后锁定，撤回—修改—重新公示 | 公示状态机 + `assertNotLocked` | E2E [17][18] |
| 关键操作记录操作人/时间/原因 | `audit_log` | E2E [19] |

---

## 八、边界（不做项）

| 大纲“不做” | 本项目 |
| --- | --- |
| 统一身份认证、教务/学工对接 | 未引入，使用演示账号 |
| 短信/邮件/微信通知 | 未引入 |
| 在线视频面试/笔试/人脸识别 | 未引入，仅记录线下结果 |
| 成绩单同步、简历解析 | 未引入 |
| AI 评价、智能推荐、复杂加权排名 | 未引入 |
| Excel 导入导出、数据大屏、复杂报表 | 未引入 |
| 多学院、跨校区、多届并行 | 未引入 |
| Redis、消息队列、微服务、Docker | 未引入 |

---

## 九、构建与验证证据

### 1. 后端构建

```bash
cd backend
mvn -DskipTests package
```

结果：`selection-backend-1.0.0.jar` 构建成功。

### 2. 后端单元测试

```bash
cd backend
mvn test
```

覆盖：PBKDF2 口令摘要、CSV 工具类。结果：通过。

### 3. 前端构建

```bash
cd frontend
npm run build
```

结果：133 个模块转换成功，产物输出到 `frontend/dist`。

### 4. 端到端业务闭环

```bash
python scripts/e2e_test.py
```

结果：

```text
ALL PASSED: 20 checks
```

覆盖：

1. 登录与角色；
2. 公开首页；
3. 资格校验；
4. 意向超限；
5. 提交 3 个意向；
6. 导师数据隔离；
7. 阶段推进；
8. 面试录入；
9. 拟录取/候补；
10. 名额超额拦截；
11. 唯一拟录取自动确认；
12. 多拟录取自主确认与释放；
13. 逾期处理接口；
14. 候补递补；
15. 补录轮次全流程；
16. 自动调剂与谈话；
17. 公示发布与锁定；
18. 公示结束锁定；
19. 审计与越权拦截；
20. 批次完成后仍可查看本批次结果与培养方向。

---

## 十、演示入口

- 公开首页：`/`
- 学生端：`/student`（`S2023001 / 123456`）
- 导师端：`/mentor`（`t1001 / 123456`）
- 管理端：`/admin`（`admin / 123456`）

逐步演示见 [DEMO.md](DEMO.md)。
