package com.tenderintelligence.module.notice.service;

import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticePageReqVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeRespVO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import jakarta.validation.Valid;

/**
 * 招投标公告 Service 接口
 */
public interface NoticeService {

    /**
     * 分页查询公告(用户端门户)
     */
    PageResult<NoticePortalDO> getNoticePage(@Valid NoticePageReqVO pageReqVO);

    /**
     * 获取公告详情
     */
    NoticePortalDO getNotice(Long id);

    /**
     * 今日新增公告数量
     */
    Long getTodayCount();

    /**
     * 公告总数(用户端首页统计)
     */
    Long getTotalCount();

    /**
     * 覆盖省份数量(用户端首页统计)
     */
    Long getProvinceCount();

    /**
     * 行业分类数量(用户端首页统计)
     */
    Long getIndustryCount();
}
