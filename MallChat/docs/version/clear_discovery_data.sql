-- 清空社区发现推荐系统相关数据

-- 清空推荐追踪记录
TRUNCATE TABLE recommendation_track;

-- 清空用户不感兴趣记录
TRUNCATE TABLE user_dismiss_record;

-- 清空用户兴趣标签
TRUNCATE TABLE user_interest_tag;

-- 清空用户兴趣画像
TRUNCATE TABLE user_interest_profile;

-- 清空服务器活跃度统计
TRUNCATE TABLE guild_activity_stats;

-- 清空服务器标签关联
TRUNCATE TABLE guild_tag_relation;

-- 清空服务器数据（保留服务器成员）
UPDATE guild SET member_count = 1, status = 0 WHERE id > 1;

-- 删除非管理员的服务器成员（保留服务器创建者）
DELETE FROM guild_member WHERE role_id != 2;

-- 重置服务器ID从1开始（可选，如需重新计数）
-- ALTER TABLE guild AUTO_INCREMENT = 1;
-- ALTER TABLE guild_member AUTO_INCREMENT = 1;
-- ALTER TABLE guild_tag AUTO_INCREMENT = 1;
-- ALTER TABLE guild_tag_relation AUTO_INCREMENT = 1;
-- ALTER TABLE guild_activity_stats AUTO_INCREMENT = 1;
