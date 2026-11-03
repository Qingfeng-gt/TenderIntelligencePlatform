-- ============================================================
-- P0-3 回填校验（只读）
-- 用途：执行 P0-3__notice_backfill.sql 后人工核对，不修改数据。
-- ============================================================

SET NAMES utf8mb4;

-- 1) 总量
SELECT 'notice_total' AS `metric`, COUNT(*) AS `cnt` FROM `notice` WHERE `deleted` = b'0'
UNION ALL SELECT 'project_total', COUNT(*) FROM `project` WHERE `deleted` = b'0'
UNION ALL SELECT 'tender_project_total', COUNT(*) FROM `tender_project` WHERE `deleted` = b'0'
UNION ALL SELECT 'tender_lot_total', COUNT(*) FROM `tender_lot` WHERE `deleted` = b'0'
UNION ALL SELECT 'notice_enterprise_total', COUNT(*) FROM `notice_enterprise` WHERE `deleted` = b'0';

-- 2) 未归一的公告（正常应为 0）
SELECT
  SUM(`project_id` IS NULL) AS `no_project`,
  SUM(`tender_project_id` IS NULL) AS `no_tender_project`,
  SUM(`lot_id` IS NULL) AS `no_lot`
FROM `notice`
WHERE `deleted` = b'0';

-- 3) 空项目 / 空招标项目 / 空标段（正常应为 0）
SELECT 'project_without_notice' AS `issue`, COUNT(*) AS `cnt`
FROM `project` `p`
WHERE `p`.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `notice` `n` WHERE `n`.`project_id` = `p`.`id` AND `n`.`deleted` = b'0')
UNION ALL
SELECT 'tender_project_without_lot', COUNT(*)
FROM `tender_project` `tp`
WHERE `tp`.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `tender_lot` `l` WHERE `l`.`tender_project_id` = `tp`.`id` AND `l`.`deleted` = b'0')
UNION ALL
SELECT 'lot_without_notice', COUNT(*)
FROM `tender_lot` `l`
WHERE `l`.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `notice` `n` WHERE `n`.`lot_id` = `l`.`id` AND `n`.`deleted` = b'0');

-- 4) project_key 重复（正常应为 0 行；唯一键本身也会阻止）
SELECT `project_key`, COUNT(*) AS `cnt`
FROM `project`
WHERE `deleted` = b'0'
GROUP BY `project_key`
HAVING `cnt` > 1;

-- 5) 企业关联角色分布
SELECT `role_code`, COUNT(*) AS `cnt`
FROM `notice_enterprise`
WHERE `deleted` = b'0'
GROUP BY `role_code`;

-- 6) 地区代码未匹配的公告（抽样，便于补 base_area）
SELECT `province`, `city`, COUNT(*) AS `cnt`
FROM `notice`
WHERE `deleted` = b'0' AND (`province_code` = '' OR `city_code` = '')
GROUP BY `province`, `city`
ORDER BY `cnt` DESC;

-- 7) 公告 → 项目 → 招标项目 → 标段 抽样
SELECT
  `n`.`id` AS `notice_id`,
  `n`.`title`,
  `n`.`notice_type_code`,
  `p`.`id` AS `project_id`,
  `p`.`project_name`,
  `tp`.`id` AS `tender_project_id`,
  `l`.`id` AS `lot_id`,
  `l`.`lot_no`
FROM `notice` `n`
LEFT JOIN `project` `p` ON `p`.`id` = `n`.`project_id`
LEFT JOIN `tender_project` `tp` ON `tp`.`id` = `n`.`tender_project_id`
LEFT JOIN `tender_lot` `l` ON `l`.`id` = `n`.`lot_id`
WHERE `n`.`deleted` = b'0'
ORDER BY `n`.`id`
LIMIT 50;

-- 8) 公告原文有企业名但未建立关联（抽样）
SELECT
  `n`.`id` AS `notice_id`,
  `n`.`tender_person`,
  `n`.`agency`
FROM `notice` `n`
WHERE `n`.`deleted` = b'0'
  AND (
    (`n`.`tender_person` <> '' AND `n`.`tenderer_enterprise_id` IS NULL)
    OR (`n`.`agency` <> '' AND `n`.`agency_enterprise_id` IS NULL)
  )
ORDER BY `n`.`id`
LIMIT 50;
