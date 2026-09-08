package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 招投标公告 Response VO")
@Data
public class NoticeRespVO {

    @Schema(description = "公告编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "公告标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "公告类型:tender 招标 / win 中标 / change 变更 / explore 采购")
    private String type;

    @Schema(description = "项目编号(源站)", example = "SXSD2026-AK-056")
    private String projectNo;

    @Schema(description = "源站详情URL")
    private String sourceUrl;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "行业")
    private String industry;

    @Schema(description = "预算金额(万元)")
    private BigDecimal budget;

    @Schema(description = "招标人 / 采购人")
    private String tenderPerson;

    @Schema(description = "代理机构")
    private String agency;

    @Schema(description = "联系人")
    private String contact;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "公告原文(HTML)")
    private String content;

    @Schema(description = "来源网站")
    private String source;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "投标截止时间")
    private LocalDateTime deadline;

    @Schema(description = "开标时间")
    private LocalDateTime openTime;
}
