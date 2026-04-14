-- 为服务器表添加公开/私密和邀请码字段
ALTER TABLE `guild` ADD COLUMN `is_public` TINYINT DEFAULT 1 COMMENT '是否公开：1-公开，0-私密' AFTER `member_count`;
ALTER TABLE `guild` ADD COLUMN `invite_code` VARCHAR(16) DEFAULT NULL COMMENT '邀请码' AFTER `is_public`;

-- 添加唯一索引
ALTER TABLE `guild` ADD UNIQUE INDEX `uk_invite_code` (`invite_code`);
