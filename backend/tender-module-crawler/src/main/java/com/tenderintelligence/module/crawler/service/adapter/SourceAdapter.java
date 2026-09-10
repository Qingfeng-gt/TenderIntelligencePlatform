package com.tenderintelligence.module.crawler.service.adapter;

import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerSiteDO;
import com.tenderintelligence.module.crawler.service.CrawlStats;

/**
 * 数据源适配器接口: 两段式采集(列表页 → 详情页) → 解析字段 → 入库
 *
 * 新源站实现本接口, 并在 crawler_site 表新增一行配置即可扩展
 */
public interface SourceAdapter {

    /**
     * 站点标识(与 crawler_site.code 对应)
     */
    String code();

    /**
     * 执行一次采集(列表→详情→解析→入库, 统计数据写入 stats)
     */
    void crawl(CrawlerSiteDO site, CrawlStats stats) throws Exception;
}
