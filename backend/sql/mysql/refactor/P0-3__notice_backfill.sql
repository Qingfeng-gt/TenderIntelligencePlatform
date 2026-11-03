-- ============================================================
-- P0-3 标讯四层回填
-- 目标库: tender
-- 前置: P0-1__base_data.sql、P0-2__enterprise.sql、P0-2__enterprise_backfill.sql、P0-3__tender_model.sql
-- 作用:
--   1) 把旧 notice 的 type/deadline/source 等同步到新列
--   2) 为每条 project_key 生成 project / tender_project / tender_lot（无标段时 DEFAULT）
--   3) 回填 notice.project_id / tender_project_id / lot_id
--   4) 建立 notice_enterprise（TENDERER / AGENCY）并回填企业 ID
-- 幂等: 是。project 按 uk_project_key；tender_project / lot 按 creator='P0-BACKFILL' 的 NOT EXISTS；
--       企业关联按唯一键 (notice_id, enterprise_id, role_code)。
-- 清理: P0-3__notice_backfill_cleanup.sql
-- 校验: P0-3__notice_validate.sql
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1) 旧列 → 新列（标量字段）
-- ------------------------------------------------------------
UPDATE `notice`
SET
  `notice_type_code` = CASE
    WHEN `notice_type_code` = '' THEN COALESCE(NULLIF(`type`, ''), 'tender')
    ELSE `notice_type_code`
  END,
  `bid_deadline` = COALESCE(`bid_deadline`, `deadline`),
  `source_platform` = CASE
    WHEN `source_platform` = '' THEN COALESCE(`source`, '')
    ELSE `source_platform`
  END,
  `source_site_code` = CASE
    WHEN `source_site_code` <> '' THEN `source_site_code`
    WHEN `source` = '中国政府采购网' THEN 'ccgp'
    WHEN `source` = '全国公共资源交易平台' THEN 'ggzy'
    WHEN `source` = '贵州省招标投标公共服务平台' THEN 'ztb_gz'
    ELSE ''
  END,
  `notice_no` = CASE
    WHEN `notice_no` = '' THEN COALESCE(`project_no`, '')
    ELSE `notice_no`
  END,
  `lot_no` = CASE
    WHEN `lot_no` = '' THEN 'DEFAULT'
    ELSE `lot_no`
  END,
  `parser_version` = CASE
    WHEN `parser_version` = '' THEN 'P0-BACKFILL'
    ELSE `parser_version`
  END,
  `data_quality_status` = CASE
    WHEN `data_quality_status` = 'UNKNOWN' THEN 'PARTIAL'
    ELSE `data_quality_status`
  END
WHERE `deleted` = b'0';

-- ------------------------------------------------------------
-- 2) 地区代码（省级/市级）
-- ------------------------------------------------------------
UPDATE `notice` `n`
LEFT JOIN `base_area` `p`
  ON `p`.`level` = 1 AND (`p`.`short_name` = `n`.`province` OR `p`.`name` = `n`.`province`)
LEFT JOIN `base_area` `c`
  ON `c`.`level` = 2 AND `c`.`parent_code` = `p`.`code`
 AND (`c`.`short_name` = `n`.`city` OR `c`.`name` = `n`.`city`)
SET
  `n`.`province_code` = COALESCE(`p`.`code`, ''),
  `n`.`city_code` = COALESCE(
    `c`.`code`,
    (SELECT `a`.`code` FROM `base_area` `a`
     WHERE `a`.`level` = 1 AND (`a`.`short_name` = `n`.`city` OR `a`.`name` = `n`.`city`)
     LIMIT 1),
    ''
  )
WHERE `n`.`deleted` = b'0';

-- ------------------------------------------------------------
-- 3) 附件标记与缺失字段
-- ------------------------------------------------------------
UPDATE `notice` `n`
LEFT JOIN (
  SELECT `notice_id`, COUNT(*) AS `cnt`
  FROM `notice_attachment`
  WHERE `deleted` = b'0'
  GROUP BY `notice_id`
) `a` ON `a`.`notice_id` = `n`.`id`
SET `n`.`has_attachment` = IF(COALESCE(`a`.`cnt`, 0) > 0, b'1', b'0')
WHERE `n`.`deleted` = b'0';

