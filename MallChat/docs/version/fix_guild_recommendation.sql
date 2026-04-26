-- 检查并修复服务器数据，使其能被推荐

-- 1. 查看当前服务器状态
SELECT id, name, is_public, status, member_count, category FROM guild;

-- 2. 将所有服务器设置为公开且活跃
UPDATE guild SET is_public = 1, status = 1 WHERE status IS NULL OR is_public IS NULL;

-- 3. 为没有分类的服务器设置默认分类
UPDATE guild SET category = '其他' WHERE category IS NULL OR category = '';

-- 4. 为服务器设置默认标签（可选）
UPDATE guild SET tags = '["社交"]' WHERE tags IS NULL AND category = '社交';
UPDATE guild SET tags = '["技术"]' WHERE tags IS NULL AND category = '技术';
UPDATE guild SET tags = '["游戏"]' WHERE tags IS NULL AND category = '游戏';
UPDATE guild SET tags = '["学习"]' WHERE tags IS NULL AND category = '学习';
UPDATE guild SET tags = '["娱乐"]' WHERE tags IS NULL AND category = '娱乐';

-- 5. 设置默认语言
UPDATE guild SET language = 'zh-CN' WHERE language IS NULL;

-- 6. 设置活跃等级
UPDATE guild SET activity_level = 2 WHERE activity_level IS NULL;

-- 7. 初始化服务器活跃度统计（用于趋势推荐）
INSERT INTO guild_activity_stats (guild_id, daily_active_users, weekly_active_users, daily_messages, weekly_messages, join_count7d, trending_score, quality_score)
SELECT 
    id,
    COALESCE(member_count, 1) * 0.3,
    COALESCE(member_count, 1) * 0.6,
    COALESCE(member_count, 1) * 5,
    COALESCE(member_count, 1) * 30,
    0,
    COALESCE(member_count, 1) / 100.0,
    0.5
FROM guild 
WHERE status = 1 AND is_public = 1
ON DUPLICATE KEY UPDATE 
    daily_active_users = VALUES(daily_active_users),
    trending_score = VALUES(trending_score);

-- 8. 为服务器添加标签关联（需要先有标签数据）
-- 假设标签ID从1开始，将服务器与标签关联
INSERT IGNORE INTO guild_tag_relation (guild_id, tag_id, relevance_score)
SELECT g.id, gt.id, 1.0
FROM guild g
CROSS JOIN guild_tag gt
WHERE g.category = gt.category AND g.status = 1;
