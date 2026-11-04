package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 标段（包）（四层模型第三层）
 */
@Schema(description = "管理后台 - 标段 Response VO")
@Data
public class TenderLotRespVO {

    @Schema(description = "标段 ID", example = "300")
    private Long id;
    @Schema(description = "招标项目 ID", example = "200")
    private Long tenderProjectId;
    @Schema(description = "项目 ID", example = "100")
    private Long projectId;
    @Schema(description = "标段（包）编号")
    private String lotNo;
    @Schema(description = "标段名称")
    private String lotName;
    @Schema(description = "标段内容/范围")
    private String lotContent;
    @Schema(description = "标段预算（万元）")
    private BigDecimal budget;
    @Schema(description = "最高限价（万元）")
    private BigDecimal priceCeiling;
    @Schema(description = "保证金（万元）")
    private BigDecimal depositAmount;
    @Schema(description = "资格要求")
    private String qualificationRequirement;
    @Schema(description = "特定资格")
    private String specificQualification;
    @Schema(description = "是否接受联合体；null 未知")
    private Boolean consortiumAllowed;
    @Schema(description = "业绩要求")
    private String performanceRequirement;
    @Schema(description = "资质等级代码")
    private String qualificationLevelCode;
    @Schema(description = "资质等级名称")
    private String qualificationLevelName;
    @Schema(description = "评标办法代码")
    private String evaluationMethodCode;
    @Schema(description = "评标办法名称")
    private String evaluationMethodName;
    @Schema(description = "行政区划代码")
    private String regionCode;
    @Schema(description = "状态")
    private String status;
}
