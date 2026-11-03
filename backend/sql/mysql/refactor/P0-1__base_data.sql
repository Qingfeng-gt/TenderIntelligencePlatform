-- ============================================================
-- P0-1 基础数据域
-- 目标库: tender
-- 前置: 无
-- 特性: 只增不改；可重复执行（CREATE TABLE IF NOT EXISTS + INSERT ... ON DUPLICATE KEY UPDATE）
-- 回滚: P0-1__base_data_rollback.sql
-- 设计: doc/数据/13-数据模型-目标设计.md §六
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1) 行政区划（GB/T 2260）
--    说明：本脚本首批只保证「省级完整 + 演示城市」；
--          全量地市/区县请用 GB/T 2260 数据文件单独导入，不要手工补。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `base_area` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        varchar(12)  NOT NULL COMMENT '行政区划代码(GB/T 2260)',
  `parent_code` varchar(12)  NOT NULL DEFAULT '' COMMENT '上级代码，省级为空',
  `level`       tinyint      NOT NULL COMMENT '层级：1省/2市/3区县',
  `name`        varchar(64)  NOT NULL COMMENT '名称',
  `short_name`  varchar(32)  DEFAULT NULL COMMENT '简称，如 湖北',
  `full_name`   varchar(128) DEFAULT NULL COMMENT '全称，如 湖北省',
  `pinyin`      varchar(64)  DEFAULT NULL COMMENT '拼音',
  `region_type` varchar(16)  NOT NULL DEFAULT '' COMMENT 'province/city/district',
  `valid_from`  date         DEFAULT NULL COMMENT '生效日期',
  `valid_to`    date         DEFAULT NULL COMMENT '失效日期',
  `enabled`     bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序',
  `creator`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent` (`parent_code`),
  KEY `idx_name` (`name`),
  KEY `idx_short_name` (`short_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政区划';

