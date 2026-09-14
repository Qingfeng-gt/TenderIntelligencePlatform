package com.tenderintelligence.module.notice.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeAttachmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 标讯附件 Mapper
 */
@Mapper
public interface NoticeAttachmentMapper extends BaseMapperX<NoticeAttachmentDO> {

    /**
     * 按公告编号查询附件(按源站出现次序)
     */
    default List<NoticeAttachmentDO> selectListByNoticeId(Long noticeId) {
        return selectList(new LambdaQueryWrapperX<NoticeAttachmentDO>()
                .eq(NoticeAttachmentDO::getNoticeId, noticeId)
                .orderByAsc(NoticeAttachmentDO::getSort));
    }

    /**
     * 按公告编号批量查询附件(多公告一次取回, 避免逐条查询)
     */
    default List<NoticeAttachmentDO> selectListByNoticeIds(Collection<Long> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<NoticeAttachmentDO>()
                .in(NoticeAttachmentDO::getNoticeId, noticeIds)
                .orderByAsc(NoticeAttachmentDO::getNoticeId)
                .orderByAsc(NoticeAttachmentDO::getSort));
    }

    /**
     * 删除某公告的全部附件(重写附件前先清空, 源站删掉的附件不会残留)
     */
    default int deleteByNoticeId(Long noticeId) {
        return delete(new LambdaQueryWrapperX<NoticeAttachmentDO>()
                .eq(NoticeAttachmentDO::getNoticeId, noticeId));
    }
}
