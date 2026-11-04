package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 公告关联企业及角色
 */
@Schema(description = "管理后台 - 公告关联企业 Response VO")
@Data
public class NoticeEnterpriseRespVO {

    @Schema(description = "企业 ID", example = "1")
    private Long enterpriseId;
    @Schema(description = "企业名称")
    private String enterpriseName;
    @Schema(description = "角色代码：TENDERER / AGENCY / WINNER / SUPPLIER")
    private String roleCode;
    @Schema(description = "角色名称")
    private String roleName;
    @Schema(description = "公告中的原文")
    private String sourceText;
    @Schema(description = "抽取置信度")
    private BigDecimal confidence;
    @Schema(description = "状态：AUTO / CONFIRMED / REJECTED")
    private String status;
}
