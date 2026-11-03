-- ============================================================
-- P0-2 回滚：删除企业主体域表
-- 警告：会删除 enterprise / enterprise_role / enterprise_contact / enterprise_alias
--       及其全部数据（含手工维护数据）。执行前务必备份。
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `enterprise_alias`;
DROP TABLE IF EXISTS `enterprise_contact`;
DROP TABLE IF EXISTS `enterprise_role`;
DROP TABLE IF EXISTS `enterprise`;

SET FOREIGN_KEY_CHECKS = 1;