UPDATE `notice`
SET `missing_fields` = JSON_ARRAY(
  'unified_deal_code',
  'trade_category_code',
  'procurement_method_code',
  'deposit_amount',
  'qualification_requirement',
  'file_get_start_time',
  'file_get_end_time'
)
WHERE `deleted` = b'0'
  AND (`missing_fields` IS NULL OR JSON_LENGTH(`missing_fields`) = 0);

-- ------------------------------------------------------------
-- 4) 生成 project（按 project_key 归并）
--    project_key = SHA256(项目编号或标题 | region_code | 项目编号)
-- ------------------------------------------------------------
INSERT INTO `project`
  (`project_key`, `unified_deal_code`, `project_no`, `project_name`,
   `trade_category_code`, `project_category_code`, `region_code`,
   `province_code`, `city_code`, `district_code`, `budget`, `funding_source_code`,
   `tenderer_enterprise_id`, `agency_enterprise_id`, `status`,
   `first_publish_time`, `last_publish_time`, `notice_count`, `lot_count`, `source_count`,
   `missing_fields`, `data_quality_status`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
  `t`.`project_key`,
  NULL,
  COALESCE(MAX(`t`.`project_no`), ''),
  COALESCE(MIN(`t`.`title`), ''),
  '', '',
  COALESCE(MAX(`t`.`region_code`), ''),
  COALESCE(MAX(`t`.`province_code`), ''),
  COALESCE(MAX(`t`.`city_code`), ''),
  COALESCE(MAX(`t`.`district_code`), ''),
  MAX(`t`.`budget`), '',
  NULL, NULL, 'ACTIVE',
  MIN(`t`.`publish_time`), MAX(`t`.`publish_time`),
  COUNT(*), 1, COUNT(DISTINCT `t`.`source_site_code`),
  JSON_ARRAY('unified_deal_code', 'trade_category_code', 'procurement_method_code'),
  'PARTIAL',
  'P0-BACKFILL', NOW(), 'P0-BACKFILL', NOW(), b'0', 0
FROM (
  SELECT
    SHA2(CONCAT(
      COALESCE(NULLIF(`n`.`project_no`, ''), `n`.`title`), '|',
      COALESCE(`n`.`region_code`, ''), '|',
      COALESCE(`n`.`project_no`, '')
    ), 256) AS `project_key`,
    `n`.`project_no`, `n`.`title`, `n`.`region_code`, `n`.`province_code`,
    `n`.`city_code`, `n`.`district_code`, `n`.`budget`, `n`.`publish_time`,
    `n`.`source_site_code`
  FROM `notice` `n`
  LEFT JOIN `project` `p`
    ON `p`.`project_key` = SHA2(CONCAT(
      COALESCE(NULLIF(`n`.`project_no`, ''), `n`.`title`), '|',
      COALESCE(`n`.`region_code`, ''), '|',
      COALESCE(`n`.`project_no`, '')
    ), 256)
  WHERE `n`.`deleted` = b'0' AND `n`.`project_id` IS NULL AND `p`.`id` IS NULL
) `t`
GROUP BY `t`.`project_key`;

-- ------------------------------------------------------------
-- 5) 回填 notice.project_id
-- ------------------------------------------------------------
UPDATE `notice` `n`
JOIN `project` `p`
  ON `p`.`project_key` = SHA2(CONCAT(
    COALESCE(NULLIF(`n`.`project_no`, ''), `n`.`title`), '|',
    COALESCE(`n`.`region_code`, ''), '|',
    COALESCE(`n`.`project_no`, '')
  ), 256)
SET `n`.`project_id` = `p`.`id`
WHERE `n`.`deleted` = b'0' AND `n`.`project_id` IS NULL;

