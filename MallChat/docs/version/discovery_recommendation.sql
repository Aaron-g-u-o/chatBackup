-- 社区发现推荐系统表

-- 服务器标签表
CREATE TABLE IF NOT EXISTS `guild_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `name` VARCHAR(64) NOT NULL COMMENT '标签名称',
    `category` VARCHAR(64) DEFAULT NULL COMMENT '标签分类(游戏/技术/社交/学习/娱乐/音乐/艺术/运动/其他)',
    `weight` DOUBLE DEFAULT 1.0 COMMENT '标签权重',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器标签表';

-- 服务器-标签关联表
CREATE TABLE IF NOT EXISTS `guild_tag_relation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `guild_id` BIGINT NOT NULL COMMENT '服务器ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `relevance_score` DOUBLE DEFAULT 1.0 COMMENT '相关性分数(0-1)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_guild_tag` (`guild_id`, `tag_id`),
    KEY `idx_guild_id` (`guild_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器-标签关联表';

-- 用户兴趣画像表
CREATE TABLE IF NOT EXISTS `user_interest_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uid` BIGINT NOT NULL COMMENT '用户ID',
    `interest_vector` JSON DEFAULT NULL COMMENT '兴趣向量(JSON数组,各维度权重)',
    `activity_score` DOUBLE DEFAULT 0.0 COMMENT '活跃度评分',
    `diversity_score` DOUBLE DEFAULT 0.5 COMMENT '兴趣多样性(0-1,1=最多样)',
    `privacy_level` TINYINT DEFAULT 0 COMMENT '隐私级别:0-完全个性化,1-仅基于加入记录,2-仅热门推荐',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户兴趣画像表';

-- 用户兴趣标签表
CREATE TABLE IF NOT EXISTS `user_interest_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uid` BIGINT NOT NULL COMMENT '用户ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `weight` DOUBLE DEFAULT 1.0 COMMENT '兴趣权重(0-1)',
    `source` TINYINT DEFAULT 0 COMMENT '来源:0-加入服务器,1-活跃行为,2-消息内容,3-手动选择',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid_tag` (`uid`, `tag_id`),
    KEY `idx_uid` (`uid`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户兴趣标签表';

-- 推荐追踪表
CREATE TABLE IF NOT EXISTS `recommendation_track` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uid` BIGINT NOT NULL COMMENT '用户ID',
    `guild_id` BIGINT NOT NULL COMMENT '推荐的服务器ID',
    `recommend_type` TINYINT NOT NULL COMMENT '推荐类型:0-个性化,1-热门,2-最新,3-趋势',
    `recommend_source` TINYINT DEFAULT 0 COMMENT '推荐来源:0-协同过滤,1-内容匹配,2-社交关系,3-热门排序',
    `relevance_score` DOUBLE DEFAULT 0.0 COMMENT '推荐相关性分数',
    `position` INT DEFAULT 0 COMMENT '推荐位置',
    `is_clicked` TINYINT DEFAULT 0 COMMENT '是否点击:0-否,1-是',
    `is_joined` TINYINT DEFAULT 0 COMMENT '是否加入:0-否,1-是',
    `is_dismissed` TINYINT DEFAULT 0 COMMENT '是否不感兴趣:0-否,1-是',
    `ab_group` VARCHAR(32) DEFAULT NULL COMMENT 'A/B测试分组',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_uid` (`uid`),
    KEY `idx_guild_id` (`guild_id`),
    KEY `idx_uid_type` (`uid`, `recommend_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐追踪表';

-- 用户不感兴趣记录表
CREATE TABLE IF NOT EXISTS `user_dismiss_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uid` BIGINT NOT NULL COMMENT '用户ID',
    `guild_id` BIGINT NOT NULL COMMENT '服务器ID',
    `reason` VARCHAR(256) DEFAULT NULL COMMENT '不感兴趣原因',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid_guild` (`uid`, `guild_id`),
    KEY `idx_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户不感兴趣记录表';

-- 服务器活跃度统计表
CREATE TABLE IF NOT EXISTS `guild_activity_stats` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `guild_id` BIGINT NOT NULL COMMENT '服务器ID',
    `daily_active_users` INT DEFAULT 0 COMMENT '日活跃用户数',
    `weekly_active_users` INT DEFAULT 0 COMMENT '周活跃用户数',
    `daily_messages` INT DEFAULT 0 COMMENT '日消息数',
    `weekly_messages` INT DEFAULT 0 COMMENT '周消息数',
    `join_count7d` INT DEFAULT 0 COMMENT '7日加入人数',
    `trending_score` DOUBLE DEFAULT 0.0 COMMENT '趋势分数',
    `quality_score` DOUBLE DEFAULT 0.5 COMMENT '质量评分(0-1)',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_guild_id` (`guild_id`),
    KEY `idx_trending` (`trending_score`),
    KEY `idx_quality` (`quality_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器活跃度统计表';

-- 初始化标签数据
INSERT INTO `guild_tag` (`name`, `category`, `weight`) VALUES
('游戏', '娱乐', 1.0),
('技术', '技术', 1.0),
('社交', '社交', 1.0),
('学习', '学习', 1.0),
('音乐', '音乐', 1.0),
('艺术', '艺术', 1.0),
('运动', '运动', 1.0),
('编程', '技术', 1.2),
('设计', '艺术', 1.0),
('读书', '学习', 1.0),
('电影', '娱乐', 1.0),
('动漫', '娱乐', 1.0),
('摄影', '艺术', 1.0),
('美食', '社交', 1.0),
('旅行', '社交', 1.0),
('健身', '运动', 1.0),
('篮球', '运动', 0.8),
('足球', '运动', 0.8),
('Java', '技术', 1.2),
('Python', '技术', 1.2),
('前端', '技术', 1.2),
('后端', '技术', 1.2),
('AI', '技术', 1.5),
('区块链', '技术', 1.0),
('创业', '社交', 1.0),
('投资', '社交', 1.0),
('语言学习', '学习', 1.0),
('考研', '学习', 1.0),
('职场', '社交', 1.0),
('开黑', '游戏', 1.2);

-- 为现有guild表增加分类和标签字段
ALTER TABLE `guild` ADD COLUMN `category` VARCHAR(64) DEFAULT '其他' COMMENT '服务器分类' AFTER `description`;
ALTER TABLE `guild` ADD COLUMN `tags` JSON DEFAULT NULL COMMENT '服务器标签列表' AFTER `category`;
ALTER TABLE `guild` ADD COLUMN `language` VARCHAR(32) DEFAULT 'zh-CN' COMMENT '主要语言' AFTER `tags`;
ALTER TABLE `guild` ADD COLUMN `activity_level` TINYINT DEFAULT 1 COMMENT '活跃等级:1-低,2-中,3-高' AFTER `language`;
