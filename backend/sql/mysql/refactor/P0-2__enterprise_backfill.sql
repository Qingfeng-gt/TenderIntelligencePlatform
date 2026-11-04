-- ============================================================
-- P0-2 企业主体回填
-- 目标库: tender
-- 前置: P0-2__enterprise.sql
-- 作用: 从现有 notice.tender_person / notice.agency 抽取企业名，
--       写入 enterprise / enterprise_role。
-- 幂等: 是。按 name_hash 去重；角色按 (enterprise_id, role_code) upsert。
-- 边界: 只回填企业主档与角色；notice_enterprise 关联、联系人抽取在 P0-3。
-- 清理: P0-2__enterprise_backfill_cleanup.sql（仅回滚回填数据时使用）
-- ⚠️ 本脚本读取旧列（notice.tender_person / notice.agency），
--    只能在 P0-6__drop_legacy_columns.sql 之前执行。
-- ============================================================

SET NAMES utf8mb4;

-- 临时聚合表：同一归一化名称 + 角色只保留一条，记录首末公告 ID
DROP TEMPORARY TABLE IF EXISTS `tmp_enterprise_names`;
CREATE TEMPORARY TABLE `tmp_enterprise_names` (
  `raw_name`        varchar(256) NOT NULL,
  `normalized_name` varchar(256) NOT NULL,
  `name_hash`       varchar(64)  NOT NULL,
  `role_code`       varchar(32)  NOT NULL,
  `first_notice_id` bigint       NOT NULL,
  `last_notice_id`  bigint       NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tmp_enterprise_names`
  (`raw_name`, `normalized_name`, `name_hash`, `role_code`, `first_notice_id`, `last_notice_id`)
SELECT
  MIN(`raw_name`),
  `normalized_name`,
  SHA2(`normalized_name`, 256),
  `role_code`,
  MIN(`notice_id`),
  MAX(`notice_id`)
FROM (
  SELECT
    TRIM(`tender_person`) AS `raw_name`,
    LEFT(
      TRIM(
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
          TRIM(`tender_person`),
          '　', ''), ' ', ''), '（', '('), '）', ')'), CHAR(10), ''), CHAR(13), ''), CHAR(9), '')
      ),
      256
    ) AS `normalized_name`,
    'TENDERER' AS `role_code`,
    `id` AS `notice_id`
  FROM `notice`
  WHERE `deleted` = b'0' AND TRIM(`tender_person`) <> ''

  UNION ALL

  SELECT
    TRIM(`agency`) AS `raw_name`,
    LEFT(
      TRIM(
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
          TRIM(`agency`),
          '　', ''), ' ', ''), '（', '('), '）', ')'), CHAR(10), ''), CHAR(13), ''), CHAR(9), '')
      ),
      256
    ) AS `normalized_name`,
    'AGENCY' AS `role_code`,
    `id` AS `notice_id`
  FROM `notice`
  WHERE `deleted` = b'0' AND TRIM(`agency`) <> ''
) `x`
GROUP BY `normalized_name`, `role_code`;

-- 1) 企业主档：按 name_hash 去重，已存在的不动
INSERT INTO `enterprise`
  (`name`, `normalized_name`, `name_hash`, `source_type`, `status`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
  LEFT(`t`.`name`, 256),
  `t`.`normalized_name`,
  `t`.`name_hash`,
  'NOTICE_BACKFILL',
  'ACTIVE',
  'system', NOW(), 'system', NOW(), b'0', 0
FROM (
  SELECT MIN(`raw_name`) AS `name`, `normalized_name`, `name_hash`
  FROM `tmp_enterprise_names`
  GROUP BY `normalized_name`, `name_hash`
) `t`
LEFT JOIN `enterprise` `e` ON `e`.`name_hash` = `t`.`name_hash`
WHERE `e`.`id` IS NULL;

-- 2) 企业角色：同一企业同一角色只保留一行，首末公告取并集
INSERT INTO `enterprise_role`
  (`enterprise_id`, `role_code`, `first_seen_notice_id`, `last_seen_notice_id`, `status`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
  `e`.`id`,
  `t`.`role_code`,
  MIN(`t`.`first_notice_id`),
  MAX(`t`.`last_notice_id`),
  'ACTIVE',
  'system', NOW(), 'system', NOW(), b'0', 0
FROM `tmp_enterprise_names` `t`
JOIN `enterprise` `e` ON `e`.`name_hash` = `t`.`name_hash`
GROUP BY `e`.`id`, `t`.`role_code`
ON DUPLICATE KEY UPDATE
  `first_seen_notice_id` = IF(`first_seen_notice_id` IS NULL,
                              VALUES(`first_seen_notice_id`),
                              LEAST(`first_seen_notice_id`, VALUES(`first_seen_notice_id`))),
  `last_seen_notice_id`  = IF(`last_seen_notice_id` IS NULL,
                              VALUES(`last_seen_notice_id`),
                              GREATEST(`last_seen_notice_id`, VALUES(`last_seen_notice_id`))),
  `updater` = 'system',
  `update_time` = NOW();

DROP TEMPORARY TABLE IF EXISTS `tmp_enterprise_names`;

-- 结果概览（可注释掉）
SELECT 'enterprise_backfill' AS `metric`, COUNT(*) AS `cnt`
FROM `enterprise` WHERE `source_type` = 'NOTICE_BACKFILL' AND `deleted` = b'0'
UNION ALL
SELECT 'enterprise_role_backfill', COUNT(*)
FROM `enterprise_role` `r`
JOIN `enterprise` `e` ON `e`.`id` = `r`.`enterprise_id`
WHERE `e`.`source_type` = 'NOTICE_BACKFILL' AND `r`.`deleted` = b'0';
