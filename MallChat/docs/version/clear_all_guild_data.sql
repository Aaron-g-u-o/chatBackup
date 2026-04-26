-- 清空所有服务器(Guild)相关数据

-- 1. 清空推荐相关表
TRUNCATE TABLE recommendation_track;
TRUNCATE TABLE user_dismiss_record;
TRUNCATE TABLE user_interest_tag;
TRUNCATE TABLE user_interest_profile;
TRUNCATE TABLE guild_activity_stats;
TRUNCATE TABLE guild_tag_relation;

-- 2. 清空频道成员表（语音频道在线用户）
TRUNCATE TABLE channel_member;

-- 3. 清空频道表
TRUNCATE TABLE channel;

-- 4. 清空服务器成员表
TRUNCATE TABLE guild_member;

-- 5. 清空服务器表
TRUNCATE TABLE guild;

-- 6. 重新创建默认服务器
INSERT INTO guild (id, name, icon, description, owner_uid, member_count, is_public, invite_code, status, category, language, activity_level) 
VALUES (1, '官方服务器', NULL, 'MallChat官方服务器', 1, 1, 1, 'OFFICIAL', 1, '技术', 'zh-CN', 3);

-- 7. 重建默认频道
INSERT INTO channel (guild_id, parent_id, name, type, topic, position, status) VALUES
(1, NULL, '文字频道', 0, NULL, 0, 1),
(1, 1, '综合讨论', 1, '欢迎大家来聊天', 1, 1),
(1, NULL, '语音频道', 0, NULL, 2, 1),
(1, 3, '语音大厅', 2, '语音聊天室', 3, 1);

-- 8. 重置自增ID
ALTER TABLE guild AUTO_INCREMENT = 2;
ALTER TABLE guild_member AUTO_INCREMENT = 2;
ALTER TABLE channel AUTO_INCREMENT = 2;
ALTER TABLE channel_member AUTO_INCREMENT = 1;
