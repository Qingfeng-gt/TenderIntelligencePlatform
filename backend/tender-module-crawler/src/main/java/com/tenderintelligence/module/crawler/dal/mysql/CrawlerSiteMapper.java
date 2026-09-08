package com.tenderintelligence.module.crawler.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerSiteDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 爬虫站点配置 Mapper
 */
@Mapper
public interface CrawlerSiteMapper extends BaseMapperX<CrawlerSiteDO> {

    default CrawlerSiteDO selectByCode(String code) {
        return selectOne(CrawlerSiteDO::getCode, code);
    }
}
