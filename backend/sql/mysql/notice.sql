-- =====================================================================
-- 招投标情报平台 · 业务全量初始化（P0 终态，一次性）
--
-- 说明：
--   本脚本直接创建最终结构，不再依赖 refactor/P0-1 ~ P0-6 的增量迁移。
--   包含：基础数据 / 站点频道 / 菜单种子；业务表为空，可直接开始采集。
--   如需演示公告，额外执行 demo-data.sql（可选，20 条）。
--
-- 执行：
--   mysql -uroot -p < backend/sql/mysql/notice.sql
--   （本脚本会自动 USE tender；须在 tender.sql 之后执行）
--
-- 注意：
--   - 业务表 DROP + CREATE，仅用于新库初始化或整体重置；重复执行会清空业务数据，仅保留基础/站点/菜单种子。
--   - 框架表（system_* / infra_* / member_user）不受影响；自定义菜单使用 ON DUPLICATE KEY UPDATE。
--   - 如需演示公告，额外执行 demo-data.sql（可选，20 条）。
--   - 新库不再需要执行 refactor/P0__run_all.sh。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `tender` DEFAULT CHARACTER SET utf8mb4;
USE `tender`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 1. 表结构（最终结构）
-- =====================================================================
-- ------------------------------------------------------------
-- 1.1 基础数据
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `base_area`;
CREATE TABLE `base_area` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(12) NOT NULL COMMENT '行政区划代码(GB/T 2260)',
  `parent_code` varchar(12) NOT NULL DEFAULT '' COMMENT '上级代码，省级为空',
  `level` tinyint NOT NULL COMMENT '层级：1省/2市/3区县',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `short_name` varchar(32) DEFAULT NULL COMMENT '简称，如 湖北',
  `full_name` varchar(128) DEFAULT NULL COMMENT '全称，如 湖北省',
  `pinyin` varchar(64) DEFAULT NULL COMMENT '拼音',
  `region_type` varchar(16) NOT NULL DEFAULT '' COMMENT 'province/city/district',
  `valid_from` date DEFAULT NULL COMMENT '生效日期',
  `valid_to` date DEFAULT NULL COMMENT '失效日期',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent` (`parent_code`),
  KEY `idx_name` (`name`),
  KEY `idx_short_name` (`short_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行政区划';
DROP TABLE IF EXISTS `base_code`;
CREATE TABLE `base_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code_set` varchar(32) NOT NULL COMMENT '编码集',
  `code` varchar(32) NOT NULL COMMENT '代码',
  `name` varchar(128) NOT NULL COMMENT '名称',
  `parent_code` varchar(32) NOT NULL DEFAULT '' COMMENT '父代码，支持层级',
  `level` tinyint NOT NULL DEFAULT '1' COMMENT '层级',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_set_code` (`code_set`,`code`),
  KEY `idx_code_set_parent` (`code_set`,`parent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标准编码集';
