-- 语音房间表
CREATE TABLE IF NOT EXISTS `voice_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(128) NOT NULL COMMENT '房间名称',
    `room_id` BIGINT DEFAULT NULL COMMENT '关联的聊天房间ID',
    `creator_uid` BIGINT NOT NULL COMMENT '创建者用户ID',
    `max_users` INT DEFAULT 10 COMMENT '最大用户数',
    `current_user_count` INT DEFAULT 0 COMMENT '当前用户数',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-活跃，0-已关闭',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音房间表';

-- 语音房间成员表
CREATE TABLE IF NOT EXISTS `voice_room_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `voice_room_id` BIGINT NOT NULL COMMENT '语音房间ID',
    `uid` BIGINT NOT NULL COMMENT '用户ID',
    `muted` TINYINT DEFAULT 0 COMMENT '是否静音：0-否，1-是',
    `deafened` TINYINT DEFAULT 0 COMMENT '是否闭麦：0-否，1-是',
    `speaking` TINYINT DEFAULT 0 COMMENT '是否正在说话：0-否，1-是',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `leave_time` DATETIME DEFAULT NULL COMMENT '离开时间',
    PRIMARY KEY (`id`),
    KEY `idx_voice_room_id` (`voice_room_id`),
    KEY `idx_uid` (`uid`),
    KEY `idx_join_time` (`join_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音房间成员表';
