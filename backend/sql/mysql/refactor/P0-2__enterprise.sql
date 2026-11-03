-- ============================================================
-- P0-2 企业主体域
-- 目标库: tender
-- 前置: P0-1__base_data.sql（企业角色代码引用 base_code）
-- 特性: 只增不改；可重复执行（CREATE TABLE IF NOT EXISTS）
-- 回滚: P0-2__enterprise_rollback.sql
-- 回填: P0-2__enterprise_backfill.sql
-- 设计: doc/数据/13-数据模型-目标设计.md §九
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1) 企业主体
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `enterprise` (
  `id`                        bigint       NOT NULL AUTO_INCREMENT COMMENT '企业 ID',
  `name`                      varchar(256) NOT NULL COMMENT '企业名称（源站原文）',
  `normalized_name`           varchar(256) NOT NULL DEFAULT '' COMMENT '归一化名称（去空格/全半角/括号）',
  `name_hash`                 varchar(64)  NOT NULL DEFAULT '' COMMENT '归一化名称 SHA-256',
  `unified_social_credit_code` varchar(64) DEFAULT NULL COMMENT '统一社会信用代码',
  `enterprise_type_code`      varchar(32)  NOT NULL DEFAULT '' COMMENT '企业类型代码',
  `legal_person`              varchar(64)  NOT NULL DEFAULT '' COMMENT '法定代表人',
  `registered_capital`        decimal(18,2) DEFAULT NULL COMMENT '注册资本（万元）',
  `established_date`          date         DEFAULT NULL COMMENT '成立日期',
  `address`                   varchar(512) NOT NULL DEFAULT '' COMMENT '企业地址',
  `region_code`               varchar(12)  NOT NULL DEFAULT '' COMMENT '行政区划代码',
  `province_code`             varchar(12)  NOT NULL DEFAULT '' COMMENT '省级代码',
  `city_code`                 varchar(12)  NOT NULL DEFAULT '' COMMENT '市级代码',
  `district_code`             varchar(12)  NOT NULL DEFAULT '' COMMENT '区县代码',
  `industry_code`             varchar(32)  NOT NULL DEFAULT '' COMMENT '行业代码',
  `contact_name`              varchar(128) NOT NULL DEFAULT '' COMMENT '默认联系人',
  `contact_phone`             varchar(255) NOT NULL DEFAULT '' COMMENT '默认联系电话（脱敏后展示）',
  `email`                     varchar(128) NOT NULL DEFAULT '' COMMENT '邮箱',
  `website`                   varchar(512) NOT NULL DEFAULT '' COMMENT '官网',
  `source_type`               varchar(32)  NOT NULL DEFAULT '' COMMENT 'NOTICE/MANUAL/IMPORT/NOTICE_BACKFILL',
  `source_url`                varchar(1024) NOT NULL DEFAULT '' COMMENT '首次来源 URL',
  `status`                    varchar(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/MERGED/INVALID',
  `merged_to_id`              bigint       DEFAULT NULL COMMENT '被合并到的企业 ID',
  `creator`                   varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`               datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                   varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`               datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                   bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`                 bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_credit_code` (`unified_social_credit_code`),
  KEY `idx_name_hash` (`name_hash`),
  KEY `idx_normalized_name` (`normalized_name`(191)),
  KEY `idx_name_prefix` (`name`(191)),
  KEY `idx_region` (`region_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业主体';

-- ------------------------------------------------------------
-- 2) 企业角色（一企多角色）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `enterprise_role` (
  `id`                   bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enterprise_id`        bigint      NOT NULL COMMENT '企业 ID',
  `role_code`            varchar(32) NOT NULL COMMENT 'TENDERER/AGENCY/SUPPLIER/WINNER',
  `first_seen_notice_id` bigint      DEFAULT NULL COMMENT '首次出现的公告 ID',
  `last_seen_notice_id`  bigint      DEFAULT NULL COMMENT '最近出现的公告 ID',
  `status`               varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  `creator`              varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`          datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`              varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`          datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`              bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`            bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enterprise_role` (`enterprise_id`, `role_code`),
  KEY `idx_role` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业角色';

-- ------------------------------------------------------------
-- 3) 企业联系人
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `enterprise_contact` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enterprise_id`     bigint       NOT NULL COMMENT '企业 ID',
  `name`              varchar(128) NOT NULL DEFAULT '' COMMENT '姓名',
  `phone`             varchar(255) NOT NULL DEFAULT '' COMMENT '电话（脱敏后展示）',
  `email`             varchar(128) NOT NULL DEFAULT '' COMMENT '邮箱',
  `title`             varchar(128) NOT NULL DEFAULT '' COMMENT '职务',
  `contact_type_code` varchar(32)  NOT NULL DEFAULT '' COMMENT '招标/中标/其他',
  `source_notice_id`  bigint       DEFAULT NULL COMMENT '来源公告 ID，便于溯源',
  `confidence`        decimal(5,2) DEFAULT NULL COMMENT '抽取置信度',
  `status`            varchar(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INVALID',
  `creator`           varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`         bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise` (`enterprise_id`),
  KEY `idx_phone` (`phone`(64))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业联系人';

-- ------------------------------------------------------------
-- 4) 企业别名
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `enterprise_alias` (
  `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enterprise_id`    bigint       NOT NULL COMMENT '企业 ID',
  `alias_name`       varchar(256) NOT NULL COMMENT '别名/历史名/简称',
  `normalized_alias` varchar(256) NOT NULL DEFAULT '' COMMENT '归一化别名',
  `alias_type`       varchar(32)  NOT NULL DEFAULT 'OTHER' COMMENT 'HISTORY/ABBR/OTHER',
  `source`           varchar(64)  NOT NULL DEFAULT '' COMMENT '来源',
  `creator`          varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`          varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`        bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enterprise_alias` (`enterprise_id`, `normalized_alias`(191)),
  KEY `idx_normalized_alias` (`normalized_alias`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业别名';

SET FOREIGN_KEY_CHECKS = 1;
