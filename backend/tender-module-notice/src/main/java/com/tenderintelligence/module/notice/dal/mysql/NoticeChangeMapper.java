package com.tenderintelligence.module.notice.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeChangeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 公告澄清修改 Mapper
 */
@Mapper
public interface NoticeChangeMapper extends BaseMapperX<NoticeChangeDO> {

    default List<NoticeChangeDO> selectListByNoticeId(Long noticeId) {
        return selectList(new LambdaQueryWrapperX<NoticeChangeDO>()
                .eq(NoticeChangeDO::getNoticeId, noticeId)
                .orderByDesc(NoticeChangeDO::getChangeTime)
                .orderByDesc(NoticeChangeDO::getId));
    }
}