DROP TABLE IF EXISTS `base_field_mapping`;
CREATE TABLE `base_field_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_site_code` varchar(32) NOT NULL COMMENT '源站 code',
  `source_field` varchar(128) NOT NULL COMMENT '源站字段名或解析路径',
  `standard_table` varchar(64) NOT NULL COMMENT '目标表',
  `standard_field` varchar(64) NOT NULL COMMENT '目标列',
  `required` bit(1) NOT NULL DEFAULT b'0' COMMENT 'V2.0 是否必填',
  `availability` varchar(16) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'ALWAYS/OFTEN/RARE/NEVER',
  `transform_rule` varchar(500) DEFAULT NULL COMMENT '换算/清洗规则',
  `parser_version` varchar(32) NOT NULL DEFAULT '' COMMENT '解析器版本',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_standard` (`source_site_code`,`standard_table`,`standard_field`),
  KEY `idx_standard` (`standard_table`,`standard_field`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='源站字段映射';
-- ------------------------------------------------------------
-- 1.2 企业主体
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `enterprise`;
CREATE TABLE `enterprise` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '企业 ID',
  `name` varchar(256) NOT NULL COMMENT '企业名称（源站原文）',
  `normalized_name` varchar(256) NOT NULL DEFAULT '' COMMENT '归一化名称（去空格/全半角/括号）',
  `name_hash` varchar(64) NOT NULL DEFAULT '' COMMENT '归一化名称 SHA-256',
  `unified_social_credit_code` varchar(64) DEFAULT NULL COMMENT '统一社会信用代码',
  `enterprise_type_code` varchar(32) NOT NULL DEFAULT '' COMMENT '企业类型代码',
  `legal_person` varchar(64) NOT NULL DEFAULT '' COMMENT '法定代表人',
  `registered_capital` decimal(18,2) DEFAULT NULL COMMENT '注册资本（万元）',
  `established_date` date DEFAULT NULL COMMENT '成立日期',
  `address` varchar(512) NOT NULL DEFAULT '' COMMENT '企业地址',
  `region_code` varchar(12) NOT NULL DEFAULT '' COMMENT '行政区划代码',
  `province_code` varchar(12) NOT NULL DEFAULT '' COMMENT '省级代码',
  `city_code` varchar(12) NOT NULL DEFAULT '' COMMENT '市级代码',
  `district_code` varchar(12) NOT NULL DEFAULT '' COMMENT '区县代码',
  `industry_code` varchar(32) NOT NULL DEFAULT '' COMMENT '行业代码',
  `contact_name` varchar(128) NOT NULL DEFAULT '' COMMENT '默认联系人',
  `contact_phone` varchar(255) NOT NULL DEFAULT '' COMMENT '默认联系电话（脱敏后展示）',
  `email` varchar(128) NOT NULL DEFAULT '' COMMENT '邮箱',
  `website` varchar(512) NOT NULL DEFAULT '' COMMENT '官网',
  `source_type` varchar(32) NOT NULL DEFAULT '' COMMENT 'NOTICE/MANUAL/IMPORT/NOTICE_BACKFILL',
  `source_url` varchar(1024) NOT NULL DEFAULT '' COMMENT '首次来源 URL',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/MERGED/INVALID',
  `merged_to_id` bigint DEFAULT NULL COMMENT '被合并到的企业 ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_credit_code` (`unified_social_credit_code`),
  KEY `idx_name_hash` (`name_hash`),
  KEY `idx_normalized_name` (`normalized_name`(191)),
  KEY `idx_name_prefix` (`name`(191)),
  KEY `idx_region` (`region_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业主体';
DROP TABLE IF EXISTS `enterprise_role`;
CREATE TABLE `enterprise_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enterprise_id` bigint NOT NULL COMMENT '企业 ID',
  `role_code` varchar(32) NOT NULL COMMENT 'TENDERER/AGENCY/SUPPLIER/WINNER',
  `first_seen_notice_id` bigint DEFAULT NULL COMMENT '首次出现的公告 ID',
  `last_seen_notice_id` bigint DEFAULT NULL COMMENT '最近出现的公告 ID',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enterprise_role` (`enterprise_id`,`role_code`),
  KEY `idx_role` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业角色';
DROP TABLE IF EXISTS `enterprise_contact`;
CREATE TABLE `enterprise_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enterprise_id` bigint NOT NULL COMMENT '企业 ID',
  `name` varchar(128) NOT NULL DEFAULT '' COMMENT '姓名',
  `phone` varchar(255) NOT NULL DEFAULT '' COMMENT '电话（脱敏后展示）',
  `email` varchar(128) NOT NULL DEFAULT '' COMMENT '邮箱',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '职务',
  `contact_type_code` varchar(32) NOT NULL DEFAULT '' COMMENT '招标/中标/其他',
  `source_notice_id` bigint DEFAULT NULL COMMENT '来源公告 ID，便于溯源',
  `confidence` decimal(5,2) DEFAULT NULL COMMENT '抽取置信度',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INVALID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise` (`enterprise_id`),
  KEY `idx_phone` (`phone`(64))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业联系人';
DROP TABLE IF EXISTS `enterprise_alias`;
CREATE TABLE `enterprise_alias` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enterprise_id` bigint NOT NULL COMMENT '企业 ID',
  `alias_name` varchar(256) NOT NULL COMMENT '别名/历史名/简称',
  `normalized_alias` varchar(256) NOT NULL DEFAULT '' COMMENT '归一化别名',
  `alias_type` varchar(32) NOT NULL DEFAULT 'OTHER' COMMENT 'HISTORY/ABBR/OTHER',
  `source` varchar(64) NOT NULL DEFAULT '' COMMENT '来源',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enterprise_alias` (`enterprise_id`,`normalized_alias`(191)),
  KEY `idx_normalized_alias` (`normalized_alias`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业别名';
-- ------------------------------------------------------------
-- 1.3 标讯四层
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目 ID',
  `project_key` varchar(128) NOT NULL COMMENT '本地归并键（规则生成，唯一）',
  `unified_deal_code` varchar(64) DEFAULT NULL COMMENT '统一交易标识码',
  `project_no` varchar(128) NOT NULL DEFAULT '' COMMENT '项目编号',
  `project_name` varchar(512) NOT NULL DEFAULT '' COMMENT '项目名称',
  `trade_category_code` varchar(32) NOT NULL DEFAULT '' COMMENT '交易分类代码',
  `project_category_code` varchar(32) NOT NULL DEFAULT '' COMMENT '项目类别代码',
  `region_code` varchar(12) NOT NULL DEFAULT '' COMMENT '行政区划代码',
  `province_code` varchar(12) NOT NULL DEFAULT '' COMMENT '省级代码',
  `city_code` varchar(12) NOT NULL DEFAULT '' COMMENT '市级代码',
  `district_code` varchar(12) NOT NULL DEFAULT '' COMMENT '区县代码',
  `budget` decimal(18,2) DEFAULT NULL COMMENT '预算（万元）',
  `funding_source_code` varchar(32) NOT NULL DEFAULT '' COMMENT '资金来源代码',
  `tenderer_enterprise_id` bigint DEFAULT NULL COMMENT '招标人企业 ID',
  `agency_enterprise_id` bigint DEFAULT NULL COMMENT '代理机构企业 ID',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED/CANCELLED',
  `first_publish_time` datetime DEFAULT NULL COMMENT '首次公告时间',
  `last_publish_time` datetime DEFAULT NULL COMMENT '最近公告时间',
  `notice_count` int NOT NULL DEFAULT '0' COMMENT '公告数（冗余统计）',
  `lot_count` int NOT NULL DEFAULT '0' COMMENT '标段数（冗余统计）',
  `source_count` int NOT NULL DEFAULT '0' COMMENT '来源站点数（冗余统计）',
  `missing_fields` json DEFAULT NULL COMMENT '缺失字段汇总',
  `data_quality_status` varchar(16) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'OK/PARTIAL/BROKEN/UNKNOWN',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_key` (`project_key`),
  UNIQUE KEY `uk_unified_deal_code` (`unified_deal_code`),
  KEY `idx_project_no` (`project_no`),
  KEY `idx_region_category` (`region_code`,`trade_category_code`),
  KEY `idx_last_publish` (`last_publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目数据集';
DROP TABLE IF EXISTS `tender_project`;
CREATE TABLE `tender_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招标项目 ID',
  `project_id` bigint NOT NULL COMMENT '项目 ID',
  `tender_project_no` varchar(128) DEFAULT NULL COMMENT '招标项目编号',
  `tender_project_name` varchar(512) NOT NULL DEFAULT '' COMMENT '招标项目名称',
  `procurement_method_code` varchar(32) NOT NULL DEFAULT '' COMMENT '采购方式代码',
  `budget` decimal(18,2) DEFAULT NULL COMMENT '预算（万元）',
  `price_ceiling` decimal(18,2) DEFAULT NULL COMMENT '最高限价（万元）',
  `funding_source_code` varchar(32) NOT NULL DEFAULT '' COMMENT '资金来源代码',
  `lot_count` int NOT NULL DEFAULT '0' COMMENT '标段数量',
  `contract_period_days` int DEFAULT NULL COMMENT '合同履行期限（天）',
  `contract_period_text` varchar(128) NOT NULL DEFAULT '' COMMENT '合同期限原文',
  `qualification_summary` text COMMENT '资格条件摘要',
  `evaluation_method_code` varchar(32) NOT NULL DEFAULT '' COMMENT '评标办法代码',
  `tenderer_enterprise_id` bigint DEFAULT NULL COMMENT '招标人企业 ID',
  `agency_enterprise_id` bigint DEFAULT NULL COMMENT '代理机构企业 ID',
  `region_code` varchar(12) NOT NULL DEFAULT '' COMMENT '行政区划代码',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED/CANCELLED',
  `missing_fields` json DEFAULT NULL COMMENT '缺失字段汇总',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_no` (`project_id`,`tender_project_no`),
  KEY `idx_project` (`project_id`),
  KEY `idx_region_publish` (`region_code`,`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='招标项目数据集';
DROP TABLE IF EXISTS `tender_lot`;
CREATE TABLE `tender_lot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标段 ID',
  `tender_project_id` bigint NOT NULL COMMENT '招标项目 ID',
  `project_id` bigint NOT NULL COMMENT '项目 ID（冗余）',
  `lot_no` varchar(128) DEFAULT NULL COMMENT '标段（包）编号',
  `lot_name` varchar(512) NOT NULL DEFAULT '' COMMENT '标段名称',
  `lot_content` text COMMENT '标段内容/范围',
  `budget` decimal(18,2) DEFAULT NULL COMMENT '标段预算（万元）',
  `price_ceiling` decimal(18,2) DEFAULT NULL COMMENT '最高限价（万元）',
  `deposit_amount` decimal(18,2) DEFAULT NULL COMMENT '保证金（万元）',
  `qualification_requirement` text COMMENT '资格要求',
  `specific_qualification` text COMMENT '特定资格',
  `consortium_allowed` bit(1) DEFAULT NULL COMMENT '是否接受联合体；NULL 未知',
  `performance_requirement` text COMMENT '业绩要求',
  `qualification_level_code` varchar(32) NOT NULL DEFAULT '' COMMENT '资质等级代码',
  `evaluation_method_code` varchar(32) NOT NULL DEFAULT '' COMMENT '评标办法代码',
  `region_code` varchar(12) NOT NULL DEFAULT '' COMMENT '行政区划代码',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED/CANCELLED',
  `missing_fields` json DEFAULT NULL COMMENT '缺失字段汇总',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tender_project_lot` (`tender_project_id`,`lot_no`),
  KEY `idx_project` (`project_id`),
  KEY `idx_region` (`region_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标段（包）数据集';
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告编号',
  `title` varchar(512) NOT NULL DEFAULT '' COMMENT '公告标题',
  `project_no` varchar(128) NOT NULL DEFAULT '' COMMENT '项目编号(源站)',
  `industry` varchar(64) NOT NULL DEFAULT '' COMMENT '行业',
  `budget` decimal(18,2) DEFAULT NULL COMMENT '预算金额(万元)',
  `contact` varchar(128) NOT NULL DEFAULT '' COMMENT '联系人(源站为自由文本,可能是多人列表)',
  `contact_phone` varchar(255) NOT NULL DEFAULT '' COMMENT '联系电话(源站为自由文本,可能含多个号码与附注)',
  `content` longtext COMMENT '公告原文(HTML)',
  `source_url` varchar(512) DEFAULT NULL COMMENT '源站详情URL(唯一去重键)',
  `publish_time` datetime NOT NULL COMMENT '发布时间',
  `open_time` datetime DEFAULT NULL COMMENT '开标时间',
  `region_code` varchar(16) NOT NULL DEFAULT '' COMMENT '源站地区代码',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `project_id` bigint DEFAULT NULL COMMENT '项目 ID',
  `tender_project_id` bigint DEFAULT NULL COMMENT '招标项目 ID',
  `lot_id` bigint DEFAULT NULL COMMENT '标段 ID',
  `unified_deal_code` varchar(64) DEFAULT NULL COMMENT '统一交易标识码',
  `tender_project_no` varchar(128) NOT NULL DEFAULT '' COMMENT '招标项目编号',
  `lot_no` varchar(128) NOT NULL DEFAULT '' COMMENT '标段（包）编号',
  `notice_no` varchar(128) NOT NULL DEFAULT '' COMMENT '公告标识/编号',
  `notice_type_code` varchar(32) NOT NULL DEFAULT '' COMMENT '公告类型代码',
  `notice_subtype_code` varchar(32) NOT NULL DEFAULT '' COMMENT '公告子类型代码',
  `source_site_code` varchar(32) NOT NULL DEFAULT '' COMMENT '源站 code',
  `source_platform` varchar(128) NOT NULL DEFAULT '' COMMENT '来源平台名称',
  `file_get_start_time` datetime DEFAULT NULL COMMENT '文件获取开始',
  `file_get_end_time` datetime DEFAULT NULL COMMENT '文件获取截止',
  `qa_deadline` datetime DEFAULT NULL COMMENT '答疑/澄清截止',
  `sign_deadline` datetime DEFAULT NULL COMMENT '报名截止',
  `bid_deadline` datetime DEFAULT NULL COMMENT '投标截止',
  `open_place` varchar(512) NOT NULL DEFAULT '' COMMENT '开标地点',
  `publicity_start_time` datetime DEFAULT NULL COMMENT '公示开始',
  `publicity_end_time` datetime DEFAULT NULL COMMENT '公示结束',
  `price_ceiling` decimal(18,2) DEFAULT NULL COMMENT '最高限价（万元）',
  `deposit_amount` decimal(18,2) DEFAULT NULL COMMENT '保证金（万元）',
  `file_price` decimal(18,2) DEFAULT NULL COMMENT '文件售价（万元）',
  `win_amount` decimal(18,2) DEFAULT NULL COMMENT '中标金额（万元）',
  `procurement_method_code` varchar(32) NOT NULL DEFAULT '' COMMENT '采购方式代码',
  `funding_source_code` varchar(32) NOT NULL DEFAULT '' COMMENT '资金来源代码',
  `evaluation_method_code` varchar(32) NOT NULL DEFAULT '' COMMENT '评标办法代码',
  `qualification_requirement` text COMMENT '资格要求',
  `specific_qualification` text COMMENT '特定资格',
  `consortium_allowed` bit(1) DEFAULT NULL COMMENT '是否接受联合体；NULL 未知',
  `performance_requirement` text COMMENT '业绩要求',
  `qualification_level_code` varchar(32) NOT NULL DEFAULT '' COMMENT '资质等级代码',
  `trade_category_code` varchar(32) NOT NULL DEFAULT '' COMMENT '交易分类代码',
  `province_code` varchar(12) NOT NULL DEFAULT '' COMMENT '省级代码',
  `city_code` varchar(12) NOT NULL DEFAULT '' COMMENT '市级代码',
  `district_code` varchar(12) NOT NULL DEFAULT '' COMMENT '区县代码',
  `tenderer_enterprise_id` bigint DEFAULT NULL COMMENT '招标人企业 ID',
  `agency_enterprise_id` bigint DEFAULT NULL COMMENT '代理机构企业 ID',
  `winner_enterprise_id` bigint DEFAULT NULL COMMENT '中标人企业 ID',
  `industry_code` varchar(32) NOT NULL DEFAULT '' COMMENT '行业代码',
  `has_attachment` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否有附件',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览次数',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/OFFLINE',
  `missing_fields` json DEFAULT NULL COMMENT '缺失字段汇总',
  `data_quality_status` varchar(16) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'OK/PARTIAL/BROKEN/UNKNOWN',
  `raw_id` bigint DEFAULT NULL COMMENT '原始快照 ID',
  `parser_version` varchar(32) NOT NULL DEFAULT '' COMMENT '解析器版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_url` (`source_url`),
  KEY `idx_industry` (`industry`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_notice_type_publish` (`notice_type_code`,`publish_time`),
  KEY `idx_notice_project` (`project_id`),
  KEY `idx_notice_tender_project` (`tender_project_id`),
  KEY `idx_notice_lot` (`lot_id`),
  KEY `idx_notice_region_publish` (`region_code`,`publish_time`),
  KEY `idx_notice_unified_code` (`unified_deal_code`),
  KEY `idx_notice_project_no` (`project_no`),
  KEY `idx_notice_lot_no` (`lot_no`),
  KEY `idx_notice_bid_deadline` (`bid_deadline`),
  KEY `idx_notice_source_site_time` (`source_site_code`,`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='招投标公告';
-- ------------------------------------------------------------
-- 1.4 公告附属
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `notice_attachment`;
CREATE TABLE `notice_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件编号',
  `notice_id` bigint NOT NULL COMMENT '公告编号(notice.id)',
  `file_name` varchar(512) NOT NULL DEFAULT '' COMMENT '附件文件名(源站锚文本)',
  `file_url` varchar(1024) NOT NULL DEFAULT '' COMMENT '源站附件直链(绝对URL)',
  `file_type` varchar(16) NOT NULL DEFAULT '' COMMENT '文件类型(pdf/doc/docx/xls/xlsx/zip 等)',
  `file_size` varchar(32) NOT NULL DEFAULT '' COMMENT '源站标注的文件大小(自由文本,如 546.3K;未标注为空)',
  `sort` int NOT NULL DEFAULT '0' COMMENT '同一公告内的展示顺序(按源站出现次序)',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `file_category_code` varchar(32) NOT NULL DEFAULT '' COMMENT '招标文件/清单/图纸/其他',
  `file_price` decimal(18,2) DEFAULT NULL COMMENT '文件售价（万元）',
  `acquisition_method_code` varchar(32) NOT NULL DEFAULT '' COMMENT '线上/线下/邮寄',
  `source_attachment_id` varchar(128) NOT NULL DEFAULT '' COMMENT '源站附件标识',
  `file_hash` varchar(64) NOT NULL DEFAULT '' COMMENT '文件哈希（若平台落盘）',
  `local_file_id` bigint DEFAULT NULL COMMENT '平台落盘时关联 infra_file.id',
  PRIMARY KEY (`id`),
  KEY `idx_notice` (`notice_id`),
  KEY `idx_attachment_category` (`file_category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标讯附件';
DROP TABLE IF EXISTS `notice_change`;
CREATE TABLE `notice_change` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notice_id` bigint NOT NULL COMMENT '原公告 ID',
  `change_type_code` varchar(32) NOT NULL DEFAULT '' COMMENT '澄清/修改/终止',
  `change_title` varchar(512) NOT NULL DEFAULT '' COMMENT '变更标题',
  `change_content` longtext COMMENT '变更内容',
  `change_time` datetime DEFAULT NULL COMMENT '变更时间',
  `source_url` varchar(1024) NOT NULL DEFAULT '' COMMENT '变更公告 URL',
  `raw_id` bigint DEFAULT NULL COMMENT '原始快照 ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_notice` (`notice_id`),
  KEY `idx_change_time` (`change_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告澄清修改';
DROP TABLE IF EXISTS `notice_enterprise`;
CREATE TABLE `notice_enterprise` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notice_id` bigint NOT NULL COMMENT '公告 ID',
  `enterprise_id` bigint NOT NULL COMMENT '企业 ID',
  `role_code` varchar(32) NOT NULL COMMENT 'TENDERER/AGENCY/WINNER/SUPPLIER',
  `source_text` varchar(512) NOT NULL DEFAULT '' COMMENT '公告中的原文',
  `confidence` decimal(5,2) DEFAULT NULL COMMENT '抽取置信度',
  `status` varchar(16) NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO/CONFIRMED/REJECTED',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_enterprise_role` (`notice_id`,`enterprise_id`,`role_code`),
  KEY `idx_enterprise_role` (`enterprise_id`,`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告-企业关联';
DROP TABLE IF EXISTS `notice_quality`;
CREATE TABLE `notice_quality` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notice_id` bigint NOT NULL COMMENT '公告 ID',
  `issue_type` varchar(32) NOT NULL COMMENT 'MISSING_FIELD/PARSE_ERROR/INVALID_VALUE/DUPLICATE/OTHER',
  `field_name` varchar(64) NOT NULL DEFAULT '' COMMENT '字段名',
  `severity` varchar(16) NOT NULL DEFAULT 'WARN' COMMENT 'INFO/WARN/ERROR',
  `detail` varchar(1000) NOT NULL DEFAULT '' COMMENT '问题描述',
  `source_url` varchar(1024) NOT NULL DEFAULT '' COMMENT '来源 URL',
  `parser_version` varchar(32) NOT NULL DEFAULT '' COMMENT '解析器版本',
  `status` varchar(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/RESOLVED/IGNORED',
  `resolved_by` varchar(64) NOT NULL DEFAULT '' COMMENT '处理人',
  `resolved_time` datetime DEFAULT NULL COMMENT '处理时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_notice` (`notice_id`),
  KEY `idx_status_severity` (`status`,`severity`),
  KEY `idx_field` (`field_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告数据质量';
-- ------------------------------------------------------------
-- 1.5 采集治理
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `crawler_site`;
CREATE TABLE `crawler_site` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '站点编号',
  `name` varchar(64) NOT NULL COMMENT '站点名称',
  `code` varchar(32) NOT NULL COMMENT '站点标识,对应 SourceAdapter 实现',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `channels` varchar(4096) NOT NULL DEFAULT '' COMMENT '频道JSON:[{path,name,type,pageCount}]',
  `interval_ms` bigint NOT NULL DEFAULT '3000' COMMENT '详情页请求间隔(ms,防反爬)',
  `config` varchar(2048) NOT NULL DEFAULT '' COMMENT '抓取配置JSON(UA/首访URL等)',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `tier` varchar(16) NOT NULL DEFAULT 'official' COMMENT 'official/industry/agency',
  `schedule_cron` varchar(64) DEFAULT NULL COMMENT '站点级调度表达式',
  `priority` int NOT NULL DEFAULT '100' COMMENT '优先级，越小越优先',
  `region_scope` varchar(500) NOT NULL DEFAULT '' COMMENT '覆盖区域代码 JSON',
  `last_success_time` datetime DEFAULT NULL COMMENT '最近成功时间',
  `last_fail_time` datetime DEFAULT NULL COMMENT '最近失败时间',
  `health_status` varchar(16) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN/HEALTHY/DEGRADED/DOWN',
  `adapter_version` varchar(32) NOT NULL DEFAULT '' COMMENT '适配器版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_tier_enabled` (`tier`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='爬虫站点配置';
DROP TABLE IF EXISTS `crawler_channel`;
CREATE TABLE `crawler_channel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `site_code` varchar(32) NOT NULL COMMENT '站点 code',
  `path` varchar(255) NOT NULL COMMENT '频道路径或类别编码',
  `name` varchar(128) NOT NULL DEFAULT '' COMMENT '频道名',
  `notice_type_code` varchar(32) NOT NULL DEFAULT '' COMMENT '公告类型代码',
  `page_count` int NOT NULL DEFAULT '1' COMMENT '每轮最大翻页数',
  `schedule_cron` varchar(64) DEFAULT NULL COMMENT '频道级调度',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `config` json DEFAULT NULL COMMENT '频道级扩展配置',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_path_type` (`site_code`,`path`,`notice_type_code`),
  KEY `idx_site_enabled` (`site_code`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采集频道配置';
DROP TABLE IF EXISTS `crawler_task`;
CREATE TABLE `crawler_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务编号',
  `site_code` varchar(32) NOT NULL COMMENT '站点标识',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `status` varchar(16) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/SUCCESS/FAILED',
  `list_fetched` int NOT NULL DEFAULT '0' COMMENT '列表页抓取数',
  `list_parsed` int NOT NULL DEFAULT '0' COMMENT '列表解析出公告数',
  `detail_fetched` int NOT NULL DEFAULT '0' COMMENT '详情页抓取成功数',
  `detail_failed` int NOT NULL DEFAULT '0' COMMENT '详情页抓取失败数',
  `new_insert` int NOT NULL DEFAULT '0' COMMENT '新增入库数',
  `updated` int NOT NULL DEFAULT '0' COMMENT '变更更新数',
  `error_msg` varchar(512) NOT NULL DEFAULT '' COMMENT '失败原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `trigger_type` varchar(16) NOT NULL DEFAULT 'SCHEDULE' COMMENT 'MANUAL/SCHEDULE/RETRY',
  `duration_ms` bigint NOT NULL DEFAULT '0' COMMENT '总耗时',
  `config_snapshot` json DEFAULT NULL COMMENT '本轮站点/频道配置快照',
  `error_type` varchar(32) NOT NULL DEFAULT '' COMMENT '异常分类',
  `data_date` date DEFAULT NULL COMMENT '数据日期',
  PRIMARY KEY (`id`),
  KEY `idx_site_start` (`site_code`,`start_time`),
  KEY `idx_trigger_type_time` (`trigger_type`,`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='爬虫采集任务日志';
DROP TABLE IF EXISTS `crawler_fetch_log`;
CREATE TABLE `crawler_fetch_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint NOT NULL COMMENT '任务 ID',
  `site_code` varchar(32) NOT NULL COMMENT '站点 code',
  `channel_path` varchar(255) NOT NULL DEFAULT '' COMMENT '频道',
  `page_type` varchar(16) NOT NULL DEFAULT '' COMMENT 'LIST/DETAIL',
  `url` varchar(1024) NOT NULL DEFAULT '' COMMENT '请求 URL',
  `http_status` int DEFAULT NULL COMMENT 'HTTP 状态',
  `duration_ms` int NOT NULL DEFAULT '0' COMMENT '耗时',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'SUCCESS/FAILED/SKIPPED',
  `error_type` varchar(32) NOT NULL DEFAULT '' COMMENT '超时/403/解析失败等',
  `error_msg` varchar(512) NOT NULL DEFAULT '' COMMENT '错误摘要',
  `fetched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抓取时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_site_time` (`site_code`,`fetched_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='逐 URL 抓取日志';
DROP TABLE IF EXISTS `crawl_raw`;
CREATE TABLE `crawl_raw` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint DEFAULT NULL COMMENT '任务 ID',
  `site_code` varchar(32) NOT NULL COMMENT '站点 code',
  `source_url` varchar(1024) NOT NULL DEFAULT '' COMMENT '原始 URL',
  `page_type` varchar(16) NOT NULL DEFAULT '' COMMENT 'LIST/DETAIL',
  `content_hash` varchar(64) NOT NULL DEFAULT '' COMMENT '内容 SHA-256',
  `content` longtext COMMENT '原始 HTML/JSON',
  `content_encoding` varchar(16) NOT NULL DEFAULT 'utf-8' COMMENT '编码',
  `parser_version` varchar(32) NOT NULL DEFAULT '' COMMENT '解析器版本',
  `parse_status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PARSED/FAILED',
  `fetched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抓取时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_url_time` (`source_url`(191),`fetched_at`),
  KEY `idx_hash` (`content_hash`),
  KEY `idx_parse_status` (`parse_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='原始页面快照';
DROP TABLE IF EXISTS `crawl_parse_rule`;
CREATE TABLE `crawl_parse_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `site_code` varchar(32) NOT NULL COMMENT '站点 code',
  `channel_path` varchar(255) NOT NULL DEFAULT '' COMMENT '频道，空表示站点级',
  `parser_version` varchar(32) NOT NULL COMMENT '版本号',
  `rule_type` varchar(32) NOT NULL COMMENT 'SELECTOR/REGEX/JSONPATH/CODE',
  `rule_content` json NOT NULL COMMENT '规则内容',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `effective_from` datetime DEFAULT NULL COMMENT '生效时间',
  `effective_to` datetime DEFAULT NULL COMMENT '失效时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_channel_version` (`site_code`,`channel_path`,`parser_version`,`rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='解析规则版本';
DROP TABLE IF EXISTS `crawl_merge_log`;
CREATE TABLE `crawl_merge_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_notice_id` bigint NOT NULL COMMENT '源公告 ID',
  `target_project_id` bigint DEFAULT NULL COMMENT '归并到的项目',
  `target_tender_project_id` bigint DEFAULT NULL COMMENT '归并到的招标项目',
  `target_lot_id` bigint DEFAULT NULL COMMENT '归并到的标段',
  `match_type` varchar(32) NOT NULL COMMENT 'UNIFIED_CODE/LOT_NO/PROJECT_NO_NOTICE_ID/SOURCE_URL/MANUAL',
  `match_score` decimal(5,2) DEFAULT NULL COMMENT '匹配分',
  `status` varchar(16) NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO/MANUAL/CONFLICT',
  `detail` json DEFAULT NULL COMMENT '匹配依据',
  `operated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_source_notice` (`source_notice_id`),
  KEY `idx_target_project` (`target_project_id`),
  KEY `idx_match_type` (`match_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='判重/归并日志';
-- ------------------------------------------------------------
-- 1.6 投标台账
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `bid_project`;
CREATE TABLE `bid_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '投标项目编号',
  `user_id` bigint NOT NULL COMMENT '发起用户(演示默认1,正式接会员)',
  `notice_id` bigint NOT NULL COMMENT '源公告编号(notice.id)',
  `project_name` varchar(512) NOT NULL COMMENT '项目名称(冗余自公告)',
  `deadline` datetime DEFAULT NULL COMMENT '投标截止时间(冗余自公告)',
  `bid_amount` decimal(18,2) DEFAULT NULL COMMENT '拟投标金额(万元)',
  `bid_file_name` varchar(256) NOT NULL DEFAULT '' COMMENT '投标文件名(演示文本)',
  `status` varchar(24) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED已递交/OTB待开标/WON中标/LOST未中标/ABANDONED放弃',
  `remark` varchar(512) NOT NULL DEFAULT '' COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_notice` (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投标项目';

-- =====================================================================
-- 2. 种子数据：基础数据 / 站点频道
--    （演示公告、企业、项目、标段见 demo-data.sql，可选）
-- =====================================================================
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (1,'110000','',1,'北京市','北京','北京市','beijing','province',NULL,NULL,_binary '',1,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (2,'120000','',1,'天津市','天津','天津市','tianjin','province',NULL,NULL,_binary '',2,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (3,'130000','',1,'河北省','河北','河北省','hebei','province',NULL,NULL,_binary '',3,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (4,'140000','',1,'山西省','山西','山西省','shanxi','province',NULL,NULL,_binary '',4,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (5,'150000','',1,'内蒙古自治区','内蒙古','内蒙古自治区','neimenggu','province',NULL,NULL,_binary '',5,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6,'210000','',1,'辽宁省','辽宁','辽宁省','liaoning','province',NULL,NULL,_binary '',6,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (7,'220000','',1,'吉林省','吉林','吉林省','jilin','province',NULL,NULL,_binary '',7,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (8,'230000','',1,'黑龙江省','黑龙江','黑龙江省','heilongjiang','province',NULL,NULL,_binary '',8,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (9,'310000','',1,'上海市','上海','上海市','shanghai','province',NULL,NULL,_binary '',9,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (10,'320000','',1,'江苏省','江苏','江苏省','jiangsu','province',NULL,NULL,_binary '',10,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (11,'330000','',1,'浙江省','浙江','浙江省','zhejiang','province',NULL,NULL,_binary '',11,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (12,'340000','',1,'安徽省','安徽','安徽省','anhui','province',NULL,NULL,_binary '',12,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (13,'350000','',1,'福建省','福建','福建省','fujian','province',NULL,NULL,_binary '',13,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (14,'360000','',1,'江西省','江西','江西省','jiangxi','province',NULL,NULL,_binary '',14,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (15,'370000','',1,'山东省','山东','山东省','shandong','province',NULL,NULL,_binary '',15,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (16,'410000','',1,'河南省','河南','河南省','henan','province',NULL,NULL,_binary '',16,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (17,'420000','',1,'湖北省','湖北','湖北省','hubei','province',NULL,NULL,_binary '',17,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (18,'430000','',1,'湖南省','湖南','湖南省','hunan','province',NULL,NULL,_binary '',18,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (19,'440000','',1,'广东省','广东','广东省','guangdong','province',NULL,NULL,_binary '',19,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (20,'450000','',1,'广西壮族自治区','广西','广西壮族自治区','guangxi','province',NULL,NULL,_binary '',20,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (21,'460000','',1,'海南省','海南','海南省','hainan','province',NULL,NULL,_binary '',21,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (22,'500000','',1,'重庆市','重庆','重庆市','chongqing','province',NULL,NULL,_binary '',22,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (23,'510000','',1,'四川省','四川','四川省','sichuan','province',NULL,NULL,_binary '',23,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (24,'520000','',1,'贵州省','贵州','贵州省','guizhou','province',NULL,NULL,_binary '',24,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (25,'530000','',1,'云南省','云南','云南省','yunnan','province',NULL,NULL,_binary '',25,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (26,'540000','',1,'西藏自治区','西藏','西藏自治区','xizang','province',NULL,NULL,_binary '',26,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (27,'610000','',1,'陕西省','陕西','陕西省','shaanxi','province',NULL,NULL,_binary '',27,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (28,'620000','',1,'甘肃省','甘肃','甘肃省','gansu','province',NULL,NULL,_binary '',28,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (29,'630000','',1,'青海省','青海','青海省','qinghai','province',NULL,NULL,_binary '',29,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (30,'640000','',1,'宁夏回族自治区','宁夏','宁夏回族自治区','ningxia','province',NULL,NULL,_binary '',30,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (31,'650000','',1,'新疆维吾尔自治区','新疆','新疆维吾尔自治区','xinjiang','province',NULL,NULL,_binary '',31,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (32,'710000','',1,'台湾省','台湾','台湾省','taiwan','province',NULL,NULL,_binary '',32,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (33,'810000','',1,'香港特别行政区','香港','香港特别行政区','xianggang','province',NULL,NULL,_binary '',33,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (34,'820000','',1,'澳门特别行政区','澳门','澳门特别行政区','aomen','province',NULL,NULL,_binary '',34,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (35,'420100','420000',2,'武汉市','武汉','武汉市','wuhan','city',NULL,NULL,_binary '',1,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (36,'440300','440000',2,'深圳市','深圳','深圳市','shenzhen','city',NULL,NULL,_binary '',2,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (37,'330100','330000',2,'杭州市','杭州','杭州市','hangzhou','city',NULL,NULL,_binary '',3,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (38,'411300','410000',2,'南阳市','南阳','南阳市','nanyang','city',NULL,NULL,_binary '',4,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (39,'370200','370000',2,'青岛市','青岛','青岛市','qingdao','city',NULL,NULL,_binary '',5,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (40,'430100','430000',2,'长沙市','长沙','长沙市','changsha','city',NULL,NULL,_binary '',6,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (41,'510100','510000',2,'成都市','成都','成都市','chengdu','city',NULL,NULL,_binary '',7,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (42,'610100','610000',2,'西安市','西安','西安市','xian','city',NULL,NULL,_binary '',8,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (43,'320500','320000',2,'苏州市','苏州','苏州市','suzhou','city',NULL,NULL,_binary '',9,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (44,'340100','340000',2,'合肥市','合肥','合肥市','hefei','city',NULL,NULL,_binary '',10,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (45,'630100','630000',2,'西宁市','西宁','西宁市','xining','city',NULL,NULL,_binary '',11,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (46,'350200','350000',2,'厦门市','厦门','厦门市','xiamen','city',NULL,NULL,_binary '',12,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (47,'360400','360000',2,'九江市','九江','九江市','jiujiang','city',NULL,NULL,_binary '',13,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (48,'450100','450000',2,'南宁市','南宁','南宁市','nanning','city',NULL,NULL,_binary '',14,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (49,'530100','530000',2,'昆明市','昆明','昆明市','kunming','city',NULL,NULL,_binary '',15,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (50,'150600','150000',2,'鄂尔多斯市','鄂尔多斯','鄂尔多斯市','eerduosi','city',NULL,NULL,_binary '',16,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_area` (`id`, `code`, `parent_code`, `level`, `name`, `short_name`, `full_name`, `pinyin`, `region_type`, `valid_from`, `valid_to`, `enabled`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (51,'520100','520000',2,'贵阳市','贵阳','贵阳市','guiyang','city',NULL,NULL,_binary '',17,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (1,'enterprise_role','TENDERER','招标人','',1,1,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (2,'enterprise_role','AGENCY','代理机构','',1,2,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (3,'enterprise_role','SUPPLIER','供应商','',1,3,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (4,'enterprise_role','WINNER','中标人','',1,4,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (5,'notice_type','tender','招标公告','',1,1,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6,'notice_type','win','中标结果','',1,2,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (7,'notice_type','change','变更公告','',1,3,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (8,'notice_type','explore','采购公告','',1,4,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (9,'notice_type','qualify','资格预审公告','',1,5,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (10,'notice_subtype','announcement','公告','',1,1,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (11,'notice_subtype','clarify','澄清/修改','',1,2,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (12,'notice_subtype','terminate','终止','',1,3,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (13,'notice_subtype','candidate','中标候选人公示','',1,4,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (14,'notice_subtype','result','中标结果','',1,5,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (15,'procurement_method','OPEN_BID','公开招标','',1,1,_binary '','代码值为占位，待按 V2.0 附录核对','','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (16,'procurement_method','INVITE_BID','邀请招标','',1,2,_binary '','代码值为占位，待按 V2.0 附录核对','','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (17,'procurement_method','COMPETITIVE_NEGOTIATION','竞争性谈判','',1,3,_binary '','代码值为占位，待按 V2.0 附录核对','','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (18,'procurement_method','COMPETITIVE_CONSULT','竞争性磋商','',1,4,_binary '','代码值为占位，待按 V2.0 附录核对','','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (19,'procurement_method','INQUIRY','询价','',1,5,_binary '','代码值为占位，待按 V2.0 附录核对','','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (20,'procurement_method','SINGLE_SOURCE','单一来源','',1,6,_binary '','代码值为占位，待按 V2.0 附录核对','','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (21,'procurement_method','OTHER','其他','',1,99,_binary '','代码值为占位，待按 V2.0 附录核对','','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (22,'funding_source','FISCAL','财政资金','',1,1,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (23,'funding_source','SELF_RAISED','自筹资金','',1,2,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (24,'funding_source','LOAN','贷款','',1,3,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (25,'funding_source','PPP','PPP','',1,4,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (26,'funding_source','OTHER','其他','',1,99,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (27,'qualification_level','SPECIAL','特级','',1,1,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (28,'qualification_level','FIRST','一级','',1,2,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (29,'qualification_level','SECOND','二级','',1,3,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (30,'qualification_level','THIRD','三级','',1,4,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (31,'evaluation_method','COMPREHENSIVE','综合评分法','',1,1,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (32,'evaluation_method','LOWEST_PRICE','最低价法','',1,2,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (33,'evaluation_method','EVALUATED_LOWEST','经评审最低价法','',1,3,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (34,'consortium_flag','YES','接受联合体','',1,1,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (35,'consortium_flag','NO','不接受联合体','',1,2,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (36,'file_category','TENDER_DOC','招标文件','',1,1,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (37,'file_category','BILL_OF_QUANTITIES','工程量清单','',1,2,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (38,'file_category','DRAWING','图纸','',1,3,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (39,'file_category','OTHER','其他','',1,99,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (40,'amount_unit','YUAN','元','',1,1,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (41,'amount_unit','WAN_YUAN','万元','',1,2,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (42,'amount_unit','YI_YUAN','亿元','',1,3,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (43,'push_channel','SITE','站内信','',1,1,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (44,'push_channel','EMAIL','邮件','',1,2,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (45,'push_channel','WECOM','企业微信','',1,3,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (46,'push_channel','FEISHU','飞书','',1,4,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (47,'push_channel','WECHAT_MP','微信服务号','',1,5,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (48,'push_channel','SMS','短信','',1,6,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (49,'trade_category','ENGINEERING','工程建设','',1,1,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (50,'trade_category','GOVERNMENT_PROCUREMENT','政府采购','',1,2,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (51,'trade_category','LAND','土地使用权','',1,3,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (52,'trade_category','PROPERTY','国有产权','',1,4,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `base_code` (`id`, `code_set`, `code`, `name`, `parent_code`, `level`, `sort`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (53,'trade_category','OTHER','其他','',1,99,_binary '',NULL,'','2026-11-04 12:29:16','','2026-11-04 12:29:16',_binary '\0');
INSERT INTO `crawler_site` (`id`, `name`, `code`, `enabled`, `channels`, `interval_ms`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `tier`, `schedule_cron`, `priority`, `region_scope`, `last_success_time`, `last_fail_time`, `health_status`, `adapter_version`) VALUES (1,'中国政府采购网','ccgp',_binary '','[{\"path\":\"/cggg/dfgg/gkzb/\",\"name\":\"地方-公开招标公告\",\"type\":\"tender\",\"pageCount\":3},{\"path\":\"/cggg/dfgg/zbgg/\",\"name\":\"地方-中标公告\",\"type\":\"win\",\"pageCount\":3},{\"path\":\"/cggg/dfgg/gzgg/\",\"name\":\"地方-更正公告\",\"type\":\"change\",\"pageCount\":2},{\"path\":\"/cggg/dfgg/cjgg/\",\"name\":\"地方-成交公告\",\"type\":\"win\",\"pageCount\":2},{\"path\":\"/cggg/dfgg/jzxcs/\",\"name\":\"地方-竞争性磋商公告\",\"type\":\"explore\",\"pageCount\":2},{\"path\":\"/cggg/dfgg/jzxtpgg/\",\"name\":\"地方-竞争性谈判公告\",\"type\":\"explore\",\"pageCount\":2},{\"path\":\"/cggg/dfgg/xjgg/\",\"name\":\"地方-询价公告\",\"type\":\"explore\",\"pageCount\":2},{\"path\":\"/cggg/dfgg/dylygg/\",\"name\":\"地方-单一来源公告和公示\",\"type\":\"explore\",\"pageCount\":2},{\"path\":\"/cggg/dfgg/fblbgg/\",\"name\":\"地方-终止公告\",\"type\":\"change\",\"pageCount\":2},{\"path\":\"/cggg/dfgg/yqzbgg/\",\"name\":\"地方-邀请招标公告\",\"type\":\"tender\",\"pageCount\":1},{\"path\":\"/cggg/dfgg/zgysgg/\",\"name\":\"地方-资格预审公告\",\"type\":\"tender\",\"pageCount\":1},{\"path\":\"/cggg/zygg/gkzb/\",\"name\":\"中央-公开招标公告\",\"type\":\"tender\",\"pageCount\":2,\"template\":\"zygg\"},{\"path\":\"/cggg/zygg/zbgg/\",\"name\":\"中央-中标公告\",\"type\":\"win\",\"pageCount\":2,\"template\":\"zygg\"},{\"path\":\"/cggg/zygg/gzgg/\",\"name\":\"中央-更正公告\",\"type\":\"change\",\"pageCount\":2,\"template\":\"zygg\"},{\"path\":\"/cggg/zygg/cjgg/\",\"name\":\"中央-成交公告\",\"type\":\"win\",\"pageCount\":2,\"template\":\"zygg\"},{\"path\":\"/cggg/zygg/jzxcs/\",\"name\":\"中央-竞争性磋商公告\",\"type\":\"explore\",\"pageCount\":2,\"template\":\"zygg\"},{\"path\":\"/cggg/zygg/jzxtpgg/\",\"name\":\"中央-竞争性谈判公告\",\"type\":\"explore\",\"pageCount\":2,\"template\":\"zygg\"},{\"path\":\"/cggg/zygg/xjgg/\",\"name\":\"中央-询价公告\",\"type\":\"explore\",\"pageCount\":2,\"template\":\"zygg\"},{\"path\":\"/cggg/zygg/fblbgg/\",\"name\":\"中央-终止公告\",\"type\":\"change\",\"pageCount\":2,\"template\":\"zygg\"},{\"path\":\"/cggg/zygg/dylygg/\",\"name\":\"中央-单一来源公告\",\"type\":\"explore\",\"pageCount\":1,\"template\":\"zygg\"},{\"path\":\"/cggg/zygg/yqzbgg/\",\"name\":\"中央-邀请招标公告\",\"type\":\"tender\",\"pageCount\":1,\"template\":\"zygg\"},{\"path\":\"/cggg/zygg/zgysgg/\",\"name\":\"中央-资格预审公告\",\"type\":\"tender\",\"pageCount\":1,\"template\":\"zygg\"}]',3000,'{\"baseUrl\":\"http://www.ccgp.gov.cn\",\"initialUrl\":\"http://www.ccgp.gov.cn/\",\"userAgent\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36\"}','','2026-11-04 12:29:16','','2026-11-04 12:29:18',_binary '\0',0,'official',NULL,10,'[\"CN\"]',NULL,NULL,'HEALTHY','v1');
INSERT INTO `crawler_site` (`id`, `name`, `code`, `enabled`, `channels`, `interval_ms`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `tier`, `schedule_cron`, `priority`, `region_scope`, `last_success_time`, `last_fail_time`, `health_status`, `adapter_version`) VALUES (2,'贵州省招标投标公共服务平台','ztb_gz',_binary '\0','[{\"path\":\"A01\",\"name\":\"招标公告\",\"type\":\"tender\",\"pageCount\":1},{\"path\":\"A02\",\"name\":\"变更公告\",\"type\":\"change\",\"pageCount\":1},{\"path\":\"A04\",\"name\":\"中标结果公示\",\"type\":\"win\",\"pageCount\":1}]',3000,'{\"baseUrl\":\"http://ztb.guizhou.gov.cn\",\"initialUrl\":\"http://ztb.guizhou.gov.cn/\",\"userAgent\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36\",\"xhrReferer\":\"http://ztb.guizhou.gov.cn/trade/?category=affiche\"}','','2026-11-04 12:29:16','','2026-11-04 12:29:18',_binary '\0',0,'official',NULL,30,'[\"520000\"]',NULL,NULL,'UNKNOWN','v1');
INSERT INTO `crawler_site` (`id`, `name`, `code`, `enabled`, `channels`, `interval_ms`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `tier`, `schedule_cron`, `priority`, `region_scope`, `last_success_time`, `last_fail_time`, `health_status`, `adapter_version`) VALUES (3,'全国公共资源交易平台','ggzy',_binary '','[{\"path\":\"01\",\"name\":\"工程建设\",\"type\":\"tender\",\"pageCount\":3},{\"path\":\"03\",\"name\":\"土地使用权\",\"type\":\"tender\",\"pageCount\":3},{\"path\":\"04\",\"name\":\"矿业权\",\"type\":\"tender\",\"pageCount\":1},{\"path\":\"05\",\"name\":\"国有产权\",\"type\":\"tender\",\"pageCount\":2},{\"path\":\"21\",\"name\":\"碳排放权\",\"type\":\"tender\",\"pageCount\":1},{\"path\":\"22\",\"name\":\"排污权\",\"type\":\"tender\",\"pageCount\":1},{\"path\":\"23\",\"name\":\"药品采购\",\"type\":\"tender\",\"pageCount\":1},{\"path\":\"24\",\"name\":\"二类疫苗\",\"type\":\"tender\",\"pageCount\":1},{\"path\":\"25\",\"name\":\"林权\",\"type\":\"tender\",\"pageCount\":1},{\"path\":\"90\",\"name\":\"其他\",\"type\":\"tender\",\"pageCount\":2}]',3000,'{\"baseUrl\":\"https://www.ggzy.gov.cn\",\"initialUrl\":\"https://www.ggzy.gov.cn/\",\"userAgent\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36\",\"dealTime\":\"01\"}','','2026-11-04 12:29:16','','2026-11-04 12:29:18',_binary '\0',0,'official',NULL,20,'[\"CN\"]',NULL,NULL,'HEALTHY','v1');
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1,'ccgp','/cggg/dfgg/gkzb/','地方-公开招标公告','tender',3,NULL,_binary '',1,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (2,'ccgp','/cggg/dfgg/zbgg/','地方-中标公告','win',3,NULL,_binary '',2,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (3,'ccgp','/cggg/dfgg/gzgg/','地方-更正公告','change',2,NULL,_binary '',3,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (4,'ccgp','/cggg/dfgg/cjgg/','地方-成交公告','win',2,NULL,_binary '',4,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (5,'ccgp','/cggg/dfgg/jzxcs/','地方-竞争性磋商公告','explore',2,NULL,_binary '',5,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (6,'ccgp','/cggg/dfgg/jzxtpgg/','地方-竞争性谈判公告','explore',2,NULL,_binary '',6,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (7,'ccgp','/cggg/dfgg/xjgg/','地方-询价公告','explore',2,NULL,_binary '',7,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (8,'ccgp','/cggg/dfgg/dylygg/','地方-单一来源公告和公示','explore',2,NULL,_binary '',8,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (9,'ccgp','/cggg/dfgg/fblbgg/','地方-终止公告','change',2,NULL,_binary '',9,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (10,'ccgp','/cggg/dfgg/yqzbgg/','地方-邀请招标公告','tender',1,NULL,_binary '',10,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (11,'ccgp','/cggg/dfgg/zgysgg/','地方-资格预审公告','tender',1,NULL,_binary '',11,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (12,'ccgp','/cggg/zygg/gkzb/','中央-公开招标公告','tender',2,NULL,_binary '',12,'{\"template\": \"zygg\"}','DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (13,'ccgp','/cggg/zygg/zbgg/','中央-中标公告','win',2,NULL,_binary '',13,'{\"template\": \"zygg\"}','DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (14,'ccgp','/cggg/zygg/gzgg/','中央-更正公告','change',2,NULL,_binary '',14,'{\"template\": \"zygg\"}','DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (15,'ccgp','/cggg/zygg/cjgg/','中央-成交公告','win',2,NULL,_binary '',15,'{\"template\": \"zygg\"}','DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (16,'ccgp','/cggg/zygg/jzxcs/','中央-竞争性磋商公告','explore',2,NULL,_binary '',16,'{\"template\": \"zygg\"}','DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (17,'ccgp','/cggg/zygg/jzxtpgg/','中央-竞争性谈判公告','explore',2,NULL,_binary '',17,'{\"template\": \"zygg\"}','DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (18,'ccgp','/cggg/zygg/xjgg/','中央-询价公告','explore',2,NULL,_binary '',18,'{\"template\": \"zygg\"}','DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (19,'ccgp','/cggg/zygg/fblbgg/','中央-终止公告','change',2,NULL,_binary '',19,'{\"template\": \"zygg\"}','DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (20,'ccgp','/cggg/zygg/dylygg/','中央-单一来源公告','explore',1,NULL,_binary '',20,'{\"template\": \"zygg\"}','DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (21,'ccgp','/cggg/zygg/yqzbgg/','中央-邀请招标公告','tender',1,NULL,_binary '',21,'{\"template\": \"zygg\"}','DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (22,'ccgp','/cggg/zygg/zgysgg/','中央-资格预审公告','tender',1,NULL,_binary '',22,'{\"template\": \"zygg\"}','DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (23,'ztb_gz','A01','招标公告','tender',1,NULL,_binary '\0',1,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (24,'ztb_gz','A02','变更公告','change',1,NULL,_binary '\0',2,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (25,'ztb_gz','A04','中标结果公示','win',1,NULL,_binary '\0',3,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (26,'ggzy','01','工程建设','tender',3,NULL,_binary '',1,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (27,'ggzy','03','土地使用权','tender',3,NULL,_binary '',2,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (28,'ggzy','04','矿业权','tender',1,NULL,_binary '',3,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (29,'ggzy','05','国有产权','tender',2,NULL,_binary '',4,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (30,'ggzy','21','碳排放权','tender',1,NULL,_binary '',5,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (31,'ggzy','22','排污权','tender',1,NULL,_binary '',6,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (32,'ggzy','23','药品采购','tender',1,NULL,_binary '',7,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (33,'ggzy','24','二类疫苗','tender',1,NULL,_binary '',8,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (34,'ggzy','25','林权','tender',1,NULL,_binary '',9,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);
INSERT INTO `crawler_channel` (`id`, `site_code`, `path`, `name`, `notice_type_code`, `page_count`, `schedule_cron`, `enabled`, `sort`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (35,'ggzy','90','其他','tender',2,NULL,_binary '',10,NULL,'DEMO-SEED','2026-11-04 12:29:18','DEMO-SEED','2026-11-04 12:29:18',_binary '\0',0);

-- =====================================================================
-- 3. 后台菜单种子
-- =====================================================================
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (12800,'数据采集中心','',2,30,0,'/crawler','ep:data-analysis','crawler/index','CrawlerCenter',0,_binary '',_binary '',_binary '','admin','2026-11-04 12:29:16','admin','2026-11-04 12:29:16',_binary '\0') ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `component_name`=VALUES(`component_name`);
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (12810,'智能站点发现','',2,41,0,'/discovery/agent','ep:search','discovery/agent/index','DiscoveryAgent',0,_binary '',_binary '',_binary '','admin','2026-11-04 12:29:16','admin','2026-11-04 12:29:16',_binary '\0') ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `component_name`=VALUES(`component_name`);
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (12811,'候选站点审核','',2,42,0,'/discovery/candidates','ep:list','discovery/candidates/index','DiscoveryCandidates',0,_binary '',_binary '',_binary '','admin','2026-11-04 12:29:16','admin','2026-11-04 12:29:16',_binary '\0') ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `component_name`=VALUES(`component_name`);
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (12820,'抓取日志与字段核对','',2,43,0,'/crawler/log','ep:document','crawler/log/index','CrawlerLog',0,_binary '',_binary '',_binary '','admin','2026-11-04 12:29:16','admin','2026-11-04 12:29:16',_binary '\0') ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `component_name`=VALUES(`component_name`);
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (12830,'检索效果评测','',2,44,0,'/ai-search/eval','ep:trend-charts','ai-search/eval/index','AiSearchEval',0,_binary '',_binary '',_binary '','admin','2026-11-04 12:29:16','admin','2026-11-04 12:29:16',_binary '\0') ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `component_name`=VALUES(`component_name`);
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (12840,'标签管理与打标','',2,45,0,'/tag/manage','ep:collection-tag','tag/manage/index','TagManage',0,_binary '',_binary '',_binary '','admin','2026-11-04 12:29:16','admin','2026-11-04 12:29:16',_binary '\0') ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `component_name`=VALUES(`component_name`);
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (12850,'公告管理','',2,46,0,'/notice/manage','ep:bell','notice/manage/index','NoticeManage',0,_binary '',_binary '',_binary '','admin','2026-11-04 12:29:16','admin','2026-11-04 12:29:16',_binary '\0') ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `component_name`=VALUES(`component_name`);
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (12860,'会员管理','',2,47,0,'/member/user','ep:user-filled','member/user/index','MemberUser',0,_binary '',_binary '',_binary '','admin','2026-11-04 12:29:16','admin','2026-11-04 12:29:16',_binary '\0') ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `component_name`=VALUES(`component_name`);

SET FOREIGN_KEY_CHECKS = 1;
