package com.tenderintelligence.module.crawler.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlRawDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原始页面快照 Mapper
 */
@Mapper
public interface CrawlRawMapper extends BaseMapperX<CrawlRawDO> {

    /**
     * 同一 URL 的同一内容是否已存过快照（避免每轮重采重复堆积原始页面）
     */
    default boolean existsByUrlAndHash(String url, String contentHash) {
        return selectCount(new LambdaQueryWrapperX<CrawlRawDO>()
                .eq(CrawlRawDO::getSourceUrl, url)
                .eq(CrawlRawDO::getContentHash, contentHash)) > 0;
    }
}
