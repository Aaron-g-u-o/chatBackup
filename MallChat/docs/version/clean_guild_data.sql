-- ==========================================
-- 清理 Guild 模块相关数据
-- 执行此脚本将删除所有服务器、频道相关数据
-- 注意：请先备份数据！
-- ==========================================

-- 1. 先记录需要删除的 room_id
CREATE TEMPORARY TABLE temp_channel_rooms AS 
SELECT DISTINCT room_id FROM `channel` WHERE room_id IS NOT NULL;

-- 2. 删除频道成员表数据
DELETE FROM `channel_member`;

-- 3. 删除群成员表中与频道房间相关的记录
DELETE FROM `group_member` WHERE group_id IN (
    SELECT id FROM `room_group` WHERE room_id IN (SELECT room_id FROM temp_channel_rooms)
);

-- 4. 删除房间组表中与频道相关的记录
DELETE FROM `room_group` WHERE room_id IN (SELECT room_id FROM temp_channel_rooms);

-- 5. 删除会话表中与频道相关的记录
DELETE FROM `contact` WHERE room_id IN (SELECT room_id FROM temp_channel_rooms);

-- 6. 删除房间表中与频道相关的记录
DELETE FROM `room` WHERE id IN (SELECT room_id FROM temp_channel_rooms);

-- 7. 删除频道表数据
DELETE FROM `channel`;

-- 8. 删除服务器成员表数据
DELETE FROM `guild_member`;

-- 9. 删除服务器表数据
DELETE FROM `guild`;

-- 10. 删除临时表
DROP TEMPORARY TABLE IF EXISTS temp_channel_rooms;

-- 重置自增ID（可选）
ALTER TABLE `channel_member` AUTO_INCREMENT = 1;
ALTER TABLE `channel` AUTO_INCREMENT = 1;
ALTER TABLE `guild_member` AUTO_INCREMENT = 1;
ALTER TABLE `guild` AUTO_INCREMENT = 1;

SELECT 'Guild 数据清理完成' AS result;
