-- ============================================================
-- PrepPal 数据库建表脚本（MVP）
-- 与 AI-CONTEXT.md §1.2 保持一致
--
-- 执行方式：
--   mysql -u root -p < schema.sql
-- 注意：所有外键均在应用层做归属校验（Service 层按 user_id 过滤），
--       数据库层不强制外键约束以避免级联删除风险，但逻辑上必须一致。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `preppal` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `preppal`;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `openid` VARCHAR(64) NOT NULL UNIQUE COMMENT '微信 openid',
  `nickname` VARCHAR(64) COMMENT '昵称',
  `avatar_url` VARCHAR(512) COMMENT '头像',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

-- 课程表
CREATE TABLE IF NOT EXISTS `course` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '归属用户（逻辑外键 → user.id）',
  `name` VARCHAR(128) NOT NULL COMMENT '课程名称',
  `description` VARCHAR(512) COMMENT '课程简介',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程';

-- 教学资料表（RAG 知识库）
CREATE TABLE IF NOT EXISTS `material` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '归属用户',
  `course_id` BIGINT COMMENT '归属课程（可选）',
  `filename` VARCHAR(256) NOT NULL,
  `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `mime_type` VARCHAR(64) COMMENT 'MIME 类型',
  `content` LONGTEXT COMMENT '解析后的全文文本',
  `content_hash` VARCHAR(64) COMMENT '内容 SHA-256，用于去重',
  `parse_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SUCCESS / FAILED',
  `error_message` VARCHAR(1024) COMMENT '解析失败原因',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (`user_id`),
  INDEX idx_course_id (`course_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教学资料';

-- 资料分片表（向量持久化，应用侧余弦检索）
CREATE TABLE IF NOT EXISTS `material_chunk` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `material_id` BIGINT NOT NULL COMMENT '归属资料',
  `user_id` BIGINT NOT NULL COMMENT '归属用户（冗余，便于按用户检索）',
  `chunk_index` INT NOT NULL COMMENT '分片序号',
  `content` TEXT NOT NULL COMMENT '分片文本',
  `vector` JSON COMMENT 'bge-m3 向量（1024 维），JSON 数组存储',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_material_id (`material_id`),
  INDEX idx_user_id (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资料分片';

-- 教案表
CREATE TABLE IF NOT EXISTS `lesson_plan` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(256) NOT NULL COMMENT '教案标题',
  `topic` VARCHAR(256) COMMENT '教学主题',
  `target_audience` VARCHAR(128) COMMENT '授课对象',
  `class_duration` INT COMMENT '课时长（分钟）',
  `teaching_type` VARCHAR(16) COMMENT '理论/实验/研讨/习题',
  `objectives` TEXT COMMENT '教学目标',
  `key_points` TEXT COMMENT '教学重点',
  `difficult_points` TEXT COMMENT '教学难点',
  `teaching_process` TEXT COMMENT '教学过程（JSON）',
  `blackboard_design` TEXT COMMENT '板书设计',
  `homework` TEXT COMMENT '作业布置',
  `source_references` TEXT COMMENT '参考来源（JSON）',
  `disclaimer` VARCHAR(256) COMMENT '资料不足提示',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT 草稿 / FINALIZED 定稿',
  `input_snapshot` TEXT COMMENT '生成时的输入参数快照（JSON），用于回溯',
  `model_name` VARCHAR(128) COMMENT '使用的对话模型名',
  `prompt_version` VARCHAR(32) COMMENT 'Prompt 模板版本号',
  `source_material_ids` VARCHAR(512) COMMENT '参与生成的资料 ID 列表（逗号分隔）',
  `idempotency_key` VARCHAR(64) COMMENT '幂等键，防重复生成',
  `raw_content` TEXT COMMENT 'AI 原始输出',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_course_id (`course_id`),
  INDEX idx_user_id (`user_id`),
  INDEX idx_status (`status`),
  UNIQUE KEY uk_user_idempotency (`user_id`, `idempotency_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教案';
