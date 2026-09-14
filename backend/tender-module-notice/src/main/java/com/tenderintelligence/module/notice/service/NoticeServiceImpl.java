package com.tenderintelligence.module.notice.service;

import com.tenderintelligence.framework.common.exception.util.ServiceExceptionUtil;
import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticePageReqVO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import com.tenderintelligence.module.notice.dal.mysql.NoticeAttachmentMapper;
import com.tenderintelligence.module.notice.dal.mysql.NoticePortalMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.tenderintelligence.module.notice.enums.ErrorCodeConstants.NOTICE_NOT_EXISTS;

/**
 * 招投标公告 Service 实现
 */
@Service("noticePortalService")
@Slf4j
public class NoticeServiceImpl implements NoticeService {

    @Resource
    private NoticePortalMapper noticePortalMapper;
    @Resource
    private NoticeAttachmentMapper noticeAttachmentMapper;

    @Override
    public PageResult<NoticePortalDO> getNoticePage(NoticePageReqVO pageReqVO) {
        // 列表不查附件: 逐条查会产生 N+1, 而列表页也用不到附件
        return noticePortalMapper.selectPage(pageReqVO);
    }

    @Override
    public NoticePortalDO getNotice(Long id) {
        NoticePortalDO notice = noticePortalMapper.selectById(id);
        if (notice == null) {
            throw ServiceExceptionUtil.exception(NOTICE_NOT_EXISTS);
        }
        notice.setAttachments(noticeAttachmentMapper.selectListByNoticeId(id));
        return notice;
    }

    @Override
    public Long getTodayCount() {
        return noticePortalMapper.selectCountByPublishTime(LocalDate.now().atStartOfDay());
    }

    @Override
    public Long getTotalCount() {
        return noticePortalMapper.selectTotalCount();
    }

    @Override
    public Long getProvinceCount() {
        return noticePortalMapper.selectProvinceCount();
    }

    @Override
    public Long getIndustryCount() {
        return noticePortalMapper.selectIndustryCount();
    }
}
