-- ============================================================
-- P0-5 兼容期对账（只读）
-- 用途：P0-3/P0-4 回填后、前端逐页切换前，核对旧列/新列/新表是否一致。
-- ============================================================

SET NAMES utf8mb4;

-- 1) 旧 type vs 新 notice_type_code
SELECT `type` AS `old_type`, `notice_type_code` AS `new_type`, COUNT(*) AS `cnt`
FROM `notice`
WHERE `deleted` = b'0'
GROUP BY `type`, `notice_type_code`
ORDER BY `cnt` DESC;

-- 2) 新列空值统计
SELECT
  SUM(`notice_type_code` = '') AS `empty_notice_type`,
  SUM(`bid_deadline` IS NULL AND `deadline` IS NOT NULL) AS `deadline_not_synced`,
  SUM(`source_site_code` = '') AS `empty_source_site`,
  SUM(`province_code` = '') AS `empty_province_code`,
  SUM(`city_code` = '') AS `empty_city_code`,
  SUM(`missing_fields` IS NULL) AS `empty_missing_fields`
FROM `notice`
WHERE `deleted` = b'0';

-- 3) 旧地区中文名 vs 新地区代码（抽样）
SELECT `province`, `province_code`, `city`, `city_code`, COUNT(*) AS `cnt`
FROM `notice`
WHERE `deleted` = b'0'
GROUP BY `province`, `province_code`, `city`, `city_code`
ORDER BY `cnt` DESC;

-- 4) 四层归并完整度
SELECT
  (SELECT COUNT(*) FROM `notice` WHERE `deleted` = b'0') AS `notice_total`,
  (SELECT COUNT(*) FROM `notice` WHERE `deleted` = b'0' AND `project_id` IS NOT NULL) AS `notice_with_project`,
  (SELECT COUNT(*) FROM `notice` WHERE `deleted` = b'0' AND `tender_project_id` IS NOT NULL) AS `notice_with_tender_project`,
  (SELECT COUNT(*) FROM `notice` WHERE `deleted` = b'0' AND `lot_id` IS NOT NULL) AS `notice_with_lot`,
  (SELECT COUNT(*) FROM `project` WHERE `deleted` = b'0') AS `project_total`,
  (SELECT COUNT(*) FROM `tender_project` WHERE `deleted` = b'0') AS `tender_project_total`,
  (SELECT COUNT(*) FROM `tender_lot` WHERE `deleted` = b'0') AS `tender_lot_total`;

-- 5) 企业关联完整度
SELECT
  (SELECT COUNT(*) FROM `notice` WHERE `deleted` = b'0' AND `tender_person` <> '' AND `tenderer_enterprise_id` IS NULL) AS `tenderer_unlinked`,
  (SELECT COUNT(*) FROM `notice` WHERE `deleted` = b'0' AND `agency` <> '' AND `agency_enterprise_id` IS NULL) AS `agency_unlinked`,
  (SELECT COUNT(*) FROM `notice_enterprise` WHERE `deleted` = b'0' AND `role_code` = 'TENDERER') AS `tenderer_links`,
  (SELECT COUNT(*) FROM `notice_enterprise` WHERE `deleted` = b'0' AND `role_code` = 'AGENCY') AS `agency_links`;

-- 6) 数据质量状态分布
SELECT `data_quality_status`, COUNT(*) AS `cnt`
FROM `notice`
WHERE `deleted` = b'0'
GROUP BY `data_quality_status`;

-- 7) 采集配置：JSON 频道数 vs 落表频道数
SELECT
  `s`.`code`,
  CASE WHEN `s`.`channels` IS NULL OR `s`.`channels` = '' OR JSON_VALID(`s`.`channels`) = 0
       THEN 0 ELSE JSON_LENGTH(`s`.`channels`) END AS `json_channels`,
  (SELECT COUNT(*) FROM `crawler_channel` `c` WHERE `c`.`site_code` = `s`.`code` AND `c`.`deleted` = b'0') AS `db_channels`
FROM `crawler_site` `s`
WHERE `s`.`deleted` = b'0'
ORDER BY `s`.`id`;
