-- ============================================================
-- P0-6 删除旧列（终态）
-- 前置：P0-1 ~ P0-5 已执行，P0-3 回填完成，代码已只读写新模型。
-- 说明：项目从未上线，不再保留旧列与兼容读路径。
--       执行后 P0-2 / P0-3 回填脚本不可再运行（它们读取旧列）。
-- ============================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS `p0_drop_legacy_notice_columns`;
DELIMITER $$
CREATE PROCEDURE `p0_drop_legacy_notice_columns`()
BEGIN
  -- 新模型必须已就绪，避免误删后无列可读
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'notice_type_code'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'notice.notice_type_code 不存在，请先执行 P0-3__tender_model.sql';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'type'
  ) THEN
    -- 先删依赖旧列的索引，再删列
    IF EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND INDEX_NAME = 'idx_type_publish'
    ) THEN
      ALTER TABLE `notice` DROP INDEX `idx_type_publish`;
    END IF;

    IF EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND INDEX_NAME = 'idx_province'
    ) THEN
      ALTER TABLE `notice` DROP INDEX `idx_province`;
    END IF;

    ALTER TABLE `notice`
      DROP COLUMN `type`,
      DROP COLUMN `province`,
      DROP COLUMN `city`,
      DROP COLUMN `tender_person`,
      DROP COLUMN `agency`,
      DROP COLUMN `deadline`,
      DROP COLUMN `source`;
  END IF;
END$$
DELIMITER ;
CALL `p0_drop_legacy_notice_columns`();
DROP PROCEDURE IF EXISTS `p0_drop_legacy_notice_columns`;

-- 复核：以下查询应返回 0
SELECT COUNT(*) AS `legacy_columns`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'notice'
  AND COLUMN_NAME IN ('type', 'province', 'city', 'tender_person', 'agency', 'deadline', 'source');
