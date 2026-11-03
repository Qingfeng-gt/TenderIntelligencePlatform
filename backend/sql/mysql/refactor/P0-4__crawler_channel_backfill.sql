-- ============================================================
-- P0-4 采集配置回填
-- 目标库: tender
-- 前置: P0-4__crawl_governance.sql
-- 作用:
--   1) 给 crawler_site 补 tier / priority / region_scope / health_status / adapter_version
--   2) 把 crawler_site.channels JSON 展开为 crawler_channel 行
-- 幂等: 是。频道按唯一键 (site_code, path, notice_type_code) upsert。
-- 注意: 只处理 JSON_VALID 的 channels；无效 JSON 会在校验脚本中列出。
-- ============================================================

SET NAMES utf8mb4;

-- 1) 站点分层与默认配置
UPDATE `crawler_site`
SET
  `tier` = CASE `code`
    WHEN 'ccgp' THEN 'official'
    WHEN 'ggzy' THEN 'official'
    WHEN 'ztb_gz' THEN 'official'
    ELSE 'industry'
  END,
  `priority` = CASE `code`
    WHEN 'ccgp' THEN 10
    WHEN 'ggzy' THEN 20
    WHEN 'ztb_gz' THEN 30
    ELSE 100
  END,
  `region_scope` = CASE `code`
    WHEN 'ccgp' THEN '["CN"]'
    WHEN 'ggzy' THEN '["CN"]'
    WHEN 'ztb_gz' THEN '["520000"]'
    ELSE '[]'
  END,
  `health_status` = CASE WHEN `enabled` = b'1' THEN 'HEALTHY' ELSE 'UNKNOWN' END,
  `adapter_version` = CASE WHEN `adapter_version` = '' THEN 'v1' ELSE `adapter_version` END
WHERE `deleted` = b'0';

-- 2) channels JSON → crawler_channel
INSERT INTO `crawler_channel`
  (`site_code`, `path`, `name`, `notice_type_code`, `page_count`, `enabled`, `sort`, `config`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
  `s`.`code`,
  `jt`.`channel_path`,
  COALESCE(`jt`.`channel_name`, ''),
  COALESCE(`jt`.`notice_type_code`, ''),
  COALESCE(`jt`.`page_count`, 1),
  `s`.`enabled`,
  `jt`.`seq`,
  CASE WHEN `jt`.`template` IS NULL THEN NULL ELSE JSON_OBJECT('template', `jt`.`template`) END,
  'P0-BACKFILL', NOW(), 'P0-BACKFILL', NOW(), b'0', 0
FROM (
  SELECT *
  FROM `crawler_site`
  WHERE `deleted` = b'0'
    AND `channels` IS NOT NULL
    AND `channels` <> ''
    AND JSON_VALID(`channels`)
) `s`
JOIN JSON_TABLE(
  CAST(`s`.`channels` AS JSON),
  '$[*]' COLUMNS (
    `seq` FOR ORDINALITY,
    `channel_path` VARCHAR(255) PATH '$.path',
    `channel_name` VARCHAR(128) PATH '$.name',
    `notice_type_code` VARCHAR(32) PATH '$.type',
    `page_count` INT PATH '$.pageCount',
    `template` VARCHAR(32) PATH '$.template'
  )
) `jt`
WHERE `jt`.`channel_path` IS NOT NULL
  AND `jt`.`channel_path` <> ''
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `page_count` = VALUES(`page_count`),
  `enabled` = VALUES(`enabled`),
  `sort` = VALUES(`sort`),
  `config` = VALUES(`config`),
  `updater` = 'P0-BACKFILL',
  `update_time` = NOW();

-- 结果概览（可注释）
SELECT
  `s`.`code`,
  CASE WHEN `s`.`channels` IS NULL OR `s`.`channels` = '' OR JSON_VALID(`s`.`channels`) = 0
       THEN 0 ELSE JSON_LENGTH(`s`.`channels`) END AS `json_channels`,
  (SELECT COUNT(*) FROM `crawler_channel` `c`
   WHERE `c`.`site_code` = `s`.`code` AND `c`.`deleted` = b'0') AS `db_channels`
FROM `crawler_site` `s`
WHERE `s`.`deleted` = b'0'
ORDER BY `s`.`id`;
