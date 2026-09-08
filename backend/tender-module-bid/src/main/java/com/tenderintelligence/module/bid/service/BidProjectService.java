package com.tenderintelligence.module.bid.service;

import com.tenderintelligence.module.bid.controller.admin.bid.vo.BidProjectCreateReqVO;
import com.tenderintelligence.module.bid.dal.dataobject.BidProjectDO;

import java.util.List;

/**
 * 投标项目 Service 接口
 */
public interface BidProjectService {

    /**
     * 用户针对公告发起投标(创建投标项目)
     *
     * @param userId 用户(演示缺省1, 正式接会员)
     */
    Long createBidProject(Long userId, BidProjectCreateReqVO reqVO);

    /**
     * 查询用户的投标项目列表
     */
    List<BidProjectDO> getBidProjectList(Long userId, String status);

    /**
     * 查询投标项目详情
     */
    BidProjectDO getBidProject(Long id, Long userId);

    /**
     * 状态流转(状态机校验)
     */
    void updateStatus(Long id, String targetStatus, Long userId);
}
