-- ============================================================
-- P0-4 回滚：删除采集治理域新增表
-- 警告：
--   1) 会删除 crawler_channel / crawler_fetch_log / crawl_raw /
--      crawl_parse_rule / crawl_merge_log / notice_quality 全部数据。
--   2) 默认**不删除** crawler_site / crawler_task 上新增的列。
--   3) 如需彻底回滚列，请手动执行文件末尾注释掉的 ALTER 段。
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `notice_quality`;
DROP TABLE IF EXISTS `crawl_merge_log`;
DROP TABLE IF EXISTS `crawl_parse_rule`;
DROP TABLE IF EXISTS `crawl_raw`;
DROP TABLE IF EXISTS `crawler_fetch_log`;
DROP TABLE IF EXISTS `crawler_channel`;

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- 彻底回滚 crawler_site / crawler_task 新增列（默认注释）
-- ------------------------------------------------------------
-- ALTER TABLE `crawler_site`
--   DROP COLUMN `tier`,
--   DROP COLUMN `schedule_cron`,
--   DROP COLUMN `priority`,
--   DROP COLUMN `region_scope`,
--   DROP COLUMN `last_success_time`,
--   DROP COLUMN `last_fail_time`,
--   DROP COLUMN `health_status`,
--   DROP COLUMN `adapter_version`;

-- ALTER TABLE `crawler_task`
--   DROP COLUMN `trigger_type`,
--   DROP COLUMN `duration_ms`,
--   DROP COLUMN `config_snapshot`,
--   DROP COLUMN `error_type`,
--   DROP COLUMN `data_date`;
