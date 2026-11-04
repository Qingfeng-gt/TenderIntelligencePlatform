package com.tenderintelligence.module.notice.service;

import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.ProjectDetailRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.ProjectPageReqVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.ProjectRespVO;
import jakarta.validation.Valid;

/**
 * 项目（project-centric）读服务
 */
public interface ProjectService {

    /**
     * 项目分页
     */
    PageResult<ProjectRespVO> getProjectPage(@Valid ProjectPageReqVO pageReqVO);

    /**
     * 项目详情（含招标项目、标段、公告、企业）
     */
    ProjectDetailRespVO getProjectDetail(Long id);
}
