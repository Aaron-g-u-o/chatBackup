-- 为 channel_member 表添加 volume 字段
ALTER TABLE channel_member ADD COLUMN volume INT DEFAULT 100 COMMENT '音量值(0-200)';

-- 如果表不存在则创建
CREATE TABLE IF NOT EXISTS channel_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_id BIGINT NOT NULL COMMENT '频道ID',
    uid BIGINT NOT NULL COMMENT '用户ID',
    muted INT DEFAULT 0 COMMENT '是否静音 0-否 1-是',
    deafened INT DEFAULT 0 COMMENT '是否闭麦 0-否 1-是',
    speaking INT DEFAULT 0 COMMENT '是否正在说话 0-否 1-是',
    volume INT DEFAULT 100 COMMENT '音量值(0-200)',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY uk_channel_uid (channel_id, uid),
    KEY idx_channel_id (channel_id),
    KEY idx_uid (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音频道成员表';
