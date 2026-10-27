-- 家庭财务管理系统数据库表结构
-- 生成时间: 2024-12-XX
-- 作者: liberty
-- 说明：使用雪花算法生成ID，所有删除操作均为逻辑删除

-- 删除表（如果存在，用于重新创建）
DROP TABLE IF EXISTS `sync_records`;
DROP TABLE IF EXISTS `operation_logs`;
DROP TABLE IF EXISTS `family_application`;
DROP TABLE IF EXISTS `family_member`;
DROP TABLE IF EXISTS `family`;
DROP TABLE IF EXISTS `user`;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE `user` (
    `id` BIGINT NOT NULL COMMENT '用户ID（雪花算法生成）',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `name` VARCHAR(50) DEFAULT NULL COMMENT '姓名',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像路径',
    `status` INT(1) DEFAULT 0 COMMENT '状态：0-正常，1-禁用',
    `current_family_id` BIGINT DEFAULT NULL COMMENT '当前选择的家庭ID',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '修改人ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT(1) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_email` (`email`),
    KEY `idx_phone` (`phone`),
    KEY `idx_current_family_id` (`current_family_id`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

INSERT INTO user (`id`, `username`, `password`, `email`, `phone`, `name`, `status`, `current_family_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`) VALUES (2009106647160520705, '13333333333', '$2a$10$3NcobJkp6KEXj5eV408FMOuwDf/b29fH2fw1RHqk2eHF.RaKqxGrO', '123367@123.com', '13333333333', '张三', 0, 2009106647676420098, NULL, '2026-01-08 11:35:27', NULL, '2026-01-08 11:54:03', 0);

-- ============================================
-- 2. 家庭表
-- ============================================
CREATE TABLE `family` (
    `id` BIGINT NOT NULL COMMENT '家庭ID（雪花算法生成）',
    `name` VARCHAR(100) NOT NULL COMMENT '家庭名称',
    `owner_id` BIGINT NOT NULL COMMENT '创建者ID（家庭管理员）',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '修改人ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT(1) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭表';

INSERT INTO family (`id`, `name`, `owner_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`) VALUES (2009106647676420098, '张三的家庭', 2009106647160520705, NULL, '2026-01-08 11:35:27', NULL, '2026-01-08 11:35:27', 0);


-- ============================================
-- 3. 家庭成员关联表
-- ============================================
CREATE TABLE `family_member` (
    `id` BIGINT NOT NULL COMMENT '关联ID（雪花算法生成）',
    `family_id` BIGINT NOT NULL COMMENT '家庭ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` INT(1) DEFAULT 0 COMMENT '角色：1-管理员，0-普通成员',
    `sync_enabled` INT(1) DEFAULT 0 COMMENT '同步开关投影：1-开启云备份同步，0-休眠（客户端开关切换时上报，仅展示用途）',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '修改人ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT(1) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_family_user` (`family_id`, `user_id`, `deleted`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭成员关联表';

INSERT INTO family_member (`id`, `family_id`, `user_id`, `role`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`) VALUES (2009106648322342914, 2009106647676420098, 2009106647160520705, 1, NULL, '2026-01-08 11:35:27', NULL, '2026-01-08 11:35:27', 0);

-- ============================================
-- 4. 家庭申请表
-- ============================================
CREATE TABLE `family_application` (
    `id` BIGINT NOT NULL COMMENT '申请ID（雪花算法生成）',
    `family_id` BIGINT NOT NULL COMMENT '家庭ID',
    `user_id` BIGINT NOT NULL COMMENT '申请人ID',
    `status` INT(1) DEFAULT 0 COMMENT '状态：0-待审核，1-已批准，2-已拒绝',
    `note` VARCHAR(500) DEFAULT NULL COMMENT '申请备注',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '修改人ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT(1) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭申请表';

-- ============================================
-- 5. 操作日志表
-- ============================================
CREATE TABLE `operation_logs` (
    `id` BIGINT NOT NULL COMMENT '日志ID（雪花算法生成）',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '操作描述',
    `url` VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
    `method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `params` TEXT DEFAULT NULL COMMENT '请求参数',
    `exception` TEXT DEFAULT NULL COMMENT '异常信息',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '修改人ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT(1) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_url` (`url`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================
-- 6. 端到端加密同步记录表
-- 服务端密文哑仓库：业务字段全部在 ciphertext 密文内，
-- 仅同步机制必要字段（id/家庭ID/version/updated_at/deleted/nonce）明文。
-- 注意：deleted 为墓碑业务字段（非逻辑删除），服务端不做物理删除、墓碑必须可查询。
-- ============================================
CREATE TABLE `sync_records` (
    `id` VARCHAR(36) NOT NULL COMMENT '记录ID（客户端生成UUIDv7）',
    `family_id` VARCHAR(36) NOT NULL COMMENT '家庭ID（路由与鉴权）',
    `version` BIGINT NOT NULL COMMENT '版本号（LWW仲裁，高者胜）',
    `updated_at` DATETIME(3) NOT NULL COMMENT '更新时间（客户端提供，毫秒精度，拉取游标）',
    `deleted` INT(1) NOT NULL DEFAULT 0 COMMENT '墓碑标识：0-正常，1-已删除',
    `nonce` BLOB NOT NULL COMMENT 'AES-GCM nonce',
    `ciphertext` LONGBLOB NOT NULL COMMENT '业务密文（含record_type+业务JSON）',
    PRIMARY KEY (`id`),
    KEY `idx_family_updated_id` (`family_id`, `updated_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='端到端加密同步记录表';

-- ============================================
-- 外键约束（可选，根据实际需求决定是否添加）
-- 注意：由于使用逻辑删除，外键约束可能会影响逻辑删除功能
-- ============================================
-- ALTER TABLE `family` ADD CONSTRAINT `fk_family_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;
-- ALTER TABLE `family_member` ADD CONSTRAINT `fk_family_member_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
-- ALTER TABLE `family_member` ADD CONSTRAINT `fk_family_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
-- ALTER TABLE `family_application` ADD CONSTRAINT `fk_family_application_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
-- ALTER TABLE `family_application` ADD CONSTRAINT `fk_family_application_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
-- ALTER TABLE `transactions` ADD CONSTRAINT `fk_transactions_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
-- ALTER TABLE `transactions` ADD CONSTRAINT `fk_transactions_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;
-- ALTER TABLE `user` ADD CONSTRAINT `fk_user_current_family` FOREIGN KEY (`current_family_id`) REFERENCES `family` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;
