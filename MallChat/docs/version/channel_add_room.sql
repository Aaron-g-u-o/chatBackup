-- 为频道表添加room_id字段
ALTER TABLE `channel` ADD COLUMN `room_id` BIGINT DEFAULT NULL COMMENT '关联的房间ID(文字频道)' AFTER `max_users`;

-- 添加索引
ALTER TABLE `channel` ADD INDEX `idx_room_id` (`room_id`);