-- ------------------------------------------------------------
-- 6) 生成 tender_project（一个 project 先建一个）
-- ------------------------------------------------------------
INSERT INTO `tender_project`
  (`project_id`, `tender_project_no`, `tender_project_name`, `procurement_method_code`,
   `budget`, `price_ceiling`, `funding_source_code`, `lot_count`,
   `contract_period_days`, `contract_period_text`, `qualification_summary`,
   `evaluation_method_code`, `tenderer_enterprise_id`, `agency_enterprise_id`,
   `region_code`, `publish_time`, `status`, `missing_fields`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
  `p`.`id`,
  NULLIF(MAX(`n`.`project_no`), ''),
  `p`.`project_name`,
  '',
  `p`.`budget`, NULL, '', 1,
  NULL, '', NULL,
  '', NULL, NULL,
  `p`.`region_code`, `p`.`first_publish_time`, 'ACTIVE',
  JSON_ARRAY('procurement_method_code', 'price_ceiling'),
  'P0-BACKFILL', NOW(), 'P0-BACKFILL', NOW(), b'0', 0
FROM `project` `p`
JOIN `notice` `n` ON `n`.`project_id` = `p`.`id` AND `n`.`deleted` = b'0'
WHERE `p`.`creator` = 'P0-BACKFILL'
  AND NOT EXISTS (
    SELECT 1 FROM `tender_project` `tp`
    WHERE `tp`.`project_id` = `p`.`id` AND `tp`.`creator` = 'P0-BACKFILL'
  )
GROUP BY `p`.`id`, `p`.`project_name`, `p`.`budget`, `p`.`region_code`, `p`.`first_publish_time`;

-- ------------------------------------------------------------
-- 7) 回填 notice.tender_project_id
-- ------------------------------------------------------------
UPDATE `notice` `n`
JOIN `tender_project` `tp`
  ON `tp`.`project_id` = `n`.`project_id` AND `tp`.`creator` = 'P0-BACKFILL'
SET `n`.`tender_project_id` = `tp`.`id`
WHERE `n`.`deleted` = b'0' AND `n`.`tender_project_id` IS NULL;

-- ------------------------------------------------------------
-- 8) 生成 tender_lot（无标段信息时 DEFAULT）
-- ------------------------------------------------------------
INSERT INTO `tender_lot`
  (`tender_project_id`, `project_id`, `lot_no`, `lot_name`, `lot_content`,
   `budget`, `price_ceiling`, `deposit_amount`, `qualification_requirement`,
   `specific_qualification`, `consortium_allowed`, `performance_requirement`,
   `qualification_level_code`, `evaluation_method_code`, `region_code`,
   `status`, `missing_fields`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
  `tp`.`id`, `tp`.`project_id`, 'DEFAULT', `tp`.`tender_project_name`, NULL,
  `tp`.`budget`, NULL, NULL, NULL,
  NULL, NULL, NULL,
  '', '', `tp`.`region_code`,
  'ACTIVE', JSON_ARRAY('lot_no', 'deposit_amount'),
  'P0-BACKFILL', NOW(), 'P0-BACKFILL', NOW(), b'0', 0
FROM `tender_project` `tp`
WHERE `tp`.`creator` = 'P0-BACKFILL'
  AND NOT EXISTS (
    SELECT 1 FROM `tender_lot` `l`
    WHERE `l`.`tender_project_id` = `tp`.`id` AND `l`.`creator` = 'P0-BACKFILL'
  );

-- ------------------------------------------------------------
-- 9) 回填 notice.lot_id / lot_no / tender_project_no
-- ------------------------------------------------------------
UPDATE `notice` `n`
JOIN `tender_project` `tp`
  ON `tp`.`id` = `n`.`tender_project_id` AND `tp`.`creator` = 'P0-BACKFILL'
JOIN `tender_lot` `l`
  ON `l`.`tender_project_id` = `tp`.`id` AND `l`.`creator` = 'P0-BACKFILL'
SET
  `n`.`lot_id` = `l`.`id`,
  `n`.`lot_no` = COALESCE(NULLIF(`n`.`lot_no`, ''), `l`.`lot_no`),
  `n`.`tender_project_no` = COALESCE(NULLIF(`n`.`tender_project_no`, ''), COALESCE(`tp`.`tender_project_no`, ''))
