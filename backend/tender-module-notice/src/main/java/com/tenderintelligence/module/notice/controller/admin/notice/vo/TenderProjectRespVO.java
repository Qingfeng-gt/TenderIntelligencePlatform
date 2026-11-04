package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 招标项目（四层模型第二层）
 */
@Schema(description = "管理后台 - 招标项目 Response VO")
@Data
public class TenderProjectRespVO {

    @Schema(description = "招标项目 ID", example = "200")
    private Long id;
    @Schema(description = "项目 ID", example = "100")
    private Long projectId;
    @Schema(description = "招标项目编号")
    private String tenderProjectNo;
    @Schema(description = "招标项目名称")
    private String tenderProjectName;
    @Schema(description = "采购方式代码")
    private String procurementMethodCode;
    @Schema(description = "采购方式名称")
    private String procurementMethodName;
    @Schema(description = "预算（万元）")
    private BigDecimal budget;
    @Schema(description = "最高限价（万元）")
    private BigDecimal priceCeiling;
    @Schema(description = "资金来源代码")
    private String fundingSourceCode;
    @Schema(description = "标段数量")
    private Integer lotCount;
    @Schema(description = "合同履行期限（天）")
    private Integer contractPeriodDays;
    @Schema(description = "合同期限原文")
    private String contractPeriodText;
    @Schema(description = "资格条件摘要")
    private String qualificationSummary;
    @Schema(description = "评标办法代码")
    private String evaluationMethodCode;
    @Schema(description = "评标办法名称")
    private String evaluationMethodName;
    @Schema(description = "招标人企业 ID")
    private Long tendererEnterpriseId;
    @Schema(description = "招标人")
    private String tendererName;
    @Schema(description = "代理机构企业 ID")
    private Long agencyEnterpriseId;
    @Schema(description = "代理机构")
    private String agencyName;
    @Schema(description = "行政区划代码")
    private String regionCode;
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
    @Schema(description = "状态")
    private String status;
}
