package com.tenderintelligence.module.crawler.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerChannelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 采集频道配置 Mapper
 */
@Mapper
public interface CrawlerChannelMapper extends BaseMapperX<CrawlerChannelDO> {

    default List<CrawlerChannelDO> selectEnabledBySiteCode(String siteCode) {
        return selectList(new LambdaQueryWrapperX<CrawlerChannelDO>()
                .eq(CrawlerChannelDO::getSiteCode, siteCode)
                .eq(CrawlerChannelDO::getEnabled, true)
                .orderByAsc(CrawlerChannelDO::getSort)
                .orderByAsc(CrawlerChannelDO::getId));
    }
}