WHERE `n`.`deleted` = b'0' AND `n`.`lot_id` IS NULL;

-- ------------------------------------------------------------
-- 10) 建立 notice_enterprise（TENDERER / AGENCY）
--     归一化表达式必须与 P0-2__enterprise_backfill.sql 保持一致。
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS `tmp_notice_enterprise_link`;
CREATE TEMPORARY TABLE `tmp_notice_enterprise_link` (
  `notice_id`   bigint       NOT NULL,
  `role_code`   varchar(32)  NOT NULL,
  `source_text` varchar(512) NOT NULL,
  `name_hash`   varchar(64)  NOT NULL,
  KEY `idx_notice` (`notice_id`),
  KEY `idx_hash` (`name_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tmp_notice_enterprise_link` (`notice_id`, `role_code`, `source_text`, `name_hash`)
SELECT
  `id`, 'TENDERER', TRIM(`tender_person`),
  SHA2(LEFT(TRIM(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
      TRIM(`tender_person`),
      '　', ''), ' ', ''), '（', '('), '）', ')'), CHAR(10), ''), CHAR(13), ''), CHAR(9), '')
  ), 256), 256)
FROM `notice`
WHERE `deleted` = b'0' AND TRIM(`tender_person`) <> ''

UNION ALL

SELECT
  `id`, 'AGENCY', TRIM(`agency`),
  SHA2(LEFT(TRIM(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
      TRIM(`agency`),
      '　', ''), ' ', ''), '（', '('), '）', ')'), CHAR(10), ''), CHAR(13), ''), CHAR(9), '')
  ), 256), 256)
FROM `notice`
WHERE `deleted` = b'0' AND TRIM(`agency`) <> '';

INSERT INTO `notice_enterprise`
  (`notice_id`, `enterprise_id`, `role_code`, `source_text`, `confidence`, `status`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
  `t`.`notice_id`, `e`.`id`, `t`.`role_code`, `t`.`source_text`, 1.00, 'AUTO',
  'P0-BACKFILL', NOW(), 'P0-BACKFILL', NOW(), b'0', 0
FROM `tmp_notice_enterprise_link` `t`
JOIN `enterprise` `e` ON `e`.`name_hash` = `t`.`name_hash` AND `e`.`deleted` = b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `notice_enterprise` `ne`
  WHERE `ne`.`notice_id` = `t`.`notice_id`
    AND `ne`.`enterprise_id` = `e`.`id`
    AND `ne`.`role_code` = `t`.`role_code`
    AND `ne`.`deleted` = b'0'
);

DROP TEMPORARY TABLE IF EXISTS `tmp_notice_enterprise_link`;

-- ------------------------------------------------------------
-- 11) 回填 notice / project / tender_project 的企业 ID
-- ------------------------------------------------------------
UPDATE `notice` `n`
LEFT JOIN (
  SELECT `notice_id`, MIN(`enterprise_id`) AS `eid`
  FROM `notice_enterprise`
  WHERE `role_code` = 'TENDERER' AND `deleted` = b'0'
  GROUP BY `notice_id`
) `t` ON `t`.`notice_id` = `n`.`id`
LEFT JOIN (
  SELECT `notice_id`, MIN(`enterprise_id`) AS `eid`
  FROM `notice_enterprise`
  WHERE `role_code` = 'AGENCY' AND `deleted` = b'0'
  GROUP BY `notice_id`
) `a` ON `a`.`notice_id` = `n`.`id`
SET
  `n`.`tenderer_enterprise_id` = `t`.`eid`,
  `n`.`agency_enterprise_id` = `a`.`eid`
WHERE `n`.`deleted` = b'0';

