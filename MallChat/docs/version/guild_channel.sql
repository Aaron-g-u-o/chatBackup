-- 服务器表
CREATE TABLE IF NOT EXISTS `guild` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '服务器ID',
    `name` VARCHAR(128) NOT NULL COMMENT '服务器名称',
    `icon` VARCHAR(512) DEFAULT NULL COMMENT '服务器图标',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '服务器描述',
    `owner_uid` BIGINT NOT NULL COMMENT '服务器创建者UID',
    `max_members` INT DEFAULT 100 COMMENT '最大成员数',
    `member_count` INT DEFAULT 1 COMMENT '当前成员数',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-已解散',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_owner_uid` (`owner_uid`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器表';

-- 服务器成员表
CREATE TABLE IF NOT EXISTS `guild_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `guild_id` BIGINT NOT NULL COMMENT '服务器ID',
    `uid` BIGINT NOT NULL COMMENT '用户ID',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '服务器内昵称',
    `role_id` INT DEFAULT 0 COMMENT '角色：0-普通成员，1-管理员，2-服务器主',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-已退出',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_guild_uid` (`guild_id`, `uid`),
    KEY `idx_uid` (`uid`),
    KEY `idx_guild_id` (`guild_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器成员表';

-- 频道表
CREATE TABLE IF NOT EXISTS `channel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '频道ID',
    `guild_id` BIGINT NOT NULL COMMENT '所属服务器ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父频道ID(分组)',
    `name` VARCHAR(64) NOT NULL COMMENT '频道名称',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '频道类型：0-分组，1-文字，2-语音',
    `topic` VARCHAR(256) DEFAULT NULL COMMENT '频道主题',
    `position` INT DEFAULT 0 COMMENT '排序位置',
    `max_users` INT DEFAULT 0 COMMENT '语音频道最大人数(0为不限)',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-已删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_guild_id` (`guild_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='频道表';

-- 频道成员表(语音频道在线用户)
CREATE TABLE IF NOT EXISTS `channel_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `channel_id` BIGINT NOT NULL COMMENT '频道ID',
    `uid` BIGINT NOT NULL COMMENT '用户ID',
    `muted` TINYINT DEFAULT 0 COMMENT '是否静音',
    `deafened` TINYINT DEFAULT 0 COMMENT '是否闭麦',
    `speaking` TINYINT DEFAULT 0 COMMENT '是否正在说话',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_channel_uid` (`channel_id`, `uid`),
    KEY `idx_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='频道成员表';

-- 初始化默认服务器和频道
INSERT INTO `guild` (`id`, `name`, `icon`, `description`, `owner_uid`, `member_count`) 
VALUES (1, '官方服务器', NULL, 'MallChat官方服务器', 1, 1);

INSERT INTO `channel` (`guild_id`, `name`, `type`, `topic`, `position`) VALUES
(1, '文字频道', 0, NULL, 0),
(1, '综合讨论', 1, '欢迎大家来聊天', 1),
(1, '语音频道', 0, NULL, 2),
(1, '语音大厅', 2, '语音聊天室', 3);
