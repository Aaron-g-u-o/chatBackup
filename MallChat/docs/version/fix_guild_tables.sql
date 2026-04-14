-- ==========================================
-- Guild 模块数据库修复脚本
-- 执行此脚本前请确保已执行 guild_channel.sql
-- ==========================================

-- 1. 为 guild 表添加公开/私密和邀请码字段（如果不存在）
ALTER TABLE `guild` ADD COLUMN IF NOT EXISTS `is_public` TINYINT DEFAULT 1 COMMENT '是否公开：1-公开，0-私密' AFTER `member_count`;
ALTER TABLE `guild` ADD COLUMN IF NOT EXISTS `invite_code` VARCHAR(16) DEFAULT NULL COMMENT '邀请码' AFTER `is_public`;

-- 添加唯一索引（如果不存在）
CREATE UNIQUE INDEX IF NOT EXISTS `uk_invite_code` ON `guild` (`invite_code`);

-- 2. 为 channel 表添加 room_id 字段（如果不存在）
ALTER TABLE `channel` ADD COLUMN IF NOT EXISTS `room_id` BIGINT DEFAULT NULL COMMENT '关联的房间ID(文字频道)' AFTER `max_users`;

-- 添加索引（如果不存在）
CREATE INDEX IF NOT EXISTS `idx_room_id` ON `channel` (`room_id`);

-- 3. 为已有的 guild 生成邀请码
UPDATE `guild` SET `invite_code` = UPPER(SUBSTRING(MD5(RAND()), 1, 8)) WHERE `invite_code` IS NULL;

-- 4. 为已有的文字频道创建 Room 和 RoomGroup
-- 注意：这个操作需要在应用层完成，或者手动执行以下步骤：

-- 查看需要处理的文字频道
SELECT c.id, c.name, c.guild_id, c.room_id 
FROM `channel` c 
WHERE c.type = 1 AND c.room_id IS NULL AND c.status = 1;
