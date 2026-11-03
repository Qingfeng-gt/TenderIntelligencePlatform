-- ============================================================
-- P0-4 回填校验（只读）
-- 用途：执行 P0-4__crawler_channel_backfill.sql 后人工核对，不修改数据。
-- ============================================================

SET NAMES utf8mb4;

-- 1) 站点 JSON 频道数 vs 落表频道数
SELECT
  `s`.`id`,
  `s`.`code`,
  `s`.`name`,
  `s`.`tier`,
  `s`.`priority`,
  `s`.`health_status`,
  CASE WHEN `s`.`channels` IS NULL OR `s`.`channels` = '' OR JSON_VALID(`s`.`channels`) = 0
       THEN 0 ELSE JSON_LENGTH(`s`.`channels`) END AS `json_channels`,
  (SELECT COUNT(*) FROM `crawler_channel` `c`
   WHERE `c`.`site_code` = `s`.`code` AND `c`.`deleted` = b'0') AS `db_channels`
FROM `crawler_site` `s`
WHERE `s`.`deleted` = b'0'
ORDER BY `s`.`id`;

-- 2) channels 不是合法 JSON 的站点（正常应为 0 行）
SELECT `id`, `code`, LEFT(`channels`, 200) AS `channels_preview`
FROM `crawler_site`
WHERE `deleted` = b'0'
  AND `channels` IS NOT NULL
  AND `channels` <> ''
  AND JSON_VALID(`channels`) = 0;

-- 3) 频道按站点分布
SELECT `site_code`, COUNT(*) AS `channel_count`
FROM `crawler_channel`
WHERE `deleted` = b'0'
GROUP BY `site_code`
ORDER BY `site_code`;

-- 4) 频道类型分布
SELECT `notice_type_code`, COUNT(*) AS `cnt`
FROM `crawler_channel`
WHERE `deleted` = b'0'
GROUP BY `notice_type_code`
ORDER BY `cnt` DESC;

-- 5) 日志与质量表当前数据量（初始化后应为 0）
SELECT 'crawler_fetch_log' AS `table_name`, COUNT(*) AS `cnt` FROM `crawler_fetch_log`
UNION ALL SELECT 'crawl_raw', COUNT(*) FROM `crawl_raw`
UNION ALL SELECT 'crawl_parse_rule', COUNT(*) FROM `crawl_parse_rule`
UNION ALL SELECT 'crawl_merge_log', COUNT(*) FROM `crawl_merge_log`
UNION ALL SELECT 'notice_quality', COUNT(*) FROM `notice_quality`;
