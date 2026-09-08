-- 数据采集中心 - 后台菜单种子(幂等: 可重复执行)
-- 超级管理员角色自动拥有全部菜单, 无需 system_role_menu 记录
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(12800, '数据采集中心', '', 2, 30, 0, '/crawler', 'ep:data-analysis', 'crawler/index', 'CrawlerCenter', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`);
