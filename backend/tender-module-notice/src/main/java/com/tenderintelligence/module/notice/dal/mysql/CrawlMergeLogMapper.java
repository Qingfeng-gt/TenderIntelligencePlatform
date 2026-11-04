package com.tenderintelligence.module.notice.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.module.notice.dal.dataobject.CrawlMergeLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 判重/归并日志 Mapper
 */
@Mapper
public interface CrawlMergeLogMapper extends BaseMapperX<CrawlMergeLogDO> {
}
