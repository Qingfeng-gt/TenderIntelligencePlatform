package com.tenderintelligence.module.notice.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeEnterpriseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 公告-企业关联 Mapper
 */
@Mapper
public interface NoticeEnterpriseMapper extends BaseMapperX<NoticeEnterpriseDO> {

    default List<NoticeEnterpriseDO> selectListByNoticeId(Long noticeId) {
        return selectList(new LambdaQueryWrapperX<NoticeEnterpriseDO>()
                .eq(NoticeEnterpriseDO::getNoticeId, noticeId)
                .orderByAsc(NoticeEnterpriseDO::getId));
    }

    default List<NoticeEnterpriseDO> selectListByNoticeIds(Collection<Long> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<NoticeEnterpriseDO>()
                .in(NoticeEnterpriseDO::getNoticeId, noticeIds)
                .orderByAsc(NoticeEnterpriseDO::getNoticeId)
                .orderByAsc(NoticeEnterpriseDO::getId));
    }

    default NoticeEnterpriseDO selectOneByNoticeEnterpriseRole(Long noticeId, Long enterpriseId, String roleCode) {
        return selectOne(new LambdaQueryWrapperX<NoticeEnterpriseDO>()
                .eq(NoticeEnterpriseDO::getNoticeId, noticeId)
                .eq(NoticeEnterpriseDO::getEnterpriseId, enterpriseId)
                .eq(NoticeEnterpriseDO::getRoleCode, roleCode)
                .last("LIMIT 1"));
    }
}