-- 省级（34 个）
INSERT INTO `base_area` (`code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `sort`) VALUES
('110000', '', 1, '北京市', '北京', '北京市', 'beijing', 'province', 1),
('120000', '', 1, '天津市', '天津', '天津市', 'tianjin', 'province', 2),
('130000', '', 1, '河北省', '河北', '河北省', 'hebei', 'province', 3),
('140000', '', 1, '山西省', '山西', '山西省', 'shanxi', 'province', 4),
('150000', '', 1, '内蒙古自治区', '内蒙古', '内蒙古自治区', 'neimenggu', 'province', 5),
('210000', '', 1, '辽宁省', '辽宁', '辽宁省', 'liaoning', 'province', 6),
('220000', '', 1, '吉林省', '吉林', '吉林省', 'jilin', 'province', 7),
('230000', '', 1, '黑龙江省', '黑龙江', '黑龙江省', 'heilongjiang', 'province', 8),
('310000', '', 1, '上海市', '上海', '上海市', 'shanghai', 'province', 9),
('320000', '', 1, '江苏省', '江苏', '江苏省', 'jiangsu', 'province', 10),
('330000', '', 1, '浙江省', '浙江', '浙江省', 'zhejiang', 'province', 11),
('340000', '', 1, '安徽省', '安徽', '安徽省', 'anhui', 'province', 12),
('350000', '', 1, '福建省', '福建', '福建省', 'fujian', 'province', 13),
('360000', '', 1, '江西省', '江西', '江西省', 'jiangxi', 'province', 14),
('370000', '', 1, '山东省', '山东', '山东省', 'shandong', 'province', 15),
('410000', '', 1, '河南省', '河南', '河南省', 'henan', 'province', 16),
('420000', '', 1, '湖北省', '湖北', '湖北省', 'hubei', 'province', 17),
('430000', '', 1, '湖南省', '湖南', '湖南省', 'hunan', 'province', 18),
('440000', '', 1, '广东省', '广东', '广东省', 'guangdong', 'province', 19),
('450000', '', 1, '广西壮族自治区', '广西', '广西壮族自治区', 'guangxi', 'province', 20),
('460000', '', 1, '海南省', '海南', '海南省', 'hainan', 'province', 21),
('500000', '', 1, '重庆市', '重庆', '重庆市', 'chongqing', 'province', 22),
('510000', '', 1, '四川省', '四川', '四川省', 'sichuan', 'province', 23),
('520000', '', 1, '贵州省', '贵州', '贵州省', 'guizhou', 'province', 24),
('530000', '', 1, '云南省', '云南', '云南省', 'yunnan', 'province', 25),
('540000', '', 1, '西藏自治区', '西藏', '西藏自治区', 'xizang', 'province', 26),
('610000', '', 1, '陕西省', '陕西', '陕西省', 'shaanxi', 'province', 27),
('620000', '', 1, '甘肃省', '甘肃', '甘肃省', 'gansu', 'province', 28),
('630000', '', 1, '青海省', '青海', '青海省', 'qinghai', 'province', 29),
('640000', '', 1, '宁夏回族自治区', '宁夏', '宁夏回族自治区', 'ningxia', 'province', 30),
('650000', '', 1, '新疆维吾尔自治区', '新疆', '新疆维吾尔自治区', 'xinjiang', 'province', 31),
('710000', '', 1, '台湾省', '台湾', '台湾省', 'taiwan', 'province', 32),
('810000', '', 1, '香港特别行政区', '香港', '香港特别行政区', 'xianggang', 'province', 33),
('820000', '', 1, '澳门特别行政区', '澳门', '澳门特别行政区', 'aomen', 'province', 34)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `short_name` = VALUES(`short_name`),
  `full_name` = VALUES(`full_name`),
  `pinyin` = VALUES(`pinyin`),
  `region_type` = VALUES(`region_type`),
  `sort` = VALUES(`sort`),
  `enabled` = b'1';

-- 演示城市（覆盖现有 notice 种子中出现的地市；全量地市请用数据文件导入）
INSERT INTO `base_area` (`code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `sort`) VALUES
('420100', '420000', 2, '武汉市', '武汉', '武汉市', 'wuhan', 'city', 1),
('440300', '440000', 2, '深圳市', '深圳', '深圳市', 'shenzhen', 'city', 2),
('330100', '330000', 2, '杭州市', '杭州', '杭州市', 'hangzhou', 'city', 3),
('411300', '410000', 2, '南阳市', '南阳', '南阳市', 'nanyang', 'city', 4),
('370200', '370000', 2, '青岛市', '青岛', '青岛市', 'qingdao', 'city', 5),
('430100', '430000', 2, '长沙市', '长沙', '长沙市', 'changsha', 'city', 6),
('510100', '510000', 2, '成都市', '成都', '成都市', 'chengdu', 'city', 7),
('610100', '610000', 2, '西安市', '西安', '西安市', 'xian', 'city', 8),
('320500', '320000', 2, '苏州市', '苏州', '苏州市', 'suzhou', 'city', 9),
('340100', '340000', 2, '合肥市', '合肥', '合肥市', 'hefei', 'city', 10),
('630100', '630000', 2, '西宁市', '西宁', '西宁市', 'xining', 'city', 11),
('350200', '350000', 2, '厦门市', '厦门', '厦门市', 'xiamen', 'city', 12),
('360400', '360000', 2, '九江市', '九江', '九江市', 'jiujiang', 'city', 13),
('450100', '450000', 2, '南宁市', '南宁', '南宁市', 'nanning', 'city', 14),
('530100', '530000', 2, '昆明市', '昆明', '昆明市', 'kunming', 'city', 15),
('150600', '150000', 2, '鄂尔多斯市', '鄂尔多斯', '鄂尔多斯市', 'eerduosi', 'city', 16),
('520100', '520000', 2, '贵阳市', '贵阳', '贵阳市', 'guiyang', 'city', 17)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `short_name` = VALUES(`short_name`),
  `full_name` = VALUES(`full_name`),
  `pinyin` = VALUES(`pinyin`),
  `region_type` = VALUES(`region_type`),
  `sort` = VALUES(`sort`),
  `enabled` = b'1';

-- ------------------------------------------------------------
-- 2) 标准编码集
--    说明：代码值为本项目稳定标识；最终数值/编码需按 V2.0 附录核对后替换，
--          替换时只更新 name 与 code，不改引用列类型。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `base_code` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code_set`    varchar(32)  NOT NULL COMMENT '编码集',
  `code`        varchar(32)  NOT NULL COMMENT '代码',
  `name`        varchar(128) NOT NULL COMMENT '名称',
  `parent_code` varchar(32)  NOT NULL DEFAULT '' COMMENT '父代码，支持层级',
  `level`       tinyint      NOT NULL DEFAULT 1 COMMENT '层级',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled`     bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
  `creator`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_set_code` (`code_set`, `code`),
  KEY `idx_code_set_parent` (`code_set`, `parent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准编码集';

