-- ============================================================
-- P0-2 回填校验（只读）
-- 用途：执行回填后人工核对，不修改数据。
-- ============================================================

SET NAMES utf8mb4;

-- 1) 同名归一化结果是否重复（正常应为 0 行）
SELECT `name_hash`, COUNT(*) AS `cnt`, GROUP_CONCAT(`name` SEPARATOR ' | ') AS `names`
FROM `enterprise`
WHERE `deleted` = b'0'
GROUP BY `name_hash`
HAVING `cnt` > 1;

-- 2) 同一信用代码是否对应多个企业（正常应为 0 行）
SELECT `unified_social_credit_code`, COUNT(*) AS `cnt`, GROUP_CONCAT(`name` SEPARATOR ' | ') AS `names`
FROM `enterprise`
WHERE `deleted` = b'0' AND `unified_social_credit_code` IS NOT NULL AND `unified_social_credit_code` <> ''
GROUP BY `unified_social_credit_code`
HAVING `cnt` > 1;

-- 3) 回填企业数量与角色分布
SELECT `source_type`, COUNT(*) AS `enterprise_count`
FROM `enterprise`
WHERE `deleted` = b'0'
GROUP BY `source_type`;

SELECT `r`.`role_code`, COUNT(*) AS `role_count`
FROM `enterprise_role` `r`
WHERE `r`.`deleted` = b'0'
GROUP BY `r`.`role_code`;

-- 4) 公告原文里仍有、但未回填进 enterprise 的名称（抽样）
SELECT `t`.`raw_name`, COUNT(*) AS `notice_count`
FROM (
  SELECT TRIM(`tender_person`) AS `raw_name` FROM `notice` WHERE `deleted` = b'0' AND TRIM(`tender_person`) <> ''
  UNION ALL
  SELECT TRIM(`agency`) AS `raw_name` FROM `notice` WHERE `deleted` = b'0' AND TRIM(`agency`) <> ''
) `t`
LEFT JOIN `enterprise` `e` ON `e`.`normalized_name` = LEFT(
  TRIM(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
      `t`.`raw_name`, '　', ''), ' ', ''), '（', '('), '）', ')'), CHAR(10), ''), CHAR(13), ''), CHAR(9), '')
  ), 256)
WHERE `e`.`id` IS NULL
GROUP BY `t`.`raw_name`
ORDER BY `notice_count` DESC;
