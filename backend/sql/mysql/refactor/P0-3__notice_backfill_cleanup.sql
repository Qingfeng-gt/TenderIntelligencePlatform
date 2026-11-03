-- ============================================================
-- P0-3 回填数据清理（保留表结构）
-- 用途：仅撤销 P0-3__notice_backfill.sql 写入的数据。
-- 警告：
--   1) 会删除 creator='P0-BACKFILL' 的 project / tender_project / tender_lot /
--      notice_enterprise 数据。
--   2) 会把 parser_version='P0-BACKFILL' 的 notice 新列重置为空。
--   3) 若这些数据后来被人工补全或关联，请先人工确认再执行。
-- ============================================================

SET NAMES utf8mb4;

DELETE FROM `notice_enterprise` WHERE `creator` = 'P0-BACKFILL';

DELETE `l`
FROM `tender_lot` `l`
JOIN `tender_project` `tp` ON `tp`.`id` = `l`.`tender_project_id`
WHERE `tp`.`creator` = 'P0-BACKFILL' OR `l`.`creator` = 'P0-BACKFILL';

DELETE FROM `tender_project` WHERE `creator` = 'P0-BACKFILL';
DELETE FROM `project` WHERE `creator` = 'P0-BACKFILL';

UPDATE `notice`
SET
  `project_id` = NULL,
  `tender_project_id` = NULL,
  `lot_id` = NULL,
  `unified_deal_code` = NULL,
  `tender_project_no` = '',
  `lot_no` = '',
  `notice_no` = '',
  `notice_type_code` = '',
  `notice_subtype_code` = '',
  `source_site_code` = '',
  `source_platform` = '',
  `file_get_start_time` = NULL,
  `file_get_end_time` = NULL,
  `qa_deadline` = NULL,
  `sign_deadline` = NULL,
  `bid_deadline` = NULL,
  `open_place` = '',
  `publicity_start_time` = NULL,
  `publicity_end_time` = NULL,
  `price_ceiling` = NULL,
  `deposit_amount` = NULL,
  `file_price` = NULL,
  `win_amount` = NULL,
  `procurement_method_code` = '',
  `funding_source_code` = '',
  `evaluation_method_code` = '',
  `qualification_requirement` = NULL,
  `specific_qualification` = NULL,
  `consortium_allowed` = NULL,
  `performance_requirement` = NULL,
  `qualification_level_code` = '',
  `trade_category_code` = '',
  `province_code` = '',
  `city_code` = '',
  `district_code` = '',
  `tenderer_enterprise_id` = NULL,
  `agency_enterprise_id` = NULL,
  `winner_enterprise_id` = NULL,
  `industry_code` = '',
  `has_attachment` = b'0',
  `view_count` = 0,
  `status` = 'ACTIVE',
  `missing_fields` = NULL,
  `data_quality_status` = 'UNKNOWN',
  `raw_id` = NULL,
  `parser_version` = ''
WHERE `parser_version` = 'P0-BACKFILL';
