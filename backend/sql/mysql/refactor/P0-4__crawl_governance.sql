-- ============================================================
-- P0-4 采集治理域
-- 目标库: tender
-- 前置: P0-1__base_data.sql；现有 crawler_site / crawler_task
-- 作用:
--   1) 新建 crawler_channel / crawler_fetch_log / crawl_raw / crawl_parse_rule /
--      crawl_merge_log / notice_quality
--   2) 扩展 crawler_site（分层/调度/健康度）与 crawler_task（触发方式/耗时/配置快照）
-- 特性: 只增不改；建表与扩展均幂等（扩展以哨兵列判断）
-- 回滚: P0-4__crawl_governance_rollback.sql
-- 回填: P0-4__crawler_channel_backfill.sql
-- 设计: doc/数据/13-数据模型-目标设计.md §七
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1) 频道配置（替代 crawler_site.channels JSON）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `crawler_channel` (
  `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `site_code`        varchar(32)  NOT NULL COMMENT '站点 code',
  `path`             varchar(255) NOT NULL COMMENT '频道路径或类别编码',
  `name`             varchar(128) NOT NULL DEFAULT '' COMMENT '频道名',
  `notice_type_code` varchar(32)  NOT NULL DEFAULT '' COMMENT '公告类型代码',
  `page_count`       int          NOT NULL DEFAULT 1 COMMENT '每轮最大翻页数',
  `schedule_cron`    varchar(64)  DEFAULT NULL COMMENT '频道级调度',
  `enabled`          bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `sort`             int          NOT NULL DEFAULT 0 COMMENT '排序',
  `config`           json         NULL COMMENT '频道级扩展配置',
  `creator`          varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`          varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`        bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_path_type` (`site_code`, `path`, `notice_type_code`),
  KEY `idx_site_enabled` (`site_code`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集频道配置';

-- ------------------------------------------------------------
-- 2) 逐 URL 抓取日志
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `crawler_fetch_log` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id`       bigint       NOT NULL COMMENT '任务 ID',
  `site_code`     varchar(32)  NOT NULL COMMENT '站点 code',
  `channel_path`  varchar(255) NOT NULL DEFAULT '' COMMENT '频道',
  `page_type`     varchar(16)  NOT NULL DEFAULT '' COMMENT 'LIST/DETAIL',
  `url`           varchar(1024) NOT NULL DEFAULT '' COMMENT '请求 URL',
  `http_status`   int          DEFAULT NULL COMMENT 'HTTP 状态',
  `duration_ms`   int          NOT NULL DEFAULT 0 COMMENT '耗时',
  `retry_count`   int          NOT NULL DEFAULT 0 COMMENT '重试次数',
  `status`        varchar(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'SUCCESS/FAILED/SKIPPED',
  `error_type`    varchar(32)  NOT NULL DEFAULT '' COMMENT '超时/403/解析失败等',
  `error_msg`     varchar(512) NOT NULL DEFAULT '' COMMENT '错误摘要',
  `fetched_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抓取时间',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`     bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_site_time` (`site_code`, `fetched_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逐 URL 抓取日志';

-- ------------------------------------------------------------
-- 3) 原始页面快照
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `crawl_raw` (
  `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id`          bigint       DEFAULT NULL COMMENT '任务 ID',
  `site_code`        varchar(32)  NOT NULL COMMENT '站点 code',
  `source_url`       varchar(1024) NOT NULL DEFAULT '' COMMENT '原始 URL',
  `page_type`        varchar(16)  NOT NULL DEFAULT '' COMMENT 'LIST/DETAIL',
  `content_hash`     varchar(64)  NOT NULL DEFAULT '' COMMENT '内容 SHA-256',
  `content`          longtext     NULL COMMENT '原始 HTML/JSON',
  `content_encoding` varchar(16)  NOT NULL DEFAULT 'utf-8' COMMENT '编码',
  `parser_version`   varchar(32)  NOT NULL DEFAULT '' COMMENT '解析器版本',
  `parse_status`     varchar(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PARSED/FAILED',
  `fetched_at`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抓取时间',
  `creator`          varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`          varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`        bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_url_time` (`source_url`(191), `fetched_at`),
  KEY `idx_hash` (`content_hash`),
  KEY `idx_parse_status` (`parse_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原始页面快照';

-- ------------------------------------------------------------
-- 4) 解析规则版本
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `crawl_parse_rule` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `site_code`      varchar(32)  NOT NULL COMMENT '站点 code',
  `channel_path`   varchar(255) NOT NULL DEFAULT '' COMMENT '频道，空表示站点级',
  `parser_version` varchar(32)  NOT NULL COMMENT '版本号',
  `rule_type`      varchar(32)  NOT NULL COMMENT 'SELECTOR/REGEX/JSONPATH/CODE',
  `rule_content`   json         NOT NULL COMMENT '规则内容',
  `enabled`        bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `effective_from` datetime     DEFAULT NULL COMMENT '生效时间',
  `effective_to`   datetime     DEFAULT NULL COMMENT '失效时间',
  `creator`        varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`      bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_channel_version` (`site_code`, `channel_path`, `parser_version`, `rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解析规则版本';

-- ------------------------------------------------------------
-- 5) 判重 / 归并日志
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `crawl_merge_log` (
  `id`                       bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_notice_id`         bigint       NOT NULL COMMENT '源公告 ID',
  `target_project_id`        bigint       DEFAULT NULL COMMENT '归并到的项目',
  `target_tender_project_id` bigint       DEFAULT NULL COMMENT '归并到的招标项目',
  `target_lot_id`            bigint       DEFAULT NULL COMMENT '归并到的标段',
  `match_type`               varchar(32)  NOT NULL COMMENT 'UNIFIED_CODE/LOT_NO/PROJECT_NO_NOTICE_ID/SOURCE_URL/MANUAL',
  `match_score`              decimal(5,2) DEFAULT NULL COMMENT '匹配分',
  `status`                   varchar(16)  NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO/MANUAL/CONFLICT',
  `detail`                   json         NULL COMMENT '匹配依据',
  `operated_at`              datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `creator`                  varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`              datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                  varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`              datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                  bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`                bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_source_notice` (`source_notice_id`),
  KEY `idx_target_project` (`target_project_id`),
  KEY `idx_match_type` (`match_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='判重/归并日志';

-- ------------------------------------------------------------
-- 6) 公告数据质量
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `notice_quality` (
  `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notice_id`      bigint        NOT NULL COMMENT '公告 ID',
  `issue_type`     varchar(32)   NOT NULL COMMENT 'MISSING_FIELD/PARSE_ERROR/INVALID_VALUE/DUPLICATE/OTHER',
  `field_name`     varchar(64)   NOT NULL DEFAULT '' COMMENT '字段名',
  `severity`       varchar(16)   NOT NULL DEFAULT 'WARN' COMMENT 'INFO/WARN/ERROR',
  `detail`         varchar(1000) NOT NULL DEFAULT '' COMMENT '问题描述',
  `source_url`     varchar(1024) NOT NULL DEFAULT '' COMMENT '来源 URL',
  `parser_version` varchar(32)   NOT NULL DEFAULT '' COMMENT '解析器版本',
  `status`         varchar(16)   NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/RESOLVED/IGNORED',
  `resolved_by`    varchar(64)   NOT NULL DEFAULT '' COMMENT '处理人',
  `resolved_time`  datetime      DEFAULT NULL COMMENT '处理时间',
  `creator`        varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`      bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_notice` (`notice_id`),
  KEY `idx_status_severity` (`status`, `severity`),
  KEY `idx_field` (`field_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告数据质量';

-- ------------------------------------------------------------
-- 7) 扩展 crawler_site（哨兵列 tier）
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS `p0_extend_crawler_site`;
DELIMITER $$
CREATE PROCEDURE `p0_extend_crawler_site`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crawler_site' AND COLUMN_NAME = 'tier'
  ) THEN
    ALTER TABLE `crawler_site`
      ADD COLUMN `tier` varchar(16) NOT NULL DEFAULT 'official' COMMENT 'official/industry/agency',
      ADD COLUMN `schedule_cron` varchar(64) DEFAULT NULL COMMENT '站点级调度表达式',
      ADD COLUMN `priority` int NOT NULL DEFAULT 100 COMMENT '优先级，越小越优先',
      ADD COLUMN `region_scope` varchar(500) NOT NULL DEFAULT '' COMMENT '覆盖区域代码 JSON',
      ADD COLUMN `last_success_time` datetime DEFAULT NULL COMMENT '最近成功时间',
      ADD COLUMN `last_fail_time` datetime DEFAULT NULL COMMENT '最近失败时间',
      ADD COLUMN `health_status` varchar(16) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN/HEALTHY/DEGRADED/DOWN',
      ADD COLUMN `adapter_version` varchar(32) NOT NULL DEFAULT '' COMMENT '适配器版本',
      ADD KEY `idx_tier_enabled` (`tier`, `enabled`);
  END IF;
END$$
DELIMITER ;
CALL `p0_extend_crawler_site`();
DROP PROCEDURE IF EXISTS `p0_extend_crawler_site`;

-- ------------------------------------------------------------
-- 8) 扩展 crawler_task（哨兵列 trigger_type）
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS `p0_extend_crawler_task`;
DELIMITER $$
CREATE PROCEDURE `p0_extend_crawler_task`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crawler_task' AND COLUMN_NAME = 'trigger_type'
  ) THEN
    ALTER TABLE `crawler_task`
      ADD COLUMN `trigger_type` varchar(16) NOT NULL DEFAULT 'SCHEDULE' COMMENT 'MANUAL/SCHEDULE/RETRY',
      ADD COLUMN `duration_ms` bigint NOT NULL DEFAULT 0 COMMENT '总耗时',
      ADD COLUMN `config_snapshot` json NULL COMMENT '本轮站点/频道配置快照',
      ADD COLUMN `error_type` varchar(32) NOT NULL DEFAULT '' COMMENT '异常分类',
      ADD COLUMN `data_date` date DEFAULT NULL COMMENT '数据日期',
      ADD KEY `idx_trigger_type_time` (`trigger_type`, `start_time`);
  END IF;
END$$
DELIMITER ;
CALL `p0_extend_crawler_task`();
DROP PROCEDURE IF EXISTS `p0_extend_crawler_task`;

SET FOREIGN_KEY_CHECKS = 1;
