-- ============================================================
-- 演示数据（幂等：重复启动不会产生重复记录）
-- 所有演示账号密码均为 123456
-- 账号：admin / t1001...t3002 / S2023001...S2023014
-- 说明：数据覆盖“填报中 + 已完成”两个批次，便于直接演示完整闭环。
-- ============================================================

-- ---------- 账号 ----------
INSERT IGNORE INTO `sys_user` (`id`, `account`, `password_hash`, `display_name`, `role`, `enabled`) VALUES
(1,  'admin',   'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '学院教务管理员', 'ADMIN', 1),
(2,  't1001',   'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '张明', 'MENTOR', 1),
(3,  't1002',   'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '李静', 'MENTOR', 1),
(4,  't2001',   'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '王强', 'MENTOR', 1),
(5,  't2002',   'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '赵敏', 'MENTOR', 1),
(6,  't3001',   'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '陈晨', 'MENTOR', 1),
(7,  't3002',   'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '刘洋', 'MENTOR', 1),
(8,  'S2023001', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '林晓', 'STUDENT', 1),
(9,  'S2023002', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '周子涵', 'STUDENT', 1),
(10, 'S2023003', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '陈雨桐', 'STUDENT', 1),
(11, 'S2023004', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '赵一鸣', 'STUDENT', 1),
(12, 'S2023005', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '孙悦', 'STUDENT', 1),
(13, 'S2023006', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '吴昊', 'STUDENT', 1),
(14, 'S2023007', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '郑可', 'STUDENT', 1),
(15, 'S2023008', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '王思远', 'STUDENT', 1),
(16, 'S2023009', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '李知遥', 'STUDENT', 1),
(17, 'S2023010', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '黄子轩', 'STUDENT', 1),
(18, 'S2023011', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '徐若曦', 'STUDENT', 1),
(19, 'S2023012', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '马腾', 'STUDENT', 1),
(20, 'S2023013', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '何佳', 'STUDENT', 1),
(21, 'S2023014', 'pbkdf2$120000$bGFiLXNlbGVjdGlvbi1kZW1vLTIwMjY=$wtBvSjvr5AVY+8VqoqGlFiMWXxbmZQWpNpAzEZpgySg=', '罗宇', 'STUDENT', 1);

-- ---------- 实训室 ----------
INSERT IGNORE INTO `lab` (`id`, `code`, `name`, `intro`, `direction`, `training_plan`, `requirements`, `contact`, `total_quota`, `status`) VALUES
(1, 'LAB-AIOT', '智能感知与边缘计算实训室',
 '面向工业与校园场景，建设从端侧感知、边缘推理到云端协同的完整实践链条。',
 '边缘智能、嵌入式视觉、工业物联网、智能硬件',
 '采用 AI+PBL 项目制培养。入组后先完成 4 周基础训练，再进入企业真实课题或学科竞赛项目，配备双周一对一指导与阶段答辩。',
 '具备 C/C++ 或 Python 基础；对嵌入式、计算机视觉有持续兴趣；能保证每周不少于 10 小时投入。',
 '王老师 · 信息楼 A301', 6, 'ACTIVE'),
(2, 'LAB-DATA', '数据智能与知识工程实训室',
 '聚焦大模型应用、知识图谱与数据挖掘，强调从数据治理到智能应用的工程化落地。',
 '大模型应用、知识图谱、数据挖掘、推荐系统',
 '以小组为单位参与横向课题，训练数据清洗、特征工程、模型评测与论文复现能力，优秀成员可参与论文与专利工作。',
 '掌握 Python 与至少一种机器学习框架；有数学建模或数据分析经历者优先。',
 '赵老师 · 信息楼 B402', 5, 'ACTIVE'),
(3, 'LAB-HCI', '人机交互与数字媒体实训室',
 '关注人与数字世界的交互方式，覆盖交互设计、可视化、VR/AR 与数字内容创作。',
 '交互设计、数据可视化、VR/AR、数字媒体',
 '项目制 + 工作坊培养，从需求洞察、原型设计到可用性测试完整实践，定期组织作品评审与行业分享。',
 '具备设计基础或前端开发能力；提交过交互/可视化作品者优先。',
 '陈老师 · 创意楼 C205', 4, 'ACTIVE');

-- ---------- 导师 ----------
INSERT IGNORE INTO `mentor` (`id`, `user_id`, `mentor_no`, `name`, `title`, `lab_id`, `research_direction`, `profile`, `quota`, `enabled`) VALUES
(1, 2, 'T1001', '张明', '教授', 1, '边缘智能与嵌入式视觉', '主持国家自然科学基金面上项目，长期指导嵌入式 AI 与工业视觉课题。', 3, 1),
(2, 3, 'T1002', '李静', '副教授', 1, '智能硬件与工业物联网', '研究方向为低功耗感知与边缘协同，注重工程实践与论文写作并重。', 3, 1),
(3, 4, 'T2001', '王强', '教授', 2, '大模型应用与知识图谱', '承担多项大模型行业应用课题，指导学生在 ACL、KDD 等会议发表论文。', 3, 1),
(4, 5, 'T2002', '赵敏', '副教授', 2, '数据挖掘与推荐系统', '长期从事数据挖掘与用户行为建模研究，擅长指导学生完成完整数据项目。', 2, 1),
(5, 6, 'T3001', '陈晨', '讲师', 3, '交互设计与数据可视化', '关注可解释交互与信息可视化，指导作品多次获省级以上奖项。', 2, 1),
(6, 7, 'T3002', '刘洋', '副教授', 3, 'VR/AR 与数字内容', '研究方向为沉浸式交互与数字内容生成，拥有丰富的产学研合作经验。', 2, 1);

-- ---------- 学生 ----------
INSERT IGNORE INTO `student` (`id`, `user_id`, `student_no`, `name`, `gender`, `major`, `class_name`, `grade`, `phone`, `email`) VALUES
(1, 8,  'S2023001', '林晓',   '女', '计算机科学与技术', '计科2301', '2023', '13800000001', 'linxiao@example.edu'),
(2, 9,  'S2023002', '周子涵', '男', '软件工程', '软工2302', '2023', '13800000002', 'zhouzihan@example.edu'),
(3, 10, 'S2023003', '陈雨桐', '女', '人工智能', '人工智能2301', '2023', '13800000003', 'chenyutong@example.edu'),
(4, 11, 'S2023004', '赵一鸣', '男', '计算机科学与技术', '计科2302', '2023', '13800000004', 'zhaoyiming@example.edu'),
(5, 12, 'S2023005', '孙悦',   '女', '数据科学与大数据技术', '大数据2301', '2023', '13800000005', 'sunyue@example.edu'),
(6, 13, 'S2023006', '吴昊',   '男', '软件工程', '软工2301', '2023', '13800000006', 'wuhao@example.edu'),
(7, 14, 'S2023007', '郑可',   '女', '数字媒体技术', '数媒2301', '2023', '13800000007', 'zhengke@example.edu'),
(8, 15, 'S2023008', '王思远', '男', '人工智能', '人工智能2302', '2023', '13800000008', 'wangsiyuan@example.edu'),
(9, 16, 'S2023009', '李知遥', '女', '计算机科学与技术', '计科2301', '2023', '13800000009', 'lizhiyao@example.edu'),
(10, 17, 'S2023010', '黄子轩', '男', '软件工程', '软工2302', '2023', '13800000010', 'huangzixuan@example.edu'),
(11, 18, 'S2023011', '徐若曦', '女', '数据科学与大数据技术', '大数据2302', '2023', '13800000011', 'xuruoxi@example.edu'),
(12, 19, 'S2023012', '马腾',   '男', '数字媒体技术', '数媒2302', '2023', '13800000012', 'mateng@example.edu'),
(13, 20, 'S2023013', '何佳',   '女', '人工智能', '人工智能2301', '2024', '13800000013', 'hejia@example.edu'),
(14, 21, 'S2023014', '罗宇',   '男', '计算机科学与技术', '计科2302', '2023', '13800000014', 'luoyu@example.edu');

-- ---------- 选拔批次 ----------
INSERT IGNORE INTO `selection_batch`
(`id`, `name`, `grade_scope`, `major_scope`, `description`, `rules`, `status`, `lecture_at`, `open_day_start`, `open_day_end`,
 `intent_start`, `intent_end`, `interview_start`, `interview_end`, `confirm_start`, `confirm_end`,
 `supplement_start`, `supplement_end`, `adjust_start`, `adjust_end`, `publicity_start`, `publicity_end`, `created_by`) VALUES
(1, '2026年秋季实训室进组选拔', '2023', NULL,
 '面向 2023 级本科生开展实训室进组选拔。学生至多填报 3 个平行导师意向，经实训室统一面试、导师初选、学生确认、分轮补录与统筹调剂后确定最终去向。',
 '1. 每人最多 3 个平行意向，不设志愿先后；2. 一位学生只能确认一个拟录取意向；3. 导师与实训室均不得超额录取；4. 逾期未确认视为放弃；5. 调剂须经学生授权，且不得分配到其明确拒绝的实训室。',
 'INTENT_OPEN',
 '2026-11-01 19:00:00', '2026-11-02 09:00:00', '2026-11-03 17:00:00',
 '2026-11-04 08:00:00', '2026-11-12 22:00:00', '2026-11-13 09:00:00', '2026-11-14 18:00:00',
 '2026-11-15 08:00:00', '2026-11-16 12:00:00', '2026-11-16 14:00:00', '2026-11-18 17:00:00',
 '2026-11-19 09:00:00', '2026-11-20 12:00:00', '2026-11-21 09:00:00', '2026-11-22 09:00:00', 1),
(2, '2026年春季实训室进组选拔', '2023', NULL,
 '2026 年春季批次，已完成首轮面试、拟录取确认、候补递补、统筹调剂与结果公示，用于展示完整的选拔历史数据。',
 '同一学生只能确认一个拟录取意向；公示结束后结果锁定。',
 'FINISHED',
 '2026-03-01 19:00:00', '2026-03-02 09:00:00', '2026-03-03 17:00:00',
 '2026-03-04 08:00:00', '2026-03-10 22:00:00', '2026-03-16 09:00:00', '2026-03-17 18:00:00',
 '2026-03-24 08:00:00', '2026-03-25 12:00:00', '2026-03-27 09:00:00', '2026-03-29 17:00:00',
 '2026-03-31 09:00:00', '2026-04-01 12:00:00', '2026-04-03 09:00:00', '2026-04-04 09:00:00', 1);

-- ---------- 报名：当前批次（填报中） ----------
INSERT IGNORE INTO `application`
(`id`, `batch_id`, `student_id`, `intro`, `skills`, `project_experience`, `adjustment_preference`, `excluded_lab_ids`, `status`, `version`, `submitted_at`) VALUES
(1, 1, 1, '计算机科学与技术专业三年级学生，关注边缘智能与端侧视觉，希望把算法真正部署到设备上。',
 'Python、C++、PyTorch、Linux 基础；了解 OpenCV 与模型量化。',
 '参与校级创新项目“校园人流检测”，负责数据标注与 YOLO 模型训练；完成操作系统课程设计。',
 'CROSS', NULL, 'SUBMITTED', 0, '2026-11-05 10:12:00'),
(2, 1, 2, '软件工程专业，工程基础扎实，偏好嵌入式软件与物联网系统。',
 'Java、C、MySQL、Git；了解 MQTT 与 Spring Boot。',
 '开发过实验室设备预约系统；参加蓝桥杯省赛获二等奖。',
 'INTERNAL', NULL, 'SUBMITTED', 0, '2026-11-05 14:30:00'),
(3, 1, 3, '人工智能专业，对知识图谱与大模型检索增强应用感兴趣。',
 'Python、Transformers、Neo4j、SQL。',
 '构建过课程知识图谱问答 demo；参与教师横向课题的数据清洗工作。',
 'CROSS', NULL, 'SUBMITTED', 0, '2026-11-06 09:05:00'),
(4, 1, 4, '喜欢底层与硬件方向，希望进入智能感知实训室做嵌入式视觉。',
 'C/C++、STM32、OpenCV。',
 '智能小车循迹与避障项目；电子设计竞赛校赛一等奖。',
 'NONE', NULL, 'SUBMITTED', 0, '2026-11-06 16:40:00'),
(5, 1, 5, '数据科学专业，正在准备数据可视化作品集。', 'Python、Pandas、ECharts、Figma。', '课程数据可视化项目；校园二手交易数据分析。', 'CROSS', '2', 'DRAFT', 0, NULL),
(6, 1, 6, '软件工程专业，偏前端与交互实现，也愿意做可视化。', 'Vue、TypeScript、Three.js、Figma。', '校园导览 Web 应用；参与学院官网改版。', 'CROSS', NULL, 'SUBMITTED', 0, '2026-11-07 11:20:00'),
(7, 1, 7, '数字媒体技术专业，关注沉浸式交互与数字内容创作。', 'Unity、Blender、C#、交互原型。', 'VR 校园漫游课程作业；数字媒体大赛校赛三等奖。', 'INTERNAL', NULL, 'SUBMITTED', 0, '2026-11-07 19:15:00'),
(8, 1, 9, '计算机专业，对推荐系统与知识图谱都有兴趣。', 'Python、Java、Spark 基础、SQL。', '电影推荐系统课程项目；数学建模校赛参与奖。', 'CROSS', NULL, 'SUBMITTED', 0, '2026-11-08 08:50:00');

-- ---------- 报名：历史批次（已完成，用于展示各状态） ----------
INSERT IGNORE INTO `application`
(`id`, `batch_id`, `student_id`, `intro`, `skills`, `project_experience`, `adjustment_preference`, `excluded_lab_ids`, `status`, `version`, `submitted_at`) VALUES
(9, 2, 1, '希望进入智能感知实训室，将视觉算法部署到边缘设备。', 'Python、C++、PyTorch。', '校园人流检测项目。', 'CROSS', NULL, 'LOCKED', 1, '2026-03-05 09:10:00'),
(10, 2, 2, '偏嵌入式软件与物联网系统。', 'Java、C、MySQL。', '设备预约系统。', 'INTERNAL', NULL, 'LOCKED', 1, '2026-03-05 10:20:00'),
(11, 2, 3, '对边缘智能和知识工程都感兴趣。', 'Python、Neo4j。', '课程知识图谱问答。', 'CROSS', NULL, 'LOCKED', 1, '2026-03-06 09:00:00'),
(12, 2, 4, '希望做嵌入式视觉。', 'C/C++、OpenCV。', '智能小车项目。', 'INTERNAL', NULL, 'LOCKED', 1, '2026-03-06 11:30:00'),
(13, 2, 5, '希望做数据挖掘与推荐系统。', 'Python、Pandas。', '电影推荐系统。', 'CROSS', NULL, 'LOCKED', 1, '2026-03-07 08:40:00'),
(14, 2, 6, '希望做交互设计与前端实现。', 'Vue、Three.js。', '校园导览 Web 应用。', 'CROSS', NULL, 'LOCKED', 1, '2026-03-07 15:10:00'),
(15, 2, 7, '希望做 VR 内容创作。', 'Unity、Blender。', 'VR 校园漫游。', 'NONE', NULL, 'LOCKED', 1, '2026-03-08 09:25:00'),
(16, 2, 8, '对沉浸式交互感兴趣。', 'Unity、C#。', 'AR 明信片项目。', 'CROSS', NULL, 'LOCKED', 1, '2026-03-08 10:45:00');

-- ---------- 平行意向：当前批次 ----------
INSERT IGNORE INTO `application_intent`
(`id`, `application_id`, `batch_id`, `student_id`, `mentor_id`, `sort_no`, `status`, `waitlist_rank`, `comment`) VALUES
(1, 1, 1, 1, 1, 1, 'PENDING', NULL, NULL),
(2, 1, 1, 1, 3, 2, 'PENDING', NULL, NULL),
(3, 1, 1, 1, 5, 3, 'PENDING', NULL, NULL),
(4, 2, 1, 2, 2, 1, 'PENDING', NULL, NULL),
(5, 2, 1, 2, 4, 2, 'PENDING', NULL, NULL),
(6, 3, 1, 3, 3, 1, 'PENDING', NULL, NULL),
(7, 3, 1, 3, 1, 2, 'PENDING', NULL, NULL),
(8, 4, 1, 4, 1, 1, 'PENDING', NULL, NULL),
(9, 5, 1, 5, 5, 1, 'PENDING', NULL, NULL),
(10, 5, 1, 5, 6, 2, 'PENDING', NULL, NULL),
(11, 6, 1, 6, 5, 1, 'PENDING', NULL, NULL),
(12, 6, 1, 6, 6, 2, 'PENDING', NULL, NULL),
(13, 6, 1, 6, 3, 3, 'PENDING', NULL, NULL),
(14, 7, 1, 7, 6, 1, 'PENDING', NULL, NULL),
(15, 8, 1, 9, 2, 1, 'PENDING', NULL, NULL),
(16, 8, 1, 9, 4, 2, 'PENDING', NULL, NULL),
(17, 8, 1, 9, 6, 3, 'PENDING', NULL, NULL);

-- ---------- 平行意向：历史批次 ----------
INSERT IGNORE INTO `application_intent`
(`id`, `application_id`, `batch_id`, `student_id`, `mentor_id`, `sort_no`, `status`, `waitlist_rank`, `comment`, `decided_by`, `decided_at`) VALUES
(18, 9, 2, 1, 1, 1, 'CONFIRMED', NULL, '算法基础扎实，面试表现优秀。', 2, '2026-03-21 10:00:00'),
(19, 9, 2, 1, 3, 2, 'RELEASED', NULL, '学生已确认其他实训室。', 4, '2026-03-21 11:00:00'),
(20, 10, 2, 2, 2, 1, 'CONFIRMED', NULL, '工程能力突出，推荐录取。', 3, '2026-03-21 14:00:00'),
(21, 11, 2, 3, 1, 1, 'DECLINED', NULL, '候补顺位靠后，首轮未确认。', 2, '2026-03-21 15:00:00'),
(22, 12, 2, 4, 3, 1, 'CONFIRMED', 1, '面试通过，进入候补，补录轮递补。', 4, '2026-03-21 16:00:00'),
(23, 13, 2, 5, 4, 1, 'DECLINED', NULL, '拟录取后逾期未确认。', 5, '2026-03-22 09:00:00'),
(24, 14, 2, 6, 5, 1, 'CONFIRMED', NULL, '作品完整，交互基础好。', 6, '2026-03-22 10:00:00'),
(25, 15, 2, 7, 6, 1, 'DECLINED', NULL, '学生明确放弃拟录取。', 7, '2026-03-22 11:00:00'),
(26, 16, 2, 8, 6, 1, 'WAITLISTED', 1, '面试通过，列入候补顺位 1。', 7, '2026-03-22 11:30:00');

-- ---------- 面试考核：历史批次 ----------
INSERT IGNORE INTO `interview`
(`id`, `batch_id`, `application_id`, `lab_id`, `mentor_id`, `scheduled_at`, `location`, `score`, `comment`, `conclusion`, `status`, `recorded_by`, `recorded_at`) VALUES
(1, 2, 9, 1, 1, '2026-03-18 09:00:00', '信息楼 A301', 88, '算法理解深入，项目描述清晰，动手能力强。', 'PASS', 'COMPLETED', 2, '2026-03-18 10:00:00'),
(2, 2, 10, 1, 2, '2026-03-18 10:00:00', '信息楼 A301', 85, '工程基础扎实，对物联网协议有实际使用经验。', 'PASS', 'COMPLETED', 3, '2026-03-18 11:00:00'),
(3, 2, 11, 1, 1, '2026-03-18 11:00:00', '信息楼 A301', 78, '知识面较广，但项目深度一般。', 'PASS', 'COMPLETED', 2, '2026-03-18 12:00:00'),
(4, 2, 12, 2, 3, '2026-03-19 09:30:00', '信息楼 B402', 91, '思路清晰，具备较好的科研潜力。', 'PASS', 'COMPLETED', 4, '2026-03-19 10:30:00'),
(5, 2, 13, 2, 4, '2026-03-19 11:00:00', '信息楼 B402', 76, '项目完成度尚可，需加强模型评估能力。', 'PASS', 'COMPLETED', 5, '2026-03-19 12:00:00'),
(6, 2, 14, 3, 5, '2026-03-20 09:00:00', '创意楼 C205', 89, '作品完成度高，交互细节考虑充分。', 'PASS', 'COMPLETED', 6, '2026-03-20 10:00:00'),
(7, 2, 15, 3, 6, '2026-03-20 10:00:00', '创意楼 C205', 72, '基础尚可，作品创意一般。', 'PASS', 'COMPLETED', 7, '2026-03-20 11:00:00'),
(8, 2, 16, 3, 6, '2026-03-20 11:00:00', '创意楼 C205', 80, '学习意愿强，动手能力不错。', 'PASS', 'COMPLETED', 7, '2026-03-20 12:00:00');

-- ---------- 拟录取/候补记录：历史批次 ----------
INSERT IGNORE INTO `offer`
(`id`, `batch_id`, `student_id`, `mentor_id`, `lab_id`, `application_id`, `intent_id`, `supplement_intent_id`, `source`, `status`, `round_no`, `created_by`, `created_at`, `confirmed_at`) VALUES
(1, 2, 1, 1, 1, 9, 18, NULL, 'INITIAL', 'CONFIRMED', NULL, 2, '2026-03-21 10:00:00', '2026-03-25 09:05:00'),
(2, 2, 1, 3, 2, 9, 19, NULL, 'INITIAL', 'RELEASED', NULL, 4, '2026-03-21 11:00:00', NULL),
(3, 2, 2, 2, 1, 10, 20, NULL, 'INITIAL', 'CONFIRMED', NULL, 3, '2026-03-21 14:00:00', '2026-03-24 09:00:00'),
(4, 2, 3, 1, 1, 11, 21, NULL, 'INITIAL', 'DECLINED', NULL, 2, '2026-03-21 15:00:00', NULL),
(5, 2, 4, 3, 2, 12, 22, NULL, 'PROMOTION', 'CONFIRMED', 1, 1, '2026-03-28 10:00:00', '2026-03-28 15:30:00'),
(6, 2, 5, 4, 2, 13, 23, NULL, 'INITIAL', 'EXPIRED', NULL, 5, '2026-03-22 09:00:00', NULL),
(7, 2, 6, 5, 3, 14, 24, NULL, 'INITIAL', 'CONFIRMED', NULL, 6, '2026-03-22 10:00:00', '2026-03-24 10:20:00'),
(8, 2, 7, 6, 3, 15, 25, NULL, 'INITIAL', 'DECLINED', NULL, 7, '2026-03-22 11:00:00', NULL);

-- ---------- 最终录取：历史批次 ----------
INSERT IGNORE INTO `final_assignment`
(`id`, `batch_id`, `student_id`, `application_id`, `mentor_id`, `lab_id`, `source`, `status`, `confirmed_at`, `remark`) VALUES
(1, 2, 1, 9, 1, 1, 'CONFIRM', 'LOCKED', '2026-03-25 09:05:00', '学生确认首选拟录取'),
(2, 2, 2, 10, 2, 1, 'CONFIRM', 'LOCKED', '2026-03-24 09:00:00', '唯一拟录取自动确认'),
(3, 2, 3, 11, 2, 1, 'INTERNAL_ADJUST', 'LOCKED', '2026-04-02 10:00:00', '首轮未录取，经实训室内部调剂至李静老师'),
(4, 2, 4, 12, 3, 2, 'PROMOTION', 'LOCKED', '2026-03-28 15:30:00', '候补顺位 1，补录轮递补确认'),
(5, 2, 5, 13, 5, 3, 'CROSS_ADJUST', 'LOCKED', '2026-04-02 11:00:00', '拟录取逾期，经学生授权跨实训室调剂'),
(6, 2, 6, 14, 5, 3, 'CONFIRM', 'LOCKED', '2026-03-24 10:20:00', '学生确认拟录取');

-- ---------- 补录轮次：历史批次 ----------
INSERT IGNORE INTO `supplement_round`
(`id`, `batch_id`, `round_no`, `title`, `status`, `start_at`, `end_at`, `created_by`) VALUES
(1, 2, 1, '第一轮候补递补', 'CLOSED', '2026-03-27 09:00:00', '2026-03-29 17:00:00', 1),
(2, 2, 2, '第二轮补录填报', 'CLOSED', '2026-03-30 09:00:00', '2026-04-01 12:00:00', 1);

INSERT IGNORE INTO `supplement_intent`
(`id`, `round_id`, `batch_id`, `student_id`, `mentor_id`, `sort_no`, `status`, `waitlist_rank`, `comment`, `decided_by`, `decided_at`) VALUES
(1, 2, 2, 7, 6, 1, 'REJECTED', NULL, '补录面试未通过。', 7, '2026-04-01 10:00:00');

-- ---------- 调剂记录：历史批次 ----------
INSERT IGNORE INTO `adjustment_record`
(`id`, `batch_id`, `student_id`, `from_mentor_id`, `from_lab_id`, `to_mentor_id`, `to_lab_id`, `type`, `status`, `reason`, `operator_id`, `created_at`) VALUES
(1, 2, 3, 1, 1, 2, 1, 'INTERNAL_ADJUST', 'LOCKED', '首轮未录取，学生接受实训室内部调剂，按剩余名额调整至同实训室李静老师。', 1, '2026-04-02 10:00:00'),
(2, 2, 5, 4, 2, 5, 3, 'CROSS_ADJUST', 'LOCKED', '拟录取逾期释放名额，学生授权跨实训室调剂，按优先级调整至人机交互实训室。', 1, '2026-04-02 11:00:00'),
(3, 2, 7, 6, 3, NULL, NULL, 'TALK', 'CONFIRMED', '学生不接受调剂，学院安排辅导员谈话后再做安置。', 1, '2026-04-02 12:00:00');

-- ---------- 公示：历史批次 ----------
INSERT IGNORE INTO `publicity`
(`id`, `batch_id`, `title`, `content`, `start_at`, `end_at`, `status`, `published_by`, `published_at`, `closed_at`) VALUES
(1, 2, '2026年春季实训室进组选拔拟录取名单公示',
 '根据《学生实训室进组选拔管理办法》，经集中宣讲、学生填报、统一面试、导师初选、学生确认、分轮补录和统筹调剂，现将拟录取名单予以公示。公示期间如有异议，请向学院教务办公室实名反映。',
 '2026-04-03 09:00:00', '2026-04-04 09:00:00', 'CLOSED', 1, '2026-04-03 09:00:00', '2026-04-04 09:00:00');

-- ---------- 公告 ----------
INSERT IGNORE INTO `announcement`
(`id`, `batch_id`, `title`, `type`, `content`, `status`, `publish_start`, `publish_end`, `created_by`) VALUES
(1, 1, '2026年秋季实训室进组选拔集中宣讲通知', 'LECTURE',
 '学院定于 11 月 1 日 19:00 在信息楼报告厅举行集中宣讲。各实训室负责人将介绍研究方向、项目内容、招生名额、培养安排与考核要求，请有意向的同学准时参加。',
 'PUBLISHED', '2026-10-28 09:00:00', '2026-11-20 22:00:00', 1),
(2, 1, '实训室开放日安排', 'OPEN_DAY',
 '11 月 2 日至 3 日为实训室开放日。同学们可实地走访智能感知、数据智能、人机交互三个实训室，与导师和高年级学生面对面交流，了解项目氛围与培养方式。',
 'PUBLISHED', '2026-10-30 09:00:00', '2026-11-20 18:00:00', 1),
(3, 1, '平行意向填报操作指南', 'GENERAL',
 '意向填报开放时间为 11 月 4 日 08:00 至 11 月 12 日 22:00。每名同学最多填报 3 个平行导师意向，不分先后，并需选择是否接受实训室内部调剂、跨实训室调剂。提交后可在截止前修改或撤回。',
 'PUBLISHED', '2026-11-04 08:00:00', '2026-11-20 22:00:00', 1),
(4, 1, '面试考核安排（草稿）', 'INTERVIEW',
 '面试按实训室统一组织，具体分组与时间将在意向填报截止后发布，请关注系统通知。',
 'DRAFT', NULL, NULL, 1),
(5, 2, '2026年春季实训室进组选拔拟录取名单公示', 'PUBLICITY',
 '2026 年春季批次拟录取名单已公示，公示期 1 天，公示无异议后正式入组。',
 'CLOSED', '2026-04-03 09:00:00', '2026-04-04 09:00:00', 1);

-- ---------- 审计日志 ----------
INSERT IGNORE INTO `audit_log`
(`id`, `batch_id`, `actor_id`, `actor_name`, `actor_role`, `action`, `target_type`, `target_id`, `detail`, `reason`, `ip`) VALUES
(1, 2, 1, '学院教务管理员', 'ADMIN', 'BATCH_STAGE_CHANGE', 'BATCH', 2, '将批次推进至结果公示', '流程正常推进', '127.0.0.1'),
(2, 2, 2, '张明', 'MENTOR', 'OFFER_CREATE', 'OFFER', 1, '向学生林晓发出拟录取意向', NULL, '127.0.0.1'),
(3, 2, 1, '学院教务管理员', 'ADMIN', 'ADJUSTMENT_APPLY', 'STUDENT', 3, '将学生陈雨桐实训室内调剂至李静老师', '首轮未录取，学生授权内部调剂', '127.0.0.1'),
(4, 2, 1, '学院教务管理员', 'ADMIN', 'PUBLICITY_PUBLISH', 'PUBLICITY', 1, '发布并关闭历史批次公示', '公示期满无异议', '127.0.0.1');
