package com.tenderintelligence.module.crawler.service;

import com.tenderintelligence.framework.common.pojo.PageParam;
import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerSiteDO;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerTaskDO;

import java.util.List;

/**
 * 采集服务接口
 */
public interface CrawlService {

    /**
     * 执行一次采集(缺省 siteCode 则采集所有启用站点), 返回任务编号
     */
    Long run(String siteCode);

    /**
     * 查询站点列表
     */
    List<CrawlerSiteDO> getSiteList();

    /**
     * 查询采集任务日志(分页)
     */
    PageResult<CrawlerTaskDO> getTaskPage(PageParam pageParam, String siteCode);

    /**
     * 查询最近一次任务结果
     */
    CrawlerTaskDO getLatestTask(String siteCode);
}
