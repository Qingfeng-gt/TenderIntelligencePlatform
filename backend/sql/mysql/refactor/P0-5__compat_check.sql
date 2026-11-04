-- ============================================================
-- P0-5 终态对账（只读）
-- 用途：P0-6 删除旧列后，核对新模型列、四层归并、企业与采集配置的完整度。
-- ============================================================

SET NAMES utf8mb4;

-- 1) 旧列是否已清理（正常应为 0）
SELECT COUNT(*) AS `legacy_columns`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'notice'
  AND COLUMN_NAME IN ('type', 'province', 'city', 'tender_person', 'agency', 'deadline', 'source');

-- 2) 新列空值统计
SELECT
  SUM(`notice_type_code` = '') AS `empty_notice_type`,
  SUM(`source_site_code` = '') AS `empty_source_site`,
  SUM(`province_code` = '') AS `empty_province_code`,
  SUM(`city_code` = '') AS `empty_city_code`,
  SUM(`missing_fields` IS NULL) AS `empty_missing_fields`
FROM `notice`
WHERE `deleted` = b'0';

-- 3) 四层归并完整度
SELECT
  (SELECT COUNT(*) FROM `notice` WHERE `deleted` = b'0') AS `notice_total`,
  (SELECT COUNT(*) FROM `notice` WHERE `deleted` = b'0' AND `project_id` IS NOT NULL) AS `notice_with_project`,
  (SELECT COUNT(*) FROM `notice` WHERE `deleted` = b'0' AND `tender_project_id` IS NOT NULL) AS `notice_with_tender_project`,
  (SELECT COUNT(*) FROM `notice` WHERE `deleted` = b'0' AND `lot_id` IS NOT NULL) AS `notice_with_lot`,
  (SELECT COUNT(*) FROM `project` WHERE `deleted` = b'0') AS `project_total`,
  (SELECT COUNT(*) FROM `tender_project` WHERE `deleted` = b'0') AS `tender_project_total`,
  (SELECT COUNT(*) FROM `tender_lot` WHERE `deleted` = b'0') AS `tender_lot_total`;

-- 4) 企业关联完整度
SELECT
  (SELECT COUNT(*) FROM `notice` WHERE `deleted` = b'0' AND `tenderer_enterprise_id` IS NULL) AS `tenderer_unlinked`,
  (SELECT COUNT(*) FROM `notice` WHERE `deleted` = b'0' AND `agency_enterprise_id` IS NULL) AS `agency_unlinked`,
  (SELECT COUNT(*) FROM `notice_enterprise` WHERE `deleted` = b'0' AND `role_code` = 'TENDERER') AS `tenderer_links`,
  (SELECT COUNT(*) FROM `notice_enterprise` WHERE `deleted` = b'0' AND `role_code` = 'AGENCY') AS `agency_links`;

-- 5) 数据质量状态分布
SELECT `data_quality_status`, COUNT(*) AS `cnt`
FROM `notice`
WHERE `deleted` = b'0'
GROUP BY `data_quality_status`;

-- 6) 采集频道配置
SELECT
  `s`.`code`,
  (SELECT COUNT(*) FROM `crawler_channel` `c` WHERE `c`.`site_code` = `s`.`code` AND `c`.`deleted` = b'0') AS `db_channels`
FROM `crawler_site` `s`
WHERE `s`.`deleted` = b'0'
ORDER BY `s`.`id`;
