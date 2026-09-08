package com.tenderintelligence.module.crawler.service;

import lombok.Data;

/**
 * 采集统计(适配器与任务日志之间的计数器)
 */
@Data
public class CrawlStats {

    /** 列表页抓取数 */
    private int listFetched;
    /** 列表解析出公告数 */
    private int listParsed;
    /** 详情页抓取成功数 */
    private int detailFetched;
    /** 详情页抓取失败数 */
    private int detailFailed;
    /** 新增入库数 */
    private int newInsert;
    /** 变更更新数 */
    private int updated;
}
