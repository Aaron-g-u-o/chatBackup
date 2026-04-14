-- 为已有的文字频道创建Room并更新roomId
-- 首先为每个文字频道创建Room记录
INSERT INTO `room` (`type`, `hot_flag`, `create_time`, `update_time`)
SELECT 2, 0, NOW(), NOW()
FROM `channel` 
WHERE `type` = 1 AND `room_id` IS NULL AND `status` = 1;

-- 然后创建RoomGroup记录并更新Channel的roomId
-- 这里需要手动处理，因为需要关联Room和Channel
-- 可以通过以下步骤：

-- 1. 获取刚创建的Room ID（假设从某个起始ID开始）
-- 2. 更新Channel的room_id

-- 简单起见，使用存储过程或手动执行以下SQL：

-- 查看需要更新的频道
SELECT c.id, c.name, c.guild_id 
FROM `channel` c 
WHERE c.type = 1 AND c.room_id IS NULL AND c.status = 1;

-- 为每个文字频道手动创建Room和RoomGroup，然后更新
-- 示例（需要根据实际情况调整）：
-- SET @roomId = LAST_INSERT_ID();
-- UPDATE `channel` SET `room_id` = @roomId WHERE `id` = 频道ID;
