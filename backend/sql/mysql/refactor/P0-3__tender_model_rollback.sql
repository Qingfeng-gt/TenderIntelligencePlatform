-- ============================================================
-- P0-3 回滚：删除四层标讯域新增表
-- 警告：
--   1) 会删除 project / tender_project / tender_lot / notice_change / notice_enterprise 全部数据。
--   2) 默认**不删除** notice / notice_attachment 上新增的列，避免误删已录入数据。
--   3) 如需彻底回滚列，请手动执行文件末尾注释掉的 ALTER 段。
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `notice_enterprise`;
DROP TABLE IF EXISTS `notice_change`;
DROP TABLE IF EXISTS `tender_lot`;
DROP TABLE IF EXISTS `tender_project`;
DROP TABLE IF EXISTS `project`;

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- 彻底回滚 notice 新增列（默认注释，确认后再执行）
-- ------------------------------------------------------------
-- ALTER TABLE `notice`
--   DROP COLUMN `project_id`,
--   DROP COLUMN `tender_project_id`,
--   DROP COLUMN `lot_id`,
--   DROP COLUMN `unified_deal_code`,
--   DROP COLUMN `tender_project_no`,
--   DROP COLUMN `lot_no`,
--   DROP COLUMN `notice_no`,
--   DROP COLUMN `notice_type_code`,
--   DROP COLUMN `notice_subtype_code`,
--   DROP COLUMN `source_site_code`,
--   DROP COLUMN `source_platform`,
--   DROP COLUMN `file_get_start_time`,
--   DROP COLUMN `file_get_end_time`,
--   DROP COLUMN `qa_deadline`,
--   DROP COLUMN `sign_deadline`,
--   DROP COLUMN `bid_deadline`,
--   DROP COLUMN `open_place`,
--   DROP COLUMN `publicity_start_time`,
--   DROP COLUMN `publicity_end_time`,
--   DROP COLUMN `price_ceiling`,
--   DROP COLUMN `deposit_amount`,
--   DROP COLUMN `file_price`,
--   DROP COLUMN `win_amount`,
--   DROP COLUMN `procurement_method_code`,
--   DROP COLUMN `funding_source_code`,
--   DROP COLUMN `evaluation_method_code`,
--   DROP COLUMN `qualification_requirement`,
--   DROP COLUMN `specific_qualification`,
--   DROP COLUMN `consortium_allowed`,
--   DROP COLUMN `performance_requirement`,
--   DROP COLUMN `qualification_level_code`,
--   DROP COLUMN `trade_category_code`,
--   DROP COLUMN `province_code`,
--   DROP COLUMN `city_code`,
--   DROP COLUMN `district_code`,
--   DROP COLUMN `tenderer_enterprise_id`,
--   DROP COLUMN `agency_enterprise_id`,
--   DROP COLUMN `winner_enterprise_id`,
--   DROP COLUMN `industry_code`,
--   DROP COLUMN `has_attachment`,
--   DROP COLUMN `view_count`,
--   DROP COLUMN `status`,
--   DROP COLUMN `missing_fields`,
--   DROP COLUMN `data_quality_status`,
--   DROP COLUMN `raw_id`,
--   DROP COLUMN `parser_version`;

-- ALTER TABLE `notice_attachment`
--   DROP COLUMN `file_category_code`,
--   DROP COLUMN `file_price`,
--   DROP COLUMN `acquisition_method_code`,
--   DROP COLUMN `source_attachment_id`,
--   DROP COLUMN `file_hash`,
--   DROP COLUMN `local_file_id`;
