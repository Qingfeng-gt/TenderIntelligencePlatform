package com.tenderintelligence.module.notice.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeQualityDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告数据质量 Mapper
 */
@Mapper
public interface NoticeQualityMapper extends BaseMapperX<NoticeQualityDO> {

    /**
     * 清理某公告尚未处理的 OPEN 质量问题（重采时重新计算缺失字段）
     */
    default int deleteOpenByNoticeId(Long noticeId) {
        return delete(new LambdaQueryWrapperX<NoticeQualityDO>()
                .eq(NoticeQualityDO::getNoticeId, noticeId)
                .eq(NoticeQualityDO::getStatus, "OPEN"));
    }
}
