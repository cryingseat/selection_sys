-- ============================================================
-- 学生实训室进组选拔系统  MySQL 8 表结构
-- 字符集：utf8mb4 / 存储引擎：InnoDB
-- 说明：所有枚举以 VARCHAR 存储，便于扩展与排查；
--       关键唯一约束用于兜底“一人一组、最多三个意向”等规则。
-- ============================================================

CREATE TABLE IF NOT EXISTS `college` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `code`         VARCHAR(32)  NOT NULL,
    `name`         VARCHAR(128) NOT NULL,
    `intro`        VARCHAR(500) NULL,
    `accent_color` VARCHAR(16)  NULL COMMENT '学院主题色，用于筛选圆点',
    `sort_order`   INT          NOT NULL DEFAULT 0,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_college_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学院';

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `account`       VARCHAR(64)  NOT NULL COMMENT '登录账号：学号/工号/admin',
    `password_hash` VARCHAR(255) NOT NULL COMMENT 'PBKDF2-SHA256 加盐摘要',
    `display_name`  VARCHAR(64)  NOT NULL COMMENT '显示姓名',
    `role`          VARCHAR(20)  NOT NULL COMMENT 'STUDENT/MENTOR/ADMIN',
    `college_id`    BIGINT       NULL COMMENT '所属学院；登录时必须与所选学院一致',
    `enabled`       TINYINT(1)   NOT NULL DEFAULT 1,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_account` (`account`),
    KEY `idx_user_role` (`role`),
    KEY `idx_user_college` (`college_id`),
    CONSTRAINT `fk_user_college` FOREIGN KEY (`college_id`) REFERENCES `college` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统账号';

CREATE TABLE IF NOT EXISTS `lab` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `college_id`     BIGINT       NOT NULL,
    `code`           VARCHAR(32)  NOT NULL,
    `name`           VARCHAR(128) NOT NULL,
    `intro`          TEXT         NULL,
    `direction`      VARCHAR(255) NULL COMMENT '研究/项目方向',
    `training_plan`  TEXT         NULL COMMENT '培养内容与方式',
    `requirements`   TEXT         NULL COMMENT '选拔要求',
    `contact`        VARCHAR(128) NULL,
    `location`       VARCHAR(128) NULL COMMENT '实训室位置',
    `cover_image`    VARCHAR(255) NULL COMMENT '空间封面图',
    `total_quota`    INT          NOT NULL DEFAULT 0,
    `status`         VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lab_code` (`code`),
    KEY `idx_lab_status` (`status`),
    KEY `idx_lab_college` (`college_id`),
    CONSTRAINT `fk_lab_college` FOREIGN KEY (`college_id`) REFERENCES `college` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实训室';

CREATE TABLE IF NOT EXISTS `student` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT      NOT NULL,
    `college_id` BIGINT      NOT NULL,
    `student_no` VARCHAR(32) NOT NULL,
    `name`       VARCHAR(64) NOT NULL,
    `gender`     VARCHAR(8)  NULL,
    `major`      VARCHAR(64) NULL,
    `class_name` VARCHAR(64) NULL,
    `grade`      VARCHAR(16) NULL,
    `phone`      VARCHAR(32) NULL,
    `email`      VARCHAR(128) NULL,
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_no` (`student_no`),
    UNIQUE KEY `uk_student_user` (`user_id`),
    KEY `idx_student_grade` (`grade`),
    KEY `idx_student_college` (`college_id`),
    CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_student_college` FOREIGN KEY (`college_id`) REFERENCES `college` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生信息';

