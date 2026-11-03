-- ============================================================
-- P0-4 回填数据清理（保留表结构）
-- 用途：仅撤销 P0-4__crawler_channel_backfill.sql 写入的数据。
-- 注意：不删除爬虫运行期产生的 crawler_fetch_log / crawl_raw / notice_quality。
-- ============================================================

SET NAMES utf8mb4;

DELETE FROM `crawler_channel` WHERE `creator` = 'P0-BACKFILL';

UPDATE `crawler_site`
SET
  `tier` = 'official',
  `schedule_cron` = NULL,
  `priority` = 100,
  `region_scope` = '',
  `last_success_time` = NULL,
  `last_fail_time` = NULL,
  `health_status` = 'UNKNOWN',
  `adapter_version` = ''
WHERE `deleted` = b'0';
