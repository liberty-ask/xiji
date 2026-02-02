-- 家庭财务管理系统数据库表结构
-- 生成时间: 2024-12-XX
-- 作者: liberty
-- 说明：使用雪花算法生成ID，所有删除操作均为逻辑删除

-- 删除表（如果存在，用于重新创建）
DROP TABLE IF EXISTS `operation_logs`;
DROP TABLE IF EXISTS `transactions`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `family_application`;
DROP TABLE IF EXISTS `family_member`;
DROP TABLE IF EXISTS `family`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `bill_upload`;
DROP TABLE IF EXISTS `bill_task`;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE `user` (
    `id` BIGINT(20) NOT NULL COMMENT '用户ID（雪花算法生成）',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `name` VARCHAR(50) DEFAULT NULL COMMENT '姓名',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像路径',
    `status` INT(1) DEFAULT 0 COMMENT '状态：0-正常，1-禁用',
    `current_family_id` BIGINT(20) DEFAULT NULL COMMENT '当前选择的家庭ID',
    `created_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT(20) DEFAULT NULL COMMENT '修改人ID',
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
    `id` BIGINT(20) NOT NULL COMMENT '家庭ID（雪花算法生成）',
    `name` VARCHAR(100) NOT NULL COMMENT '家庭名称',
    `owner_id` BIGINT(20) NOT NULL COMMENT '创建者ID（家庭管理员）',
    `created_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT(20) DEFAULT NULL COMMENT '修改人ID',
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
    `id` BIGINT(20) NOT NULL COMMENT '关联ID（雪花算法生成）',
    `family_id` BIGINT(20) NOT NULL COMMENT '家庭ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `role` INT(1) DEFAULT 0 COMMENT '角色：1-管理员，0-普通成员',
    `created_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT(20) DEFAULT NULL COMMENT '修改人ID',
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
    `id` BIGINT(20) NOT NULL COMMENT '申请ID（雪花算法生成）',
    `family_id` BIGINT(20) NOT NULL COMMENT '家庭ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '申请人ID',
    `status` INT(1) DEFAULT 0 COMMENT '状态：0-待审核，1-已批准，2-已拒绝',
    `note` VARCHAR(500) DEFAULT NULL COMMENT '申请备注',
    `created_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT(20) DEFAULT NULL COMMENT '修改人ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT(1) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭申请表';

-- ============================================
-- 5. 收支类别表
-- ============================================
CREATE TABLE `category` (
    `id` BIGINT(20) NOT NULL COMMENT '类别ID（雪花算法生成）',
    `name` VARCHAR(50) NOT NULL COMMENT '类别名称',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '类别图标',
    `sort_order` INT(11) DEFAULT 0 COMMENT '排序序号（数字越小越靠前）',
    `status` INT(1) DEFAULT 1 COMMENT '是否启用：1-启用，0-禁用',
    `family_id` BIGINT(20) DEFAULT NULL COMMENT '家庭ID（NULL表示系统默认分类，有值表示该家庭的自定义分类）',
    `type` INT(1) NOT NULL COMMENT '类别类型：0-收入，1-支出',
    `created_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT(20) DEFAULT NULL COMMENT '修改人ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT(1) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name_type_family` (`name`, `type`, `family_id`, `deleted`),
    KEY `idx_status` (`status`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_type` (`type`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收支类别表';

-- ============================================
-- 6. 交易记录表
-- ============================================
CREATE TABLE `transactions` (
    `id` BIGINT(20) NOT NULL COMMENT '交易记录ID（雪花算法生成）',
    `family_id` BIGINT(20) NOT NULL COMMENT '家庭ID',
    `type` INT(1) NOT NULL COMMENT '交易类型：0-收入，1-支出',
    `amount` DECIMAL(20,2) NOT NULL COMMENT '金额',
    `category_id` BIGINT(20) NOT NULL COMMENT '类别ID（关联category表）',
    `pay_method` VARCHAR(50) DEFAULT NULL COMMENT '支付方式',
    `date` DATE NOT NULL COMMENT '交易日期',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `trade_no` VARCHAR(100) DEFAULT NULL COMMENT '交易单号（用于去重，账单导入时使用）',
    `platform` VARCHAR(50) DEFAULT NULL COMMENT '平台来源（支付宝、微信、京东、招商银行等，账单导入时使用）',
    `created_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT(20) DEFAULT NULL COMMENT '修改人ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT(1) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_date` (`date`),
    KEY `idx_type` (`type`),
    KEY `idx_family_date` (`family_id`, `date`),
    KEY `idx_family_trade_no` (`family_id`, `trade_no`),
    KEY `idx_platform` (`platform`),
    KEY `idx_family_platform` (`family_id`, `platform`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易记录表';

-- ============================================
-- 7. 操作日志表
-- ============================================
CREATE TABLE `operation_logs` (
    `id` BIGINT(20) NOT NULL COMMENT '日志ID（雪花算法生成）',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '操作描述',
    `url` VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
    `method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `params` TEXT DEFAULT NULL COMMENT '请求参数',
    `exception` TEXT DEFAULT NULL COMMENT '异常信息',
    `created_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT(20) DEFAULT NULL COMMENT '修改人ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT(1) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_url` (`url`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================
-- 8. 预算表
-- ============================================
CREATE TABLE `budget` (
    `id` BIGINT(20) NOT NULL COMMENT '预算ID（雪花算法生成）',
    `family_id` BIGINT(20) NOT NULL COMMENT '家庭ID',
    `amount` DECIMAL(20,2) NOT NULL COMMENT '预算金额',
    `created_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT(20) DEFAULT NULL COMMENT '修改人ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT(1) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_family_year_month_type` (`family_id`, `deleted`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算表';

-- ============================================
-- 9. 账单上传记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `bill_upload` (
    `id` BIGINT(20) NOT NULL COMMENT 'ID（雪花算法生成）',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `family_id` BIGINT(20) NOT NULL COMMENT '家庭ID',
    `file_id` VARCHAR(100) NOT NULL COMMENT '文件ID（OSS objectKey）',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_url` VARCHAR(500) COMMENT '文件URL',
    `file_size` BIGINT(20) COMMENT '文件大小（字节）',
    `platform` VARCHAR(50) COMMENT '平台名称（支付宝、微信、招商银行等）',
    `status` INT(1) DEFAULT 0 COMMENT '状态：0-已上传，1-已解析，2-已导入，3-导入失败',
    `total_count` INT(11) COMMENT '总记录数',
    `success_count` INT(11) COMMENT '成功数',
    `error_count` INT(11) COMMENT '错误数',
    `parse_result` TEXT COMMENT '解析结果（JSON字符串）',
    `created_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT(20) DEFAULT NULL COMMENT '修改人ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT(1) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_file_id` (`file_id`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单上传记录表';

-- ============================================
-- 10. 账单任务表
-- ============================================
CREATE TABLE `bill_task` (
    `id` BIGINT(20) NOT NULL COMMENT '任务ID（雪花算法生成）',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `family_id` BIGINT(20) NOT NULL COMMENT '家庭ID',
    `bill_upload_id` BIGINT(20) NOT NULL COMMENT '账单上传ID',
    `original_file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_size` BIGINT(20) COMMENT '文件大小（字节）',
    `oss_file_path` VARCHAR(100) NOT NULL COMMENT 'OSS文件路径（objectKey）',
    `file_url` VARCHAR(500) COMMENT '文件URL',
    `task_type` INT(1) NOT NULL COMMENT '任务类型：1-上传解析，2-导入',
    `status` INT(1) DEFAULT 0 COMMENT '任务状态：0-待处理，1-处理中，2-成功，3-失败',
    `progress` INT(3) DEFAULT 0 COMMENT '处理进度（0-100）',
    `total_count` INT(11) COMMENT '总记录数',
    `success_count` INT(11) COMMENT '成功记录数',
    `fail_count` INT(11) COMMENT '失败记录数',
    `error_message` TEXT COMMENT '错误信息',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始处理时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束处理时间',
    `platform` VARCHAR(50) COMMENT '平台类型（支付宝、微信、京东等）',
    `created_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT(20) DEFAULT NULL COMMENT '修改人ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT(1) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_bill_upload_id` (`bill_upload_id`),
    KEY `idx_task_type` (`task_type`),
    KEY `idx_status` (`status`),
    KEY `idx_progress` (`progress`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_platform` (`platform`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单任务表';

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
