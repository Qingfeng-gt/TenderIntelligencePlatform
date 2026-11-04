-- ============================================================
-- P0-6 回滚：仅恢复旧列结构（数据无法恢复）
-- 说明：项目未上线，本回滚只用于结构回退演练；旧列值全部为空默认值。
-- ============================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS `p0_restore_legacy_notice_columns`;
DELIMITER $$
CREATE PROCEDURE `p0_restore_legacy_notice_columns`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'type'
  ) THEN
    ALTER TABLE `notice`
      ADD COLUMN `type` varchar(16) NOT NULL DEFAULT '' COMMENT '公告类型（已废弃）',
      ADD COLUMN `province` varchar(32) NOT NULL DEFAULT '' COMMENT '省份中文名（已废弃）',
      ADD COLUMN `city` varchar(32) NOT NULL DEFAULT '' COMMENT '城市中文名（已废弃）',
      ADD COLUMN `tender_person` varchar(128) NOT NULL DEFAULT '' COMMENT '招标人原文（已废弃）',
      ADD COLUMN `agency` varchar(128) NOT NULL DEFAULT '' COMMENT '代理机构原文（已废弃）',
      ADD COLUMN `deadline` datetime DEFAULT NULL COMMENT '投标截止（已废弃）',
      ADD COLUMN `source` varchar(128) NOT NULL DEFAULT '' COMMENT '来源网站（已废弃）',
      ADD KEY `idx_type_publish` (`type`, `publish_time`),
      ADD KEY `idx_province` (`province`);
  END IF;
END$$
DELIMITER ;
CALL `p0_restore_legacy_notice_columns`();
DROP PROCEDURE IF EXISTS `p0_restore_legacy_notice_columns`;
