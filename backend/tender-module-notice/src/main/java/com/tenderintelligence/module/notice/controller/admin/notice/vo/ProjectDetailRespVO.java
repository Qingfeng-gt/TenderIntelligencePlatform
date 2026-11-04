package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 项目详情（四层模型聚合）
 */
@Schema(description = "管理后台 - 项目详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProjectDetailRespVO extends ProjectRespVO {

    @Schema(description = "招标项目列表")
    private List<TenderProjectRespVO> tenderProjects;

    @Schema(description = "标段列表")
    private List<TenderLotRespVO> lots;

    @Schema(description = "公告列表（按发布时间倒序）")
    private List<NoticeRespVO> notices;

    @Schema(description = "关联企业及角色")
    private List<NoticeEnterpriseRespVO> enterprises;
}