INSERT INTO `base_code` (`code_set`, `code`, `name`, `sort`, `remark`) VALUES
-- 企业角色
('enterprise_role', 'TENDERER', '招标人', 1, NULL),
('enterprise_role', 'AGENCY', '代理机构', 2, NULL),
('enterprise_role', 'SUPPLIER', '供应商', 3, NULL),
('enterprise_role', 'WINNER', '中标人', 4, NULL),
-- 公告类型
('notice_type', 'tender', '招标公告', 1, NULL),
('notice_type', 'win', '中标结果', 2, NULL),
('notice_type', 'change', '变更公告', 3, NULL),
('notice_type', 'explore', '采购公告', 4, NULL),
('notice_type', 'qualify', '资格预审公告', 5, NULL),
-- 公告子类型
('notice_subtype', 'announcement', '公告', 1, NULL),
('notice_subtype', 'clarify', '澄清/修改', 2, NULL),
('notice_subtype', 'terminate', '终止', 3, NULL),
('notice_subtype', 'candidate', '中标候选人公示', 4, NULL),
('notice_subtype', 'result', '中标结果', 5, NULL),
-- 采购方式
('procurement_method', 'OPEN_BID', '公开招标', 1, '代码值为占位，待按 V2.0 附录核对'),
('procurement_method', 'INVITE_BID', '邀请招标', 2, '代码值为占位，待按 V2.0 附录核对'),
('procurement_method', 'COMPETITIVE_NEGOTIATION', '竞争性谈判', 3, '代码值为占位，待按 V2.0 附录核对'),
('procurement_method', 'COMPETITIVE_CONSULT', '竞争性磋商', 4, '代码值为占位，待按 V2.0 附录核对'),
('procurement_method', 'INQUIRY', '询价', 5, '代码值为占位，待按 V2.0 附录核对'),
('procurement_method', 'SINGLE_SOURCE', '单一来源', 6, '代码值为占位，待按 V2.0 附录核对'),
('procurement_method', 'OTHER', '其他', 99, '代码值为占位，待按 V2.0 附录核对'),
-- 资金来源
('funding_source', 'FISCAL', '财政资金', 1, NULL),
('funding_source', 'SELF_RAISED', '自筹资金', 2, NULL),
('funding_source', 'LOAN', '贷款', 3, NULL),
('funding_source', 'PPP', 'PPP', 4, NULL),
('funding_source', 'OTHER', '其他', 99, NULL),
-- 资质等级
('qualification_level', 'SPECIAL', '特级', 1, NULL),
('qualification_level', 'FIRST', '一级', 2, NULL),
('qualification_level', 'SECOND', '二级', 3, NULL),
('qualification_level', 'THIRD', '三级', 4, NULL),
-- 评标办法
('evaluation_method', 'COMPREHENSIVE', '综合评分法', 1, NULL),
('evaluation_method', 'LOWEST_PRICE', '最低价法', 2, NULL),
('evaluation_method', 'EVALUATED_LOWEST', '经评审最低价法', 3, NULL),
-- 是否接受联合体
('consortium_flag', 'YES', '接受联合体', 1, NULL),
('consortium_flag', 'NO', '不接受联合体', 2, NULL),
-- 附件分类
('file_category', 'TENDER_DOC', '招标文件', 1, NULL),
('file_category', 'BILL_OF_QUANTITIES', '工程量清单', 2, NULL),
('file_category', 'DRAWING', '图纸', 3, NULL),
('file_category', 'OTHER', '其他', 99, NULL),
-- 金额单位
('amount_unit', 'YUAN', '元', 1, NULL),
('amount_unit', 'WAN_YUAN', '万元', 2, NULL),
('amount_unit', 'YI_YUAN', '亿元', 3, NULL),
-- 推送渠道
('push_channel', 'SITE', '站内信', 1, NULL),
('push_channel', 'EMAIL', '邮件', 2, NULL),
('push_channel', 'WECOM', '企业微信', 3, NULL),
('push_channel', 'FEISHU', '飞书', 4, NULL),
('push_channel', 'WECHAT_MP', '微信服务号', 5, NULL),
('push_channel', 'SMS', '短信', 6, NULL),
-- 交易分类
('trade_category', 'ENGINEERING', '工程建设', 1, NULL),
('trade_category', 'GOVERNMENT_PROCUREMENT', '政府采购', 2, NULL),
('trade_category', 'LAND', '土地使用权', 3, NULL),
('trade_category', 'PROPERTY', '国有产权', 4, NULL),
('trade_category', 'OTHER', '其他', 99, NULL)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `sort` = VALUES(`sort`),
  `remark` = VALUES(`remark`),
  `enabled` = b'1';

-- ------------------------------------------------------------
-- 3) 源站字段映射
--    说明：P0 只建表，不预置映射；P0-3 随 ccgp/ggzy 解析器一起补。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `base_field_mapping` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_site_code` varchar(32)  NOT NULL COMMENT '源站 code',
  `source_field`    varchar(128) NOT NULL COMMENT '源站字段名或解析路径',
  `standard_table`  varchar(64)  NOT NULL COMMENT '目标表',
  `standard_field`  varchar(64)  NOT NULL COMMENT '目标列',
  `required`        bit(1)       NOT NULL DEFAULT b'0' COMMENT 'V2.0 是否必填',
  `availability`    varchar(16)  NOT NULL DEFAULT 'UNKNOWN' COMMENT 'ALWAYS/OFTEN/RARE/NEVER',
  `transform_rule`  varchar(500) DEFAULT NULL COMMENT '换算/清洗规则',
  `parser_version`  varchar(32)  NOT NULL DEFAULT '' COMMENT '解析器版本',
  `enabled`         bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `creator`         varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_standard` (`source_site_code`, `standard_table`, `standard_field`),
  KEY `idx_standard` (`standard_table`, `standard_field`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='源站字段映射';

SET FOREIGN_KEY_CHECKS = 1;
