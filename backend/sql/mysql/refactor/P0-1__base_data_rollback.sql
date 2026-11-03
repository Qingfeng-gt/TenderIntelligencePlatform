-- ============================================================
-- P0-1 回滚：删除基础数据域表
-- 警告：会连同 base_area / base_code 的种子数据一起删除。
--       若已有业务表引用这些代码，请先确认没有外键或应用层引用。
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `base_field_mapping`;
DROP TABLE IF EXISTS `base_code`;
DROP TABLE IF EXISTS `base_area`;

SET FOREIGN_KEY_CHECKS = 1;
