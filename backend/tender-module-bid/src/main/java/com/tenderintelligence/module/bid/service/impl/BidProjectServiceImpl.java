package com.tenderintelligence.module.bid.service.impl;

import com.tenderintelligence.module.bid.controller.admin.bid.vo.BidProjectCreateReqVO;
import com.tenderintelligence.module.bid.dal.dataobject.BidProjectDO;
import com.tenderintelligence.module.bid.dal.mysql.BidProjectMapper;
import com.tenderintelligence.module.bid.enums.BidProjectStatusEnum;
import com.tenderintelligence.module.bid.enums.ErrorCodeConstants;
import com.tenderintelligence.module.bid.service.BidProjectService;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import com.tenderintelligence.module.notice.service.NoticeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.tenderintelligence.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 投标项目 Service 实现
 */
@Service
@Slf4j
public class BidProjectServiceImpl implements BidProjectService {

    @Resource
    private BidProjectMapper bidProjectMapper;
    @Resource
    private NoticeService noticeService;

    @Override
    public Long createBidProject(Long userId, BidProjectCreateReqVO reqVO) {
        // 1) 读取源公告
        NoticePortalDO notice = noticeService.getNotice(reqVO.getNoticeId());

        // 2) 防重复: 同一公告存在在办项目
        if (bidProjectMapper.selectActiveByUserAndNotice(userId, notice.getId()) != null) {
            throw exception(ErrorCodeConstants.BID_PROJECT_ALREADY_EXISTS);
        }

        // 3) 创建(冗余公告关键信息)
        BidProjectDO bidProject = new BidProjectDO();
        bidProject.setUserId(userId);
        bidProject.setNoticeId(notice.getId());
        bidProject.setProjectName(notice.getTitle());
        bidProject.setDeadline(notice.getDeadline());
        bidProject.setBidAmount(reqVO.getBidAmount());
        bidProject.setBidFileName(reqVO.getBidFileName());
        bidProject.setRemark(reqVO.getRemark());
        bidProject.setStatus(BidProjectStatusEnum.SUBMITTED.getStatus());
        bidProjectMapper.insert(bidProject);
        log.info("[bid] 用户{}针对公告#{}发起投标, 项目#{}", userId, notice.getId(), bidProject.getId());
        return bidProject.getId();
    }

    @Override
    public List<BidProjectDO> getBidProjectList(Long userId, String status) {
        return bidProjectMapper.selectListByUser(userId, status);
    }

    @Override
    public BidProjectDO getBidProject(Long id, Long userId) {
        BidProjectDO bidProject = bidProjectMapper.selectById(id);
        if (bidProject == null) {
            throw exception(ErrorCodeConstants.BID_PROJECT_NOT_EXISTS);
        }
        if (userId != null && !bidProject.getUserId().equals(userId)) {
            throw exception(ErrorCodeConstants.BID_PROJECT_NOT_EXISTS);
        }
        return bidProject;
    }

    @Override
    public void updateStatus(Long id, String targetStatus, Long userId) {
        BidProjectDO bidProject = getBidProject(id, userId);
        if (!BidProjectStatusEnum.allowed(bidProject.getStatus(), targetStatus)) {
            throw exception(ErrorCodeConstants.BID_PROJECT_STATUS_ILLEGAL);
        }
        bidProject.setStatus(targetStatus);
        bidProjectMapper.updateById(bidProject);
        log.info("[bid] 项目#{} 状态流转: {} → {}", id, bidProject.getStatus(), targetStatus);
    }
}
