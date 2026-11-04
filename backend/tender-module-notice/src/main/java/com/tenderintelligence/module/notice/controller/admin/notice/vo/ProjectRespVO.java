package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目列表项（project-centric）
 */
@Schema(description = "管理后台 - 项目 Response VO")
@Data
public class ProjectRespVO {

    @Schema(description = "项目 ID", example = "100")
    private Long id;
    @Schema(description = "本地归并键")
    private String projectKey;
    @Schema(description = "统一交易标识码")
    private String unifiedDealCode;
    @Schema(description = "项目编号")
    private String projectNo;
    @Schema(description = "项目名称")
    private String projectName;
    @Schema(description = "交易分类代码")
    private String tradeCategoryCode;
    @Schema(description = "交易分类名称")
    private String tradeCategoryName;

    @Schema(description = "省级代码")
    private String provinceCode;
    @Schema(description = "省份名称")
    private String provinceName;
    @Schema(description = "市级代码")
    private String cityCode;
    @Schema(description = "城市名称")
    private String cityName;

    @Schema(description = "项目预算（万元）")
    private BigDecimal budget;
    @Schema(description = "招标人企业 ID")
    private Long tendererEnterpriseId;
    @Schema(description = "招标人")
    private String tendererName;
    @Schema(description = "代理机构企业 ID")
    private Long agencyEnterpriseId;
    @Schema(description = "代理机构")
    private String agencyName;

    @Schema(description = "状态：ACTIVE / CLOSED / CANCELLED")
    private String status;
    @Schema(description = "首次公告时间")
    private LocalDateTime firstPublishTime;
    @Schema(description = "最近公告时间")
    private LocalDateTime lastPublishTime;
    @Schema(description = "公告数")
    private Integer noticeCount;
    @Schema(description = "标段数")
    private Integer lotCount;
    @Schema(description = "来源站点数")
    private Integer sourceCount;
    @Schema(description = "数据质量状态")
    private String dataQualityStatus;

    @Schema(description = "最新公告 ID")
    private Long latestNoticeId;
    @Schema(description = "最新公告类型代码")
    private String latestNoticeTypeCode;
    @Schema(description = "最新公告类型名称")
    private String latestNoticeTypeName;
    @Schema(description = "最新公告标题")
    private String latestNoticeTitle;
    @Schema(description = "最新公告发布时间")
    private LocalDateTime latestNoticePublishTime;
}
