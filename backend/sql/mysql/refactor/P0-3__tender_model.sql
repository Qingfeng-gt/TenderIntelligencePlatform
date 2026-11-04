-- ============================================================
-- P0-3 国标标讯域（四层模型）
-- 目标库: tender
-- 前置: P0-1__base_data.sql、P0-2__enterprise.sql
-- 作用:
--   1) 新建 project / tender_project / tender_lot / notice_change / notice_enterprise
--   2) 扩展 notice（新增国标字段；旧列在 P0-6 删除）
--   3) 扩展 notice_attachment（附件分类/售价/获取方式等）
-- 特性: 只增不改；建表与扩展均为幂等（扩展以哨兵列判断）
-- 回滚: P0-3__tender_model_rollback.sql
-- 回填: P0-3__notice_backfill.sql
-- 设计: doc/数据/13-数据模型-目标设计.md §八
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1) 项目数据集
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `project` (
  `id`                    bigint        NOT NULL AUTO_INCREMENT COMMENT '项目 ID',
  `project_key`           varchar(128)  NOT NULL COMMENT '本地归并键（规则生成，唯一）',
  `unified_deal_code`     varchar(64)   DEFAULT NULL COMMENT '统一交易标识码',
  `project_no`            varchar(128)  NOT NULL DEFAULT '' COMMENT '项目编号',
  `project_name`          varchar(512)  NOT NULL DEFAULT '' COMMENT '项目名称',
  `trade_category_code`   varchar(32)   NOT NULL DEFAULT '' COMMENT '交易分类代码',
  `project_category_code` varchar(32)   NOT NULL DEFAULT '' COMMENT '项目类别代码',
  `region_code`           varchar(12)   NOT NULL DEFAULT '' COMMENT '行政区划代码',
  `province_code`         varchar(12)   NOT NULL DEFAULT '' COMMENT '省级代码',
  `city_code`             varchar(12)   NOT NULL DEFAULT '' COMMENT '市级代码',
  `district_code`         varchar(12)   NOT NULL DEFAULT '' COMMENT '区县代码',
  `budget`                decimal(18,2) DEFAULT NULL COMMENT '预算（万元）',
  `funding_source_code`   varchar(32)   NOT NULL DEFAULT '' COMMENT '资金来源代码',
  `tenderer_enterprise_id` bigint       DEFAULT NULL COMMENT '招标人企业 ID',
  `agency_enterprise_id`  bigint        DEFAULT NULL COMMENT '代理机构企业 ID',
  `status`                varchar(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED/CANCELLED',
  `first_publish_time`    datetime      DEFAULT NULL COMMENT '首次公告时间',
  `last_publish_time`     datetime      DEFAULT NULL COMMENT '最近公告时间',
  `notice_count`          int           NOT NULL DEFAULT 0 COMMENT '公告数（冗余统计）',
  `lot_count`             int           NOT NULL DEFAULT 0 COMMENT '标段数（冗余统计）',
  `source_count`          int           NOT NULL DEFAULT 0 COMMENT '来源站点数（冗余统计）',
  `missing_fields`        json          NULL COMMENT '缺失字段汇总',
  `data_quality_status`   varchar(16)   NOT NULL DEFAULT 'UNKNOWN' COMMENT 'OK/PARTIAL/BROKEN/UNKNOWN',
  `creator`               varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`           datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`           datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`             bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_key` (`project_key`),
  UNIQUE KEY `uk_unified_deal_code` (`unified_deal_code`),
  KEY `idx_project_no` (`project_no`),
  KEY `idx_region_category` (`region_code`, `trade_category_code`),
  KEY `idx_last_publish` (`last_publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目数据集';

-- ------------------------------------------------------------
-- 2) 招标项目数据集
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tender_project` (
  `id`                      bigint        NOT NULL AUTO_INCREMENT COMMENT '招标项目 ID',
  `project_id`              bigint        NOT NULL COMMENT '项目 ID',
  `tender_project_no`       varchar(128)  DEFAULT NULL COMMENT '招标项目编号',
  `tender_project_name`     varchar(512)  NOT NULL DEFAULT '' COMMENT '招标项目名称',
  `procurement_method_code` varchar(32)   NOT NULL DEFAULT '' COMMENT '采购方式代码',
  `budget`                  decimal(18,2) DEFAULT NULL COMMENT '预算（万元）',
  `price_ceiling`           decimal(18,2) DEFAULT NULL COMMENT '最高限价（万元）',
  `funding_source_code`     varchar(32)   NOT NULL DEFAULT '' COMMENT '资金来源代码',
  `lot_count`               int           NOT NULL DEFAULT 0 COMMENT '标段数量',
  `contract_period_days`    int           DEFAULT NULL COMMENT '合同履行期限（天）',
  `contract_period_text`    varchar(128)  NOT NULL DEFAULT '' COMMENT '合同期限原文',
  `qualification_summary`   text          NULL COMMENT '资格条件摘要',
  `evaluation_method_code`  varchar(32)   NOT NULL DEFAULT '' COMMENT '评标办法代码',
  `tenderer_enterprise_id`  bigint        DEFAULT NULL COMMENT '招标人企业 ID',
  `agency_enterprise_id`    bigint        DEFAULT NULL COMMENT '代理机构企业 ID',
  `region_code`             varchar(12)   NOT NULL DEFAULT '' COMMENT '行政区划代码',
  `publish_time`            datetime      DEFAULT NULL COMMENT '发布时间',
  `status`                  varchar(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED/CANCELLED',
  `missing_fields`          json          NULL COMMENT '缺失字段汇总',
  `creator`                 varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`             datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                 varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`             datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                 bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`               bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_no` (`project_id`, `tender_project_no`),
  KEY `idx_project` (`project_id`),
  KEY `idx_region_publish` (`region_code`, `publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标项目数据集';

-- ------------------------------------------------------------
-- 3) 标段（包）数据集
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tender_lot` (
  `id`                        bigint        NOT NULL AUTO_INCREMENT COMMENT '标段 ID',
  `tender_project_id`         bigint        NOT NULL COMMENT '招标项目 ID',
  `project_id`                bigint        NOT NULL COMMENT '项目 ID（冗余）',
  `lot_no`                    varchar(128)  DEFAULT NULL COMMENT '标段（包）编号',
  `lot_name`                  varchar(512)  NOT NULL DEFAULT '' COMMENT '标段名称',
  `lot_content`               text          NULL COMMENT '标段内容/范围',
  `budget`                    decimal(18,2) DEFAULT NULL COMMENT '标段预算（万元）',
  `price_ceiling`             decimal(18,2) DEFAULT NULL COMMENT '最高限价（万元）',
  `deposit_amount`            decimal(18,2) DEFAULT NULL COMMENT '保证金（万元）',
  `qualification_requirement` text          NULL COMMENT '资格要求',
  `specific_qualification`    text          NULL COMMENT '特定资格',
  `consortium_allowed`        bit(1)        DEFAULT NULL COMMENT '是否接受联合体；NULL 未知',
  `performance_requirement`   text          NULL COMMENT '业绩要求',
  `qualification_level_code`  varchar(32)   NOT NULL DEFAULT '' COMMENT '资质等级代码',
  `evaluation_method_code`    varchar(32)   NOT NULL DEFAULT '' COMMENT '评标办法代码',
  `region_code`               varchar(12)   NOT NULL DEFAULT '' COMMENT '行政区划代码',
  `status`                    varchar(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED/CANCELLED',
  `missing_fields`            json          NULL COMMENT '缺失字段汇总',
  `creator`                   varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`               datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                   varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`               datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                   bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`                 bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tender_project_lot` (`tender_project_id`, `lot_no`),
  KEY `idx_project` (`project_id`),
  KEY `idx_region` (`region_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标段（包）数据集';

-- ------------------------------------------------------------
-- 4) 公告澄清修改
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `notice_change` (
  `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notice_id`        bigint       NOT NULL COMMENT '原公告 ID',
  `change_type_code` varchar(32)  NOT NULL DEFAULT '' COMMENT '澄清/修改/终止',
  `change_title`     varchar(512) NOT NULL DEFAULT '' COMMENT '变更标题',
  `change_content`   longtext     NULL COMMENT '变更内容',
  `change_time`      datetime     DEFAULT NULL COMMENT '变更时间',
  `source_url`       varchar(1024) NOT NULL DEFAULT '' COMMENT '变更公告 URL',
  `raw_id`           bigint       DEFAULT NULL COMMENT '原始快照 ID',
  `creator`          varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`          varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`        bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_notice` (`notice_id`),
  KEY `idx_change_time` (`change_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告澄清修改';

-- ------------------------------------------------------------
-- 5) 公告-企业关联
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `notice_enterprise` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notice_id`     bigint        NOT NULL COMMENT '公告 ID',
  `enterprise_id` bigint        NOT NULL COMMENT '企业 ID',
  `role_code`     varchar(32)   NOT NULL COMMENT 'TENDERER/AGENCY/WINNER/SUPPLIER',
  `source_text`   varchar(512)  NOT NULL DEFAULT '' COMMENT '公告中的原文',
  `confidence`    decimal(5,2)  DEFAULT NULL COMMENT '抽取置信度',
  `status`        varchar(16)   NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO/CONFIRMED/REJECTED',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`     bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_enterprise_role` (`notice_id`, `enterprise_id`, `role_code`),
  KEY `idx_enterprise_role` (`enterprise_id`, `role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告-企业关联';

-- ------------------------------------------------------------
-- 6) 扩展 notice（哨兵列 project_id）
--    说明：本脚本只加新列；旧列由 P0-6 在回填完成后删除。
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS `p0_extend_notice`;
DELIMITER $$
CREATE PROCEDURE `p0_extend_notice`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'project_id'
  ) THEN
    ALTER TABLE `notice`
      ADD COLUMN `project_id` bigint DEFAULT NULL COMMENT '项目 ID',
      ADD COLUMN `tender_project_id` bigint DEFAULT NULL COMMENT '招标项目 ID',
      ADD COLUMN `lot_id` bigint DEFAULT NULL COMMENT '标段 ID',
      ADD COLUMN `unified_deal_code` varchar(64) DEFAULT NULL COMMENT '统一交易标识码',
      ADD COLUMN `tender_project_no` varchar(128) NOT NULL DEFAULT '' COMMENT '招标项目编号',
      ADD COLUMN `lot_no` varchar(128) NOT NULL DEFAULT '' COMMENT '标段（包）编号',
      ADD COLUMN `notice_no` varchar(128) NOT NULL DEFAULT '' COMMENT '公告标识/编号',
      ADD COLUMN `notice_type_code` varchar(32) NOT NULL DEFAULT '' COMMENT '公告类型代码',
      ADD COLUMN `notice_subtype_code` varchar(32) NOT NULL DEFAULT '' COMMENT '公告子类型代码',
      ADD COLUMN `source_site_code` varchar(32) NOT NULL DEFAULT '' COMMENT '源站 code',
      ADD COLUMN `source_platform` varchar(128) NOT NULL DEFAULT '' COMMENT '来源平台名称',
      ADD COLUMN `file_get_start_time` datetime DEFAULT NULL COMMENT '文件获取开始',
      ADD COLUMN `file_get_end_time` datetime DEFAULT NULL COMMENT '文件获取截止',
      ADD COLUMN `qa_deadline` datetime DEFAULT NULL COMMENT '答疑/澄清截止',
      ADD COLUMN `sign_deadline` datetime DEFAULT NULL COMMENT '报名截止',
      ADD COLUMN `bid_deadline` datetime DEFAULT NULL COMMENT '投标截止',
      ADD COLUMN `open_place` varchar(512) NOT NULL DEFAULT '' COMMENT '开标地点',
      ADD COLUMN `publicity_start_time` datetime DEFAULT NULL COMMENT '公示开始',
      ADD COLUMN `publicity_end_time` datetime DEFAULT NULL COMMENT '公示结束',
      ADD COLUMN `price_ceiling` decimal(18,2) DEFAULT NULL COMMENT '最高限价（万元）',
      ADD COLUMN `deposit_amount` decimal(18,2) DEFAULT NULL COMMENT '保证金（万元）',
      ADD COLUMN `file_price` decimal(18,2) DEFAULT NULL COMMENT '文件售价（万元）',
      ADD COLUMN `win_amount` decimal(18,2) DEFAULT NULL COMMENT '中标金额（万元）',
      ADD COLUMN `procurement_method_code` varchar(32) NOT NULL DEFAULT '' COMMENT '采购方式代码',
      ADD COLUMN `funding_source_code` varchar(32) NOT NULL DEFAULT '' COMMENT '资金来源代码',
      ADD COLUMN `evaluation_method_code` varchar(32) NOT NULL DEFAULT '' COMMENT '评标办法代码',
      ADD COLUMN `qualification_requirement` text NULL COMMENT '资格要求',
      ADD COLUMN `specific_qualification` text NULL COMMENT '特定资格',
      ADD COLUMN `consortium_allowed` bit(1) DEFAULT NULL COMMENT '是否接受联合体；NULL 未知',
      ADD COLUMN `performance_requirement` text NULL COMMENT '业绩要求',
      ADD COLUMN `qualification_level_code` varchar(32) NOT NULL DEFAULT '' COMMENT '资质等级代码',
      ADD COLUMN `trade_category_code` varchar(32) NOT NULL DEFAULT '' COMMENT '交易分类代码',
      ADD COLUMN `province_code` varchar(12) NOT NULL DEFAULT '' COMMENT '省级代码',
      ADD COLUMN `city_code` varchar(12) NOT NULL DEFAULT '' COMMENT '市级代码',
      ADD COLUMN `district_code` varchar(12) NOT NULL DEFAULT '' COMMENT '区县代码',
      ADD COLUMN `tenderer_enterprise_id` bigint DEFAULT NULL COMMENT '招标人企业 ID',
      ADD COLUMN `agency_enterprise_id` bigint DEFAULT NULL COMMENT '代理机构企业 ID',
      ADD COLUMN `winner_enterprise_id` bigint DEFAULT NULL COMMENT '中标人企业 ID',
      ADD COLUMN `industry_code` varchar(32) NOT NULL DEFAULT '' COMMENT '行业代码',
      ADD COLUMN `has_attachment` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否有附件',
      ADD COLUMN `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览次数',
      ADD COLUMN `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/OFFLINE',
      ADD COLUMN `missing_fields` json NULL COMMENT '缺失字段汇总',
      ADD COLUMN `data_quality_status` varchar(16) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'OK/PARTIAL/BROKEN/UNKNOWN',
      ADD COLUMN `raw_id` bigint DEFAULT NULL COMMENT '原始快照 ID',
      ADD COLUMN `parser_version` varchar(32) NOT NULL DEFAULT '' COMMENT '解析器版本',
      ADD KEY `idx_notice_type_publish` (`notice_type_code`, `publish_time`),
      ADD KEY `idx_notice_project` (`project_id`),
      ADD KEY `idx_notice_tender_project` (`tender_project_id`),
      ADD KEY `idx_notice_lot` (`lot_id`),
      ADD KEY `idx_notice_region_publish` (`region_code`, `publish_time`),
      ADD KEY `idx_notice_unified_code` (`unified_deal_code`),
      ADD KEY `idx_notice_project_no` (`project_no`),
      ADD KEY `idx_notice_lot_no` (`lot_no`),
      ADD KEY `idx_notice_bid_deadline` (`bid_deadline`),
      ADD KEY `idx_notice_source_site_time` (`source_site_code`, `publish_time`);
  END IF;
END$$
DELIMITER ;
CALL `p0_extend_notice`();
DROP PROCEDURE IF EXISTS `p0_extend_notice`;

-- ------------------------------------------------------------
-- 7) 扩展 notice_attachment（哨兵列 file_category_code）
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS `p0_extend_notice_attachment`;
DELIMITER $$
CREATE PROCEDURE `p0_extend_notice_attachment`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice_attachment' AND COLUMN_NAME = 'file_category_code'
  ) THEN
    ALTER TABLE `notice_attachment`
      ADD COLUMN `file_category_code` varchar(32) NOT NULL DEFAULT '' COMMENT '招标文件/清单/图纸/其他',
      ADD COLUMN `file_price` decimal(18,2) DEFAULT NULL COMMENT '文件售价（万元）',
      ADD COLUMN `acquisition_method_code` varchar(32) NOT NULL DEFAULT '' COMMENT '线上/线下/邮寄',
      ADD COLUMN `source_attachment_id` varchar(128) NOT NULL DEFAULT '' COMMENT '源站附件标识',
      ADD COLUMN `file_hash` varchar(64) NOT NULL DEFAULT '' COMMENT '文件哈希（若平台落盘）',
      ADD COLUMN `local_file_id` bigint DEFAULT NULL COMMENT '平台落盘时关联 infra_file.id',
      ADD KEY `idx_attachment_category` (`file_category_code`);
  END IF;
END$$
DELIMITER ;
CALL `p0_extend_notice_attachment`();
DROP PROCEDURE IF EXISTS `p0_extend_notice_attachment`;

SET FOREIGN_KEY_CHECKS = 1;