UPDATE `project` `p`
LEFT JOIN (
  SELECT `project_id`, MIN(`tenderer_enterprise_id`) AS `eid`
  FROM `notice`
  WHERE `project_id` IS NOT NULL AND `tenderer_enterprise_id` IS NOT NULL AND `deleted` = b'0'
  GROUP BY `project_id`
) `t` ON `t`.`project_id` = `p`.`id`
LEFT JOIN (
  SELECT `project_id`, MIN(`agency_enterprise_id`) AS `eid`
  FROM `notice`
  WHERE `project_id` IS NOT NULL AND `agency_enterprise_id` IS NOT NULL AND `deleted` = b'0'
  GROUP BY `project_id`
) `a` ON `a`.`project_id` = `p`.`id`
SET
  `p`.`tenderer_enterprise_id` = `t`.`eid`,
  `p`.`agency_enterprise_id` = `a`.`eid`
WHERE `p`.`deleted` = b'0';

UPDATE `tender_project` `tp`
JOIN `project` `p` ON `p`.`id` = `tp`.`project_id`
SET
  `tp`.`tenderer_enterprise_id` = `p`.`tenderer_enterprise_id`,
  `tp`.`agency_enterprise_id` = `p`.`agency_enterprise_id`
WHERE `tp`.`deleted` = b'0';

-- ------------------------------------------------------------
-- 12) 冗余统计回收
-- ------------------------------------------------------------
UPDATE `project` `p`
JOIN (
  SELECT
    `project_id`,
    COUNT(*) AS `cnt`,
    COUNT(DISTINCT `source_site_code`) AS `src`,
    MIN(`publish_time`) AS `first_t`,
    MAX(`publish_time`) AS `last_t`
  FROM `notice`
  WHERE `project_id` IS NOT NULL AND `deleted` = b'0'
  GROUP BY `project_id`
) `x` ON `x`.`project_id` = `p`.`id`
SET
  `p`.`notice_count` = `x`.`cnt`,
  `p`.`source_count` = `x`.`src`,
  `p`.`first_publish_time` = `x`.`first_t`,
  `p`.`last_publish_time` = `x`.`last_t`
WHERE `p`.`deleted` = b'0';

UPDATE `project` `p`
JOIN (
  SELECT `project_id`, COUNT(*) AS `cnt`
  FROM `tender_lot`
  WHERE `deleted` = b'0'
  GROUP BY `project_id`
) `l` ON `l`.`project_id` = `p`.`id`
SET `p`.`lot_count` = `l`.`cnt`
WHERE `p`.`deleted` = b'0';

UPDATE `tender_project` `tp`
JOIN (
  SELECT `tender_project_id`, COUNT(*) AS `cnt`
  FROM `tender_lot`
  WHERE `deleted` = b'0'
  GROUP BY `tender_project_id`
) `l` ON `l`.`tender_project_id` = `tp`.`id`
SET `tp`.`lot_count` = `l`.`cnt`
WHERE `tp`.`deleted` = b'0';

-- ------------------------------------------------------------
-- 13) 结果概览（可注释）
-- ------------------------------------------------------------
SELECT 'notice_total' AS `metric`, COUNT(*) AS `cnt` FROM `notice` WHERE `deleted` = b'0'
UNION ALL SELECT 'notice_with_project', COUNT(*) FROM `notice` WHERE `deleted` = b'0' AND `project_id` IS NOT NULL
UNION ALL SELECT 'notice_with_tender_project', COUNT(*) FROM `notice` WHERE `deleted` = b'0' AND `tender_project_id` IS NOT NULL
UNION ALL SELECT 'notice_with_lot', COUNT(*) FROM `notice` WHERE `deleted` = b'0' AND `lot_id` IS NOT NULL
UNION ALL SELECT 'project_total', COUNT(*) FROM `project` WHERE `deleted` = b'0'
UNION ALL SELECT 'tender_project_total', COUNT(*) FROM `tender_project` WHERE `deleted` = b'0'
UNION ALL SELECT 'tender_lot_total', COUNT(*) FROM `tender_lot` WHERE `deleted` = b'0'
UNION ALL SELECT 'notice_enterprise_total', COUNT(*) FROM `notice_enterprise` WHERE `deleted` = b'0';
