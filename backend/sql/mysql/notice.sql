-- =====================================================================
-- 业务全量初始化(一次性):标讯 + 爬虫采集 + 用户投标
-- 执行方式: mysql -uroot -p tender < notice.sql   (须在 tender.sql 之后执行)
-- 模式: DROP + CREATE 全量重建(同 tender.sql),无增量/演进片段;仅用于初始化
-- 由原 notice.sql + demo-crawler-bid.sql + demo-crawler-bid-seed.sql + demo-crawler-admin.sql 合并
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 1) 招投标公告(标讯)表
--    扩展字段(project_no/source_url/deadline/open_time/region_code)为爬虫入库所需,
--    直接建表,注意: source_url 为唯一去重键,允许 NULL(MySQL 唯一索引允许多个 NULL)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '公告编号',
  `title` varchar(512) NOT NULL DEFAULT '' COMMENT '公告标题',
  `project_no` varchar(128) NOT NULL DEFAULT '' COMMENT '项目编号(源站)',
  `type` varchar(16) NOT NULL DEFAULT '' COMMENT '公告类型:tender 招标/win 中标/change 变更/explore 采购',
  `province` varchar(32) NOT NULL DEFAULT '' COMMENT '省份',
  `city` varchar(32) NOT NULL DEFAULT '' COMMENT '城市',
  `industry` varchar(64) NOT NULL DEFAULT '' COMMENT '行业',
  `budget` decimal(18,2) DEFAULT NULL COMMENT '预算金额(万元)',
  `tender_person` varchar(128) NOT NULL DEFAULT '' COMMENT '招标人/采购人',
  `agency` varchar(128) NOT NULL DEFAULT '' COMMENT '代理机构',
  `contact` varchar(128) NOT NULL DEFAULT '' COMMENT '联系人(源站为自由文本,可能是多人列表)',
  `contact_phone` varchar(255) NOT NULL DEFAULT '' COMMENT '联系电话(源站为自由文本,可能含多个号码与附注)',
  `content` longtext COMMENT '公告原文(HTML)',
  `source` varchar(128) NOT NULL DEFAULT '' COMMENT '来源网站',
  `source_url` varchar(512) DEFAULT NULL COMMENT '源站详情URL(唯一去重键)',
  `publish_time` datetime NOT NULL COMMENT '发布时间',
  `deadline` datetime DEFAULT NULL COMMENT '投标截止时间',
  `open_time` datetime DEFAULT NULL COMMENT '开标时间',
  `region_code` varchar(16) NOT NULL DEFAULT '' COMMENT '源站地区代码',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_url` (`source_url`),
  KEY `idx_type_publish` (`type`, `publish_time`),
  KEY `idx_province` (`province`),
  KEY `idx_industry` (`industry`),
  KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='招投标公告';

-- 种子数据(与 user-web mock 一致,供开发联调;正式数据由爬虫写入)
INSERT INTO `notice` (`id`, `title`, `type`, `province`, `city`, `industry`, `budget`, `tender_person`, `agency`, `contact`, `contact_phone`, `content`, `source`, `publish_time`, `creator`, `updater`, `deleted`, `tenant_id`) VALUES
(1, '2026 年市政道路提升改造工程设计施工总承包项目招标公告', 'tender', '湖北', '武汉市', '市政工程', 12500.00, '武汉市市政建设集团有限公司', '湖北中天招标咨询有限公司', '张工', '027-87654321', '<h3>一、招标条件</h3><p>本项目已由 <strong>武汉市发展和改革委员会</strong> 批准建设,项目资金来源为 <strong>政府投资</strong>,招标人为武汉市市政建设集团有限公司。</p><h3>二、项目概况与招标范围</h3><p>1. 项目名称:2026 年市政道路提升改造工程<br/>2. 招标编号:HBZT-2026-0917<br/>3. 建设地点:武汉市江汉区、武昌区<br/>4. 建设规模:道路改造里程约 8.6 公里,含排水管网提升、人行道铺装、路灯亮化<br/>5. 计划工期:180 日历天<br/>6. 招标范围:设计施工总承包(EPC)</p>', '中国招标投标公共服务平台', '2026-09-07 09:30:00', '', '', b'0', 0),
(2, '武汉市轨道交通 12 号线一期工程土建施工 3 标段中标候选人公示', 'win', '湖北', '武汉市', '轨道交通', 38600.00, '武汉地铁集团有限公司', '湖北省成套招标股份有限公司', '李经理', '027-88776655', '<h3>中标候选人公示</h3><p>经评标委员会评审,<strong>中铁十一局集团有限公司</strong> 为本项目第一中标候选人,投标报价 38600 万元,评标得分 96.32 分。</p><p>第二中标候选人:中铁六局集团有限公司,报价 38920 万元;<br/>第三中标候选人:中铁二十三局集团有限公司,报价 39100 万元。</p>', '武汉市公共资源交易网', '2026-09-07 08:50:00', '', '', b'0', 0),
(3, '某市人民医院智慧医疗信息化建设项目招标文件变更公告', 'change', '广东', '深圳市', '医疗卫生', 2600.00, '深圳市人民医院', '广东明正项目管理有限公司', '赵女士', '0755-26551133', '<h3>变更内容</h3><p>1. 原招标公告中"投标截止时间"变更为 <strong>2026 年 9 月 22 日 14:00</strong>;<br/>2. "完成经营库容档案"评分项分值由 5 分调整为 8 分;<br/>3. 开标地点不变。</p>', '中国政府采购网', '2026-09-07 08:10:00', '', '', b'0', 0),
(4, '中国移动某省公司 2026-2028 年通信工程建设项目集中采购招标公告', 'tender', '浙江', '杭州市', '通信工程', 98000.00, '中国移动通信集团浙江有限公司', '中通服咨询设计研究院有限公司', '王工', '0571-86661122', '<h3>项目概况</h3><p>本次集中采购涉及全省 11 个地市的通信管道施工、传输线路建设及机房配套改造,分为 5 个标包,投标人可兼投但不兼得。</p>', '中国招标投标公共服务平台', '2026-09-06 17:20:00', '', '', b'0', 0),
(5, '某县 2026 年高标准农田建设项目(第三批)施工标段招标公告', 'tender', '河南', '南阳市', '农业水利', 4300.00, '南阳市农业农村局', '河南豫信招标有限责任公司', '刘工', '0377-63332255', '<h3>招标范围</h3><p>项目区土地平整 5200 亩,新建灌溉机井 86 眼,配套输水管道 42 公里,田间道路 18 公里。工期 120 日历天。</p>', '河南省公共资源交易平台', '2026-09-06 16:40:00', '', '', b'0', 0),
(6, '青岛市地铁 9 号线一期工程机电安装标段中标结果公告', 'win', '山东', '青岛市', '轨道交通', 21500.00, '青岛地铁集团有限公司', '青岛利安建设咨询有限公司', '孙总', '0532-80992211', '<p>中标人:<strong>中建安装集团有限公司</strong>,中标金额 21478 万元,工期 550 日历天。</p>', '山东省公共资源交易网', '2026-09-06 15:05:00', '', '', b'0', 0),
(7, '某国防科技大学光电实验平台设备购置项目采购公告', 'explore', '湖南', '长沙市', '软件服务', 980.00, '某高校', '湖南中联招标代理有限公司', '陈老师', '0731-84551122', '<h3>采购内容</h3><p>高精度激光干涉仪 2 套、光电成像系统 1 套、高速采集卡 8 块。预算 980 万元。</p>', '中国政府采购网', '2026-09-06 11:00:00', '', '', b'0', 0),
(8, '某省高速公路改扩建工程特许经营社会资本方招标公告', 'tender', '四川', '成都市', '交通公路', 520000.00, '四川省交通运输厅', '四川公路桥梁建设咨询公司', '周工', '028-85223377', '<h3>项目概况</h3><p>本项目采用"特许经营 + 政府付费"模式,全长 96 公里,双向八车道,建设期 4 年,经营期 25 年。</p>', '四川省公共资源交易平台', '2026-09-05 18:30:00', '', '', b'0', 0),
(9, '西部某某大数据产业园数据中心(一期)机电配套工程招标公告', 'tender', '陕西', '西安市', '软件服务', 18600.00, '陕西秦云数据产业发展有限公司', '西部项目管理咨询有限公司', '高工', '029-85225511', '<h3>招标范围</h3><p>数据中心 1 号楼与 2 号楼的机电安装工程,含变配电、UPS、冷水机组、精密空调、动环监控系统安装调试(3000 机柜规模)。</p>', '陕西采购与招标网', '2026-09-05 15:45:00', '', '', b'0', 0),
(10, '某河道水环境综合治理项目 EPC 总承包中标公示', 'win', '江苏', '苏州市', '生态环保', 7600.00, '苏州市水务局', '江苏苏咨工程咨询有限公司', '钱工', '0512-68771122', '<p>中标人:<strong>中国电建集团华东勘测设计研究院有限公司</strong>(牵头人)与江苏苏建建设工程有限公司联合体,中标金额 7421 万元。</p>', '江苏省公共资源交易网', '2026-09-05 10:15:00', '', '', b'0', 0),
(11, '某职业技术学院新校区二期建设工程总承包招标公告', 'tender', '安徽', '合肥市', '建筑工程', 32000.00, '合肥职业技术学院', '安徽安天利信工程管理股份有限公司', '吴老师', '0551-63791122', '<h3>建设规模</h3><p>新校区二期总建筑面积 9.2 万平方米,含综合实训楼、学生宿舍、风雨操场、地下车库及室外配套工程。</p>', '合肥公共资源交易网', '2026-09-04 16:20:00', '', '', b'0', 0),
(12, '某光伏电站 100MW 项目逆变器设备采购变更公告', 'change', '青海', '西宁市', '新能源', 2800.00, '青海绿能新能源有限公司', '青海电力招标咨询有限公司', '马经理', '0971-6335123', '<h3>变更内容</h3><p>逆变器技术要求变更:组串式逆变器单机功率由 ≤176kW 调整为 ≤220kW,容配化比例 1.2:1。投标文件递交截止时间顺延至 2026 年 9 月 18 日。</p>', '青海省公共资源交易网', '2026-09-04 14:00:00', '', '', b'0', 0),
(13, '某市公安局智慧安防小区建设项目中标结果公告', 'win', '北京', '北京', '公共安全', 5600.00, '北京市公安局', '北京国际招标有限公司', '崔主任', '010-65518322', '<p>中标人:<strong>北京电通安防科技有限公司</strong>,中标金额 5432 万元,供货期 90 天。</p>', '中国政府采购网', '2026-09-04 09:30:00', '', '', b'0', 0),
(14, '某自贸区 2026 年新建排水管网及窨井改造工程施工招标', 'tender', '福建', '厦门市', '市政工程', 6800.00, '厦门城建集团', '福建建融工程咨询有限公司', '林工', '0592-2660112', '<h3>项目概况</h3><p>新建 DN400-DN1200 雨水管 12.6 公里,改造老旧窨井 480 座,配套泵站 2 座。工期 240 日历天。</p>', '厦门市公共资源交易网', '2026-09-03 17:00:00', '', '', b'0', 0),
(15, '某银行软件开发中心外部采购——核心系统数字化能力提升项目招标公告', 'tender', '上海', '上海', '软件服务', 1500.00, '某股份制银行上海软件开发中心', '上海浦成建设招标咨询有限公司', '朱经理', '021-58881234', '<h3>服务内容</h3><p>核心系统微服务化改造咨询、分布式数据库迁移实施、自动化测试平台建设,服务期 12 个月。</p>', '上海政府采购网', '2026-09-03 13:40:00', '', '', b'0', 0),
(16, '某县城乡供水一体化项目(一期)设备采购中标候选人公示', 'win', '江西', '九江市', '农业水利', 3900.00, '九江市水利局', '江西明正招标咨询有限公司', '桂工', '0792-8225117', '<p>第一中标候选人:<strong>上海熊猫机械(集团)有限公司</strong>,投标报价 3715 万元。</p>', '江西省公共资源交易网', '2026-09-03 10:20:00', '', '', b'0', 0),
(17, '某学校食堂大宗食材(米面油等)供应商采购公告', 'explore', '广西', '南宁市', '教育', 620.00, '南宁市第二中学', '广西嘉华工程咨询有限公司', '覃老师', '0771-3120233', '<h3>采购内容</h3><p>大米、食用油、面粉、调味品一批,服务期一年(2026 年 10 月—2027 年 9 月)。</p>', '广西壮族自治区政府采购网', '2026-09-02 16:30:00', '', '', b'0', 0),
(18, '某国际机场 T3 航站楼改扩建工程地质勘察服务招标公告', 'tender', '云南', '昆明市', '咨询服务', 1500.00, '昆明长水国际机场', '云南昇腾招标有限公司', '洪工程师', '0871-67121199', '<h3>项目概况</h3><p>T3 航站楼改扩建勘察钻孔约 420 个,需提交岩土工程勘察报告、地基地震评价与沉降分析。</p>', '云南省公共资源交易网', '2026-09-02 11:15:00', '', '', b'0', 0),
(19, '某天然气管道工程一标段钢管采购中标结果公告', 'win', '内蒙古', '鄂尔多斯市', '能源化工', 12800.00, '鄂尔多斯市中浩能源有限公司', '内蒙古蒙正招标有限公司', '敖先生', '0477-3890002', '<p>中标人:<strong>宝鸡石油钢管有限责任公司</strong>,中标金额 12736 万元。</p>', '内蒙古公共资源交易网', '2026-09-02 09:05:00', '', '', b'0', 0),
(20, '某省级政务云平台扩容升级项目公开招标公告', 'tender', '贵州', '贵阳市', '软件服务', 8600.00, '贵州省大数据发展管理局', '贵州聚力项目管理有限公司', '陈老师', '0851-86993122', '<h3>采购内容</h3><p>政务云平台计算节点扩容 2000 核、存储扩容 2PB,政务数据治理服务及安全防护提升,服务期 24 个月。</p>', '贵州省政府采购网', '2026-09-01 14:50:00', '', '', b'0', 0);

-- ---------------------------------------------------------------------
-- 2) 标讯附件表(一条公告可有多个附件)
--    只存源站附件直链与元信息,不存文件本体 —— 附件由用户在源站下载,
--    平台不做文件的二次分发。附件在详情页正文区段内解析,见 CcgpAttachmentParser。
--    去重策略: 无唯一键,由 NoticeUpsertService 在公告变更时「先删后写」整组重写,
--    因此源站删掉的附件不会残留(单站点采集串行,不存在并发写同一公告)。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `notice_attachment`;
CREATE TABLE `notice_attachment` (
  `id`         bigint        NOT NULL AUTO_INCREMENT COMMENT '附件编号',
  `notice_id`  bigint        NOT NULL COMMENT '公告编号(notice.id)',
  `file_name`  varchar(512)  NOT NULL DEFAULT '' COMMENT '附件文件名(源站锚文本)',
  `file_url`   varchar(1024) NOT NULL DEFAULT '' COMMENT '源站附件直链(绝对URL)',
  `file_type`  varchar(16)   NOT NULL DEFAULT '' COMMENT '文件类型(pdf/doc/docx/xls/xlsx/zip 等)',
  `file_size`  varchar(32)   NOT NULL DEFAULT '' COMMENT '源站标注的文件大小(自由文本,如 546.3K;未标注为空)',
  `sort`       int           NOT NULL DEFAULT 0 COMMENT '同一公告内的展示顺序(按源站出现次序)',
  `creator`    varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`    varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`    bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`  bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_notice` (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='标讯附件';

-- ---------------------------------------------------------------------
-- 3) 爬虫站点配置(适配器可扩展,新源站只需加一行配置+适配器)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `crawler_site`;
CREATE TABLE `crawler_site` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '站点编号',
  `name`        varchar(64)  NOT NULL                  COMMENT '站点名称',
  `code`        varchar(32)  NOT NULL                  COMMENT '站点标识,对应 SourceAdapter 实现',
  `enabled`     bit(1)       NOT NULL DEFAULT b'1'     COMMENT '是否启用',
  `channels`    varchar(4096) NOT NULL DEFAULT ''      COMMENT '频道JSON:[{path,name,type,pageCount}]',
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

-- ---------------------------------------------------------------------
-- 4) 采集任务日志
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `crawler_task`;
CREATE TABLE `crawler_task` (
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

-- ---------------------------------------------------------------------
-- 5) 投标项目(用户招投标闭环,演示用户 user_id=1)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `bid_project`;
CREATE TABLE `bid_project` (
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

-- ---------------------------------------------------------------------
-- 6) 爬虫站点种子
-- ---------------------------------------------------------------------
-- 中国政府采购网(22 个频道: cggg/dfgg 地方公告 11 个 + cggg/zygg 中央公告 11 个)
--
-- 2026-09-11 实测结论(决定了本配置的频道口径):
--   1) dfgg(地方公告)本身就是"全国省级公告聚合流" —— 单页 20 条混排各省(陕西/江苏/新疆兵团/广西/上海…),
--      不是按省分列。因此"逐省接入 31 个省级政府采购网"在数据覆盖上是冗余的, 无需 31 个适配器。
--   2) zygg(中央公告)与 dfgg 结构、详情页模板完全一致, 此前一个频道都没采(中央级采购为空白), 本轮补齐。
--   3) 两分组下各 11 个频道全部可用, 列表页每页 20 条, 分页 index_N.htm 有效。
--   4) 详情页解析锚点与既有 CcgpNoticeParser 完全吻合, 适配器无需改动。
--
-- pageCount 按各频道实测产量分档(每页 20 条), 目标: 单档至少覆盖 2 小时间隔的 1.5 倍以上:
--   高产(dfgg 公开招标/中标)    3 页 —— 实测 100 条不足 1 天, 单页仅够 2 小时, 需留余量
--   中产(其余在采频道)          2 页 —— 实测 5 页跨 1~2 天
--   休眠(channel 名称含 yqzbgg/zgysgg 等) 1 页 —— 实测 5 页跨数月甚至跨年, 多抓纯属重复请求
-- 合计 41 页 = 820 条候选/轮, 限速 3s ≈ 41 分钟, 落在 2 小时调度窗口内(占用约 34%)。
-- 说明: 候选 URL 每轮全量重抓详情(既有语义, 用于发现已入库公告的字段变更), 未做增量跳过。
INSERT INTO `crawler_site` (`id`, `name`, `code`, `enabled`, `channels`, `interval_ms`, `config`, `tenant_id`) VALUES
(1, '中国政府采购网', 'ccgp', b'1',
 '[{"path":"/cggg/dfgg/gkzb/","name":"地方-公开招标公告","type":"tender","pageCount":3},{"path":"/cggg/dfgg/zbgg/","name":"地方-中标公告","type":"win","pageCount":3},{"path":"/cggg/dfgg/gzgg/","name":"地方-更正公告","type":"change","pageCount":2},{"path":"/cggg/dfgg/cjgg/","name":"地方-成交公告","type":"win","pageCount":2},{"path":"/cggg/dfgg/jzxcs/","name":"地方-竞争性磋商公告","type":"explore","pageCount":2},{"path":"/cggg/dfgg/jzxtpgg/","name":"地方-竞争性谈判公告","type":"explore","pageCount":2},{"path":"/cggg/dfgg/xjgg/","name":"地方-询价公告","type":"explore","pageCount":2},{"path":"/cggg/dfgg/dylygg/","name":"地方-单一来源公告和公示","type":"explore","pageCount":2},{"path":"/cggg/dfgg/fblbgg/","name":"地方-终止公告","type":"change","pageCount":2},{"path":"/cggg/dfgg/yqzbgg/","name":"地方-邀请招标公告","type":"tender","pageCount":1},{"path":"/cggg/dfgg/zgysgg/","name":"地方-资格预审公告","type":"tender","pageCount":1},{"path":"/cggg/zygg/gkzb/","name":"中央-公开招标公告","type":"tender","pageCount":2,"template":"zygg"},{"path":"/cggg/zygg/zbgg/","name":"中央-中标公告","type":"win","pageCount":2,"template":"zygg"},{"path":"/cggg/zygg/gzgg/","name":"中央-更正公告","type":"change","pageCount":2,"template":"zygg"},{"path":"/cggg/zygg/cjgg/","name":"中央-成交公告","type":"win","pageCount":2,"template":"zygg"},{"path":"/cggg/zygg/jzxcs/","name":"中央-竞争性磋商公告","type":"explore","pageCount":2,"template":"zygg"},{"path":"/cggg/zygg/jzxtpgg/","name":"中央-竞争性谈判公告","type":"explore","pageCount":2,"template":"zygg"},{"path":"/cggg/zygg/xjgg/","name":"中央-询价公告","type":"explore","pageCount":2,"template":"zygg"},{"path":"/cggg/zygg/fblbgg/","name":"中央-终止公告","type":"change","pageCount":2,"template":"zygg"},{"path":"/cggg/zygg/dylygg/","name":"中央-单一来源公告","type":"explore","pageCount":1,"template":"zygg"},{"path":"/cggg/zygg/yqzbgg/","name":"中央-邀请招标公告","type":"tender","pageCount":1,"template":"zygg"},{"path":"/cggg/zygg/zgysgg/","name":"中央-资格预审公告","type":"tender","pageCount":1,"template":"zygg"}]',
 3000,
 '{"baseUrl":"http://www.ccgp.gov.cn","initialUrl":"http://www.ccgp.gov.cn/","userAgent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"}',
 0);

-- 贵州省招标投标公共服务平台(JSON API 源站; channels[].path = search 的 noticeType 类别编码)
-- 2026-09-08 实测: 该站 search 列表接口当前返回 0 条(列表数据下线), GetDetail 可用, 源站恢复后即自动采到数据
INSERT INTO `crawler_site` (`id`, `name`, `code`, `enabled`, `channels`, `interval_ms`, `config`, `tenant_id`) VALUES
(2, '贵州省招标投标公共服务平台', 'ztb_gz', b'1',
 '[{"path":"A01","name":"招标公告","type":"tender","pageCount":1},{"path":"A02","name":"变更公告","type":"change","pageCount":1},{"path":"A04","name":"中标结果公示","type":"win","pageCount":1}]',
 3000,
 '{"baseUrl":"http://ztb.guizhou.gov.cn","initialUrl":"http://ztb.guizhou.gov.cn/","userAgent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36","xhrReferer":"http://ztb.guizhou.gov.cn/trade/?category=affiche"}',
 0);

-- ---------------------------------------------------------------------
-- 7) 后台「数据采集中心」菜单(超级管理员自动拥有全部菜单,无需 system_role_menu 记录)
-- ---------------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(12800, '数据采集中心', '', 2, 30, 0, '/crawler', 'ep:data-analysis', 'crawler/index', 'CrawlerCenter', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`);

SET FOREIGN_KEY_CHECKS = 1;
