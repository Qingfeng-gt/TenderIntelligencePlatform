-- ============================================================
-- DEMO: 爬虫采集 + 用户招投标 相关表结构(设计见 doc/平台设计-数据采集与投标平台.md)
-- 适用于 MySQL 8,库: ruoyi-vue-pro-jdk8
-- ============================================================

-- 1) notice 扩展字段(爬虫入库需要): 去重键 source_url / 项目编号 / 时间节点
-- 注意: source_url 允许 NULL(MySQL 唯一索引允许多个 NULL, 兼容历史种子数据)
ALTER TABLE `notice`
  ADD COLUMN `project_no`  varchar(128) NOT NULL DEFAULT '' COMMENT '项目编号(源站)' AFTER `title`,
  ADD COLUMN `source_url`  varchar(512) DEFAULT NULL COMMENT '源站详情URL(唯一去重键)' AFTER `source`,
  ADD COLUMN `deadline`    datetime DEFAULT NULL COMMENT '投标截止时间' AFTER `publish_time`,
  ADD COLUMN `open_time`   datetime DEFAULT NULL COMMENT '开标时间' AFTER `deadline`,
  ADD COLUMN `region_code` varchar(16)  NOT NULL DEFAULT '' COMMENT '源站地区代码' AFTER `open_time`;

-- 历史种子数据不回填源站,置 NULL 以便建唯一键
UPDATE `notice` SET `source_url` = NULL WHERE `source_url` = '';
ALTER TABLE `notice` ADD UNIQUE KEY `uk_source_url` (`source_url`);

-- 2) 爬虫站点配置(适配器可扩展,新源站只需加一行配置+适配器)
CREATE TABLE IF NOT EXISTS `crawler_site` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '站点编号',
  `name`        varchar(64)  NOT NULL                  COMMENT '站点名称',
  `code`        varchar(32)  NOT NULL                  COMMENT '站点标识,对应 SourceAdapter 实现',
  `enabled`     bit(1)       NOT NULL DEFAULT b'1'     COMMENT '是否启用',
  `channels`    varchar(1024) NOT NULL DEFAULT ''      COMMENT '频道JSON:[{path,name,type,pageCount}]',
  `interval_ms` bigint       NOT NULL DEFAULT 3000     COMMENT '详情页请求间隔(ms,防反爬)',
  `config`      varchar(2048) NOT NULL DEFAULT ''      COMMENT '抓取配置JSON(UA/首访URL等)',
  `creator`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`   bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='爬虫站点配置';

-- 3) 采集任务日志
CREATE TABLE IF NOT EXISTS `crawler_task` (
  `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '任务编号',
  `site_code`      varchar(32) NOT NULL                 COMMENT '站点标识',
  `start_time`     datetime    NOT NULL                 COMMENT '开始时间',
  `end_time`       datetime    DEFAULT NULL             COMMENT '结束时间',
  `status`         varchar(16) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/SUCCESS/FAILED',
  `list_fetched`   int         NOT NULL DEFAULT 0 COMMENT '列表页抓取数',
  `list_parsed`    int         NOT NULL DEFAULT 0 COMMENT '列表解析出公告数',
  `detail_fetched` int         NOT NULL DEFAULT 0 COMMENT '详情页抓取成功数',
  `detail_failed`  int         NOT NULL DEFAULT 0 COMMENT '详情页抓取失败数',
  `new_insert`     int         NOT NULL DEFAULT 0 COMMENT '新增入库数',
  `updated`        int         NOT NULL DEFAULT 0 COMMENT '变更更新数',
  `error_msg`      varchar(512) NOT NULL DEFAULT '' COMMENT '失败原因',
  `creator`        varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`      bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_site_start` (`site_code`, `start_time`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='爬虫采集任务日志';

-- 4) 投标项目(用户招投标闭环,演示用户 user_id=1)
CREATE TABLE IF NOT EXISTS `bid_project` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '投标项目编号',
  `user_id`       bigint        NOT NULL                 COMMENT '发起用户(演示默认1,正式接会员)',
  `notice_id`     bigint        NOT NULL                 COMMENT '源公告编号(notice.id)',
  `project_name`  varchar(512)  NOT NULL                 COMMENT '项目名称(冗余自公告)',
  `deadline`      datetime      DEFAULT NULL             COMMENT '投标截止时间(冗余自公告)',
  `bid_amount`    decimal(18,2) DEFAULT NULL             COMMENT '拟投标金额(万元)',
  `bid_file_name` varchar(256)  NOT NULL DEFAULT ''      COMMENT '投标文件名(演示文本)',
  `status`        varchar(24)   NOT NULL DEFAULT 'SUBMITTED'
    COMMENT 'SUBMITTED已递交/OTB待开标/WON中标/LOST未中标/ABANDONED放弃',
  `remark`        varchar(512)  NOT NULL DEFAULT '' COMMENT '备注',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`     bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_notice` (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='投标项目';

-- 5) 默认爬虫源: 中国政府采购网(四个频道: 公开招标/中标/更正/询价)
INSERT INTO `crawler_site` (`id`, `name`, `code`, `enabled`, `channels`, `interval_ms`, `config`, `tenant_id`) VALUES
(1, '中国政府采购网', 'ccgp', b'1',
 CONCAT(
  '[{"path":"/cggg/dfgg/gkzb/","name":"地方-公开招标","type":"tender","pageCount":1},',
  '{"path":"/cggg/dfgg/zbgg/","name":"地方-中标公告","type":"win","pageCount":1},',
  '{"path":"/cggg/dfgg/gzgg/","name":"地方-更正公告","type":"change","pageCount":1},',
  '{"path":"/cggg/dfgg/xjgg/","name":"地方-询价公告","type":"explore","pageCount":1}]'),
 3000,
 '{"baseUrl":"http://www.ccgp.gov.cn","initialUrl":"http://www.ccgp.gov.cn/","userAgent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"}',
 0);
