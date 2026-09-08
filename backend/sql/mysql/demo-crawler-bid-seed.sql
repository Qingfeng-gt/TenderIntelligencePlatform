-- 爬虫站点种子数据(幂等: 可重复执行; 依赖 demo-crawler-bid.sql 建表)
INSERT INTO `crawler_site` (`id`, `name`, `code`, `enabled`, `channels`, `interval_ms`, `config`, `tenant_id`) VALUES
(1, '中国政府采购网', 'ccgp', b'1',
 CONCAT(
  '[{"path":"/cggg/dfgg/gkzb/","name":"地方-公开招标","type":"tender","pageCount":1},',
  '{"path":"/cggg/dfgg/zbgg/","name":"地方-中标公告","type":"win","pageCount":1},',
  '{"path":"/cggg/dfgg/gzgg/","name":"地方-更正公告","type":"change","pageCount":1},',
  '{"path":"/cggg/dfgg/xjgg/","name":"地方-询价公告","type":"explore","pageCount":1}]'),
 3000,
 '{"baseUrl":"http://www.ccgp.gov.cn","initialUrl":"http://www.ccgp.gov.cn/","userAgent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"}',
 0)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `enabled` = VALUES(`enabled`),
  `channels` = VALUES(`channels`),
  `interval_ms` = VALUES(`interval_ms`),
  `config` = VALUES(`config`);

-- 贵州省招标投标公共服务平台(JSON API 源站; channels[].path = search 的 noticeType 类别编码)
-- 2026-09-08 实测: 该站 search 列表接口当前返回 0 条(列表数据下线), GetDetail 可用, 源站恢复后即自动采到数据
INSERT INTO `crawler_site` (`id`, `name`, `code`, `enabled`, `channels`, `interval_ms`, `config`, `tenant_id`) VALUES
(2, '贵州省招标投标公共服务平台', 'ztb_gz', b'1',
 CONCAT(
  '[{"path":"A01","name":"招标公告","type":"tender","pageCount":1},',
  '{"path":"A02","name":"变更公告","type":"change","pageCount":1},',
  '{"path":"A04","name":"中标结果公示","type":"win","pageCount":1}]'),
 3000,
 '{"baseUrl":"http://ztb.guizhou.gov.cn","initialUrl":"http://ztb.guizhou.gov.cn/","userAgent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36","xhrReferer":"http://ztb.guizhou.gov.cn/trade/?category=affiche"}',
 0)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `enabled` = VALUES(`enabled`),
  `channels` = VALUES(`channels`),
  `interval_ms` = VALUES(`interval_ms`),
  `config` = VALUES(`config`);