CREATE TABLE IF NOT EXISTS `mentor` (
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`            BIGINT      NOT NULL,
    `mentor_no`          VARCHAR(32) NOT NULL,
    `name`               VARCHAR(64) NOT NULL,
    `title`              VARCHAR(32) NULL,
    `lab_id`             BIGINT      NOT NULL,
    `research_direction` VARCHAR(255) NULL,
    `profile`            TEXT        NULL,
    `quota`              INT         NOT NULL DEFAULT 0 COMMENT '招生名额',
    `enabled`            TINYINT(1)  NOT NULL DEFAULT 1,
    `created_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mentor_no` (`mentor_no`),
    UNIQUE KEY `uk_mentor_user` (`user_id`),
    KEY `idx_mentor_lab` (`lab_id`),
    CONSTRAINT `fk_mentor_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_mentor_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导师';

CREATE TABLE IF NOT EXISTS `selection_batch` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `name`            VARCHAR(128) NOT NULL,
    `grade_scope`     VARCHAR(64)  NULL COMMENT '参与年级，逗号分隔',
    `major_scope`     VARCHAR(255) NULL COMMENT '参与专业，为空表示不限',
    `description`     TEXT         NULL,
    `rules`           TEXT         NULL,
    `status`          VARCHAR(24)  NOT NULL DEFAULT 'DRAFT',
    `lecture_at`      DATETIME     NULL,
    `open_day_start`  DATETIME     NULL,
    `open_day_end`    DATETIME     NULL,
    `intent_start`    DATETIME     NULL,
    `intent_end`      DATETIME     NULL,
    `interview_start` DATETIME     NULL,
    `interview_end`   DATETIME     NULL,
    `confirm_start`   DATETIME     NULL,
    `confirm_end`     DATETIME     NULL,
    `supplement_start` DATETIME    NULL,
    `supplement_end`  DATETIME     NULL,
    `adjust_start`    DATETIME     NULL,
    `adjust_end`      DATETIME     NULL,
    `publicity_start` DATETIME     NULL,
    `publicity_end`   DATETIME     NULL,
    `created_by`      BIGINT       NULL,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_batch_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='选拔批次';

CREATE TABLE IF NOT EXISTS `application` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `batch_id`              BIGINT       NOT NULL,
    `student_id`            BIGINT       NOT NULL,
    `intro`                 TEXT         NULL COMMENT '个人简介',
    `skills`                TEXT         NULL COMMENT '技术基础',
    `project_experience`    TEXT         NULL COMMENT '项目经历',
    `adjustment_preference` VARCHAR(16)  NOT NULL DEFAULT 'NONE',
    `excluded_lab_ids`      VARCHAR(255) NULL COMMENT '明确拒绝调剂的实训室ID，逗号分隔',
    `status`                VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    `version`               INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `submitted_at`          DATETIME     NULL,
    `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_application_student` (`batch_id`, `student_id`),
    KEY `idx_application_status` (`batch_id`, `status`),
    CONSTRAINT `fk_application_batch` FOREIGN KEY (`batch_id`) REFERENCES `selection_batch` (`id`),
    CONSTRAINT `fk_application_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报名';

CREATE TABLE IF NOT EXISTS `application_intent` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT,
    `application_id` BIGINT      NOT NULL,
    `batch_id`       BIGINT      NOT NULL,
    `student_id`     BIGINT      NOT NULL,
    `mentor_id`      BIGINT      NOT NULL,
    `sort_no`        INT         NOT NULL COMMENT '展示序号，平行意向不设先后',
    `status`         VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    `waitlist_rank`  INT         NULL COMMENT '候补顺位',
    `comment`        VARCHAR(500) NULL,
    `decided_by`     BIGINT      NULL,
    `decided_at`     DATETIME    NULL,
    `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_intent_mentor` (`application_id`, `mentor_id`),
    UNIQUE KEY `uk_intent_sort` (`application_id`, `sort_no`),
    KEY `idx_intent_mentor_status` (`mentor_id`, `status`),
    KEY `idx_intent_batch` (`batch_id`),
    CONSTRAINT `fk_intent_application` FOREIGN KEY (`application_id`) REFERENCES `application` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_intent_mentor` FOREIGN KEY (`mentor_id`) REFERENCES `mentor` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平行意向';

CREATE TABLE IF NOT EXISTS `interview` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT,
    `batch_id`       BIGINT      NOT NULL,
    `application_id` BIGINT      NOT NULL,
    `lab_id`         BIGINT      NOT NULL,
    `mentor_id`      BIGINT      NULL COMMENT '考核教师',
    `scheduled_at`   DATETIME    NULL,
    `location`       VARCHAR(128) NULL,
    `score`          INT         NULL COMMENT '百分制',
    `comment`        TEXT        NULL,
    `conclusion`     VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    `status`         VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    `recorded_by`    BIGINT      NULL,
    `recorded_at`    DATETIME    NULL,
    `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interview_application_lab` (`application_id`, `lab_id`),
    KEY `idx_interview_batch` (`batch_id`),
    KEY `idx_interview_mentor` (`mentor_id`),
    CONSTRAINT `fk_interview_application` FOREIGN KEY (`application_id`) REFERENCES `application` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_interview_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试考核';

CREATE TABLE IF NOT EXISTS `offer` (
    `id`                  BIGINT      NOT NULL AUTO_INCREMENT,
    `batch_id`            BIGINT      NOT NULL,
    `student_id`          BIGINT      NOT NULL,
    `mentor_id`           BIGINT      NOT NULL,
    `lab_id`              BIGINT      NOT NULL,
    `application_id`      BIGINT      NULL COMMENT '补录意向可能没有首轮报名记录',
    `intent_id`           BIGINT      NULL,
    `supplement_intent_id` BIGINT     NULL,
    `source`              VARCHAR(20) NOT NULL DEFAULT 'INITIAL',
    `status`              VARCHAR(16) NOT NULL DEFAULT 'OFFERED',
    `round_no`            INT         NULL,
    `created_by`          BIGINT      NULL,
    `created_at`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `confirmed_at`        DATETIME    NULL,
    `updated_at`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_offer_student` (`batch_id`, `student_id`, `status`),
    KEY `idx_offer_mentor` (`mentor_id`, `status`),
    KEY `idx_offer_lab` (`lab_id`, `status`),
    CONSTRAINT `fk_offer_batch` FOREIGN KEY (`batch_id`) REFERENCES `selection_batch` (`id`),
    CONSTRAINT `fk_offer_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`),
    CONSTRAINT `fk_offer_mentor` FOREIGN KEY (`mentor_id`) REFERENCES `mentor` (`id`),
    CONSTRAINT `fk_offer_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`),
    CONSTRAINT `fk_offer_application` FOREIGN KEY (`application_id`) REFERENCES `application` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拟录取/候补递补记录';

CREATE TABLE IF NOT EXISTS `final_assignment` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT,
    `batch_id`       BIGINT      NOT NULL,
    `student_id`     BIGINT      NOT NULL,
    `application_id` BIGINT      NULL,
    `mentor_id`      BIGINT      NOT NULL,
    `lab_id`         BIGINT      NOT NULL,
    `source`         VARCHAR(24) NOT NULL,
    `status`         VARCHAR(16) NOT NULL DEFAULT 'CONFIRMED',
    `confirmed_at`   DATETIME    NULL,
    `remark`         VARCHAR(500) NULL,
    `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_assignment_student` (`batch_id`, `student_id`),
    KEY `idx_assignment_mentor` (`mentor_id`, `status`),
    KEY `idx_assignment_lab` (`lab_id`, `status`),
    CONSTRAINT `fk_assignment_batch` FOREIGN KEY (`batch_id`) REFERENCES `selection_batch` (`id`),
    CONSTRAINT `fk_assignment_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`),
    CONSTRAINT `fk_assignment_mentor` FOREIGN KEY (`mentor_id`) REFERENCES `mentor` (`id`),
    CONSTRAINT `fk_assignment_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='最终录取结果';

CREATE TABLE IF NOT EXISTS `supplement_round` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `batch_id`   BIGINT      NOT NULL,
    `round_no`   INT         NOT NULL,
    `title`      VARCHAR(128) NOT NULL,
    `status`     VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    `start_at`   DATETIME    NULL,
    `end_at`     DATETIME    NULL,
    `created_by` BIGINT      NULL,
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_round_batch_no` (`batch_id`, `round_no`),
    CONSTRAINT `fk_round_batch` FOREIGN KEY (`batch_id`) REFERENCES `selection_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补录轮次';

CREATE TABLE IF NOT EXISTS `supplement_intent` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `round_id`      BIGINT      NOT NULL,
    `batch_id`      BIGINT      NOT NULL,
    `student_id`    BIGINT      NOT NULL,
    `mentor_id`     BIGINT      NOT NULL,
    `sort_no`       INT         NOT NULL,
    `status`        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    `waitlist_rank` INT         NULL,
    `comment`       VARCHAR(500) NULL,
    `decided_by`    BIGINT      NULL,
    `decided_at`    DATETIME    NULL,
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_supplement_intent` (`round_id`, `student_id`, `mentor_id`),
    KEY `idx_supplement_mentor` (`mentor_id`, `status`),
    CONSTRAINT `fk_supplement_round` FOREIGN KEY (`round_id`) REFERENCES `supplement_round` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_supplement_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`),
    CONSTRAINT `fk_supplement_mentor` FOREIGN KEY (`mentor_id`) REFERENCES `mentor` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补录意向';

CREATE TABLE IF NOT EXISTS `adjustment_record` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `batch_id`      BIGINT      NOT NULL,
    `student_id`    BIGINT      NOT NULL,
    `from_mentor_id` BIGINT     NULL,
    `from_lab_id`   BIGINT      NULL,
    `to_mentor_id`  BIGINT      NULL,
    `to_lab_id`     BIGINT      NULL,
    `type`          VARCHAR(24) NOT NULL,
    `status`        VARCHAR(16) NOT NULL,
    `reason`        VARCHAR(500) NULL,
    `operator_id`   BIGINT      NULL,
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_adjustment_batch` (`batch_id`),
    CONSTRAINT `fk_adjustment_batch` FOREIGN KEY (`batch_id`) REFERENCES `selection_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调剂记录';

CREATE TABLE IF NOT EXISTS `publicity` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT,
    `batch_id`     BIGINT      NOT NULL,
    `title`        VARCHAR(128) NOT NULL,
    `content`      TEXT        NULL,
    `start_at`     DATETIME    NULL,
    `end_at`       DATETIME    NULL,
    `status`       VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    `published_by` BIGINT      NULL,
    `published_at` DATETIME    NULL,
    `closed_at`    DATETIME    NULL,
    `created_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_publicity_batch` (`batch_id`, `status`),
    CONSTRAINT `fk_publicity_batch` FOREIGN KEY (`batch_id`) REFERENCES `selection_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公示';

CREATE TABLE IF NOT EXISTS `announcement` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `batch_id`     BIGINT       NULL,
    `title`        VARCHAR(128) NOT NULL,
    `type`         VARCHAR(20)  NOT NULL DEFAULT 'GENERAL',
    `content`      TEXT         NULL,
    `status`       VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    `publish_start` DATETIME    NULL,
    `publish_end`  DATETIME     NULL,
    `created_by`   BIGINT       NULL,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_announcement_batch` (`batch_id`, `status`),
    CONSTRAINT `fk_announcement_batch` FOREIGN KEY (`batch_id`) REFERENCES `selection_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告';

CREATE TABLE IF NOT EXISTS `audit_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `batch_id`    BIGINT       NULL,
    `actor_id`    BIGINT       NULL,
    `actor_name`  VARCHAR(64)  NULL,
    `actor_role`  VARCHAR(20)  NULL,
    `action`      VARCHAR(64)  NOT NULL,
    `target_type` VARCHAR(32)  NULL,
    `target_id`   BIGINT       NULL,
    `detail`      TEXT         NULL,
    `reason`      VARCHAR(500) NULL,
    `ip`          VARCHAR(64)  NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_audit_batch` (`batch_id`, `created_at`),
    KEY `idx_audit_actor` (`actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计';

CREATE TABLE IF NOT EXISTS `auth_token` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT      NOT NULL,
    `token_hash`   CHAR(64)    NOT NULL,
    `expires_at`   DATETIME    NOT NULL,
    `revoked`      TINYINT(1)  NOT NULL DEFAULT 0,
    `created_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_used_at` DATETIME    NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token_hash` (`token_hash`),
    KEY `idx_token_user` (`user_id`),
    KEY `idx_token_expire` (`expires_at`),
    CONSTRAINT `fk_token_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录令牌';
