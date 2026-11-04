package com.tenderintelligence.module.notice.service;

import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticePageReqVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeStatsRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.RegionRespVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 招投标公告 Service 接口（四层模型读路径）
 */
public interface NoticeService {

    /**
     * 分页查询公告
     */
    PageResult<NoticeRespVO> getNoticePage(@Valid NoticePageReqVO pageReqVO);

    /**
     * 获取公告详情（含附件、澄清修改、关联企业）
     */
    NoticeRespVO getNoticeDetail(Long id);

    /**
     * 获取公告摘要（不含正文、附件、澄清修改、关联企业）
     *
     * <p>给内部写链路（如投标创建）取标题/截止时间用，避免不必要的关联查询。
     */
    NoticeRespVO getNotice(Long id);

    /**
     * 门户首页统计
     */
    NoticeStatsRespVO getStats();

    /**
     * 省级行政区划选项
     */
    List<RegionRespVO> getProvinces();

    /**
     * 行业选项（规则抽取结果去重）
     */
    List<String> getIndustries();
}
