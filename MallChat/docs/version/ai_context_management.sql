-- AI上下文管理系统表

-- AI会话表
CREATE TABLE IF NOT EXISTS `ai_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `uid` BIGINT NOT NULL COMMENT '用户ID',
    `room_id` BIGINT DEFAULT NULL COMMENT '房间ID',
    `ai_type` TINYINT NOT NULL DEFAULT 0 COMMENT 'AI类型:0-GPT,1-GLM,2-其他',
    `title` VARCHAR(256) DEFAULT NULL COMMENT '会话标题',
    `system_prompt` TEXT COMMENT '系统提示词',
    `context_window` INT DEFAULT 10 COMMENT '上下文窗口大小(消息对数)',
    `total_tokens` INT DEFAULT 0 COMMENT '累计使用token数',
    `message_count` INT DEFAULT 0 COMMENT '消息数',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否活跃:0-否,1-是',
    `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_uid` (`uid`),
    KEY `idx_uid_room` (`uid`, `room_id`),
    KEY `idx_last_message` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话表';

-- AI对话历史表
CREATE TABLE IF NOT EXISTS `ai_message_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `role` VARCHAR(32) NOT NULL COMMENT '角色:user/assistant/system',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `token_count` INT DEFAULT 0 COMMENT 'token数量',
    `intent_tags` VARCHAR(512) DEFAULT NULL COMMENT '意图标签(逗号分隔)',
    `metadata` JSON DEFAULT NULL COMMENT '元数据(JSON)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_intent_tags` (`intent_tags`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话历史表';

-- 用户意图追踪表
CREATE TABLE IF NOT EXISTS `ai_user_intent` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `uid` BIGINT NOT NULL COMMENT '用户ID',
    `session_id` BIGINT DEFAULT NULL COMMENT '会话ID',
    `intent_type` VARCHAR(64) DEFAULT NULL COMMENT '意图类型:question/chat/task/feedback/other',
    `intent_keywords` VARCHAR(512) DEFAULT NULL COMMENT '意图关键词',
    `confidence` DOUBLE DEFAULT 0.5 COMMENT '置信度(0-1)',
    `context_before` TEXT COMMENT '前文摘要',
    `entities` JSON DEFAULT NULL COMMENT '识别的实体(JSON数组)',
    `followup_needed` TINYINT DEFAULT 0 COMMENT '是否需要跟进:0-否,1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_uid` (`uid`),
    KEY `idx_session` (`session_id`),
    KEY `idx_intent_type` (`intent_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户意图追踪表';

-- AI系统提示词模板表
CREATE TABLE IF NOT EXISTS `ai_system_prompt` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `name` VARCHAR(128) NOT NULL COMMENT '模板名称',
    `prompt_type` VARCHAR(64) NOT NULL COMMENT '提示词类型:general/technical/creative/custom',
    `content` TEXT NOT NULL COMMENT '提示词内容',
    `variables` JSON DEFAULT NULL COMMENT '变量定义(JSON)',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否为默认:0-否,1-是',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用:0-否,1-是',
    `usage_count` INT DEFAULT 0 COMMENT '使用次数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_type` (`prompt_type`),
    KEY `idx_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI系统提示词模板表';

-- 初始化默认系统提示词
INSERT INTO `ai_system_prompt` (`name`, `prompt_type`, `content`, `is_default`, `is_active`) VALUES
('通用助手', 'general', '你是一个友好、有帮助的AI助手。请用清晰、准确的中文回答用户的问题。如果不确定答案，请诚实告知用户。保持对话简洁有序。', 1, 1),
('技术专家', 'technical', '你是一个技术专家。回答问题时要专业、准确，提供代码示例时要注意语法正确性。如果问题涉及多个技术点，请逐一分析。', 0, 1),
('创意作家', 'creative', '你是一个创意作家。可以用生动、有趣的方式表达观点。适当使用比喻、例子来解释概念。', 0, 1);

-- AI上下文配置表
CREATE TABLE IF NOT EXISTS `ai_context_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `uid` BIGINT NOT NULL COMMENT '用户ID',
    `ai_type` TINYINT NOT NULL DEFAULT 0 COMMENT 'AI类型',
    `max_context_window` INT DEFAULT 10 COMMENT '最大上下文窗口',
    `enable_intent_tracking` TINYINT DEFAULT 1 COMMENT '启用意图追踪:0-否,1-是',
    `enable_relevance_filter` TINYINT DEFAULT 1 COMMENT '启用相关性过滤:0-否,1-是',
    `relevance_threshold` DOUBLE DEFAULT 0.3 COMMENT '相关性阈值',
    `custom_prompt_id` BIGINT DEFAULT NULL COMMENT '自定义提示词ID',
    `diversity_ratio` DOUBLE DEFAULT 0.2 COMMENT '多样性比例(0-1)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid_ai_type` (`uid`, `ai_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI上下文配置表';

-- AI性能指标表
CREATE TABLE IF NOT EXISTS `ai_performance_metrics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `prompt_tokens` INT DEFAULT 0 COMMENT '提示词token数',
    `completion_tokens` INT DEFAULT 0 COMMENT '回答token数',
    `total_tokens` INT DEFAULT 0 COMMENT '总token数',
    `latency_ms` INT DEFAULT 0 COMMENT '响应延迟(毫秒)',
    `user_rating` TINYINT DEFAULT NULL COMMENT '用户评分(1-5)',
    `feedback` TEXT COMMENT '用户反馈',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session` (`session_id`),
    KEY `idx_rating` (`user_rating`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI性能指标表';
