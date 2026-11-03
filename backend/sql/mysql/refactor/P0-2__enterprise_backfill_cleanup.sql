-- ============================================================
-- P0-2 回填数据清理（保留表结构）
-- 用途：仅撤销 P0-2__enterprise_backfill.sql 写入的数据。
-- 警告：会删除 source_type = 'NOTICE_BACKFILL' 的企业及其角色。
--       若这些企业后来被人工补全、合并或关联，请先人工确认再执行。
-- ============================================================

SET NAMES utf8mb4;

DELETE `r`
FROM `enterprise_role` `r`
JOIN `enterprise` `e` ON `e`.`id` = `r`.`enterprise_id`
WHERE `e`.`source_type` = 'NOTICE_BACKFILL';

DELETE FROM `enterprise` WHERE `source_type` = 'NOTICE_BACKFILL';
