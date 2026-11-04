package com.tenderintelligence.module.crawler.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerFetchLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 逐 URL 抓取日志 Mapper
 */
@Mapper
public interface CrawlerFetchLogMapper extends BaseMapperX<CrawlerFetchLogDO> {
}
