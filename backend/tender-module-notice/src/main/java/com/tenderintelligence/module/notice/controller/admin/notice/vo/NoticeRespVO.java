package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告详情/列表响应（四层模型）
 *
 * <p>所有字段来自新模型：{@code notice} 新列 + {@code project} / {@code tender_project} /
 * {@code tender_lot} / {@code enterprise} / {@code base_area}。旧扁平列已删除，不再兜底。
 */
@Schema(description = "管理后台 - 招投标公告 Response VO")
@Data
public class NoticeRespVO {

    @Schema(description = "公告编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "项目 ID", example = "100")
    private Long projectId;
    @Schema(description = "招标项目 ID", example = "200")
    private Long tenderProjectId;
    @Schema(description = "标段 ID", example = "300")
    private Long lotId;

    @Schema(description = "公告标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "公告类型代码：tender / win / change / explore / qualify", example = "tender")
    private String noticeTypeCode;
    @Schema(description = "公告类型名称", example = "招标公告")
    private String noticeTypeName;

    @Schema(description = "公告标识/编号")
    private String noticeNo;
    @Schema(description = "项目编号")
    private String projectNo;
    @Schema(description = "招标项目编号")
    private String tenderProjectNo;
    @Schema(description = "标段（包）编号")
    private String lotNo;
    @Schema(description = "统一交易标识码")
    private String unifiedDealCode;

    @Schema(description = "项目名称")
    private String projectName;
    @Schema(description = "项目预算（万元）")
    private BigDecimal projectBudget;
    @Schema(description = "项目公告数")
    private Integer projectNoticeCount;
    @Schema(description = "招标项目名称")
    private String tenderProjectName;
    @Schema(description = "采购方式代码")
    private String procurementMethodCode;
    @Schema(description = "采购方式名称")
    private String procurementMethodName;

    @Schema(description = "省级代码")
    private String provinceCode;
    @Schema(description = "省份名称")
    private String provinceName;
    @Schema(description = "市级代码")
    private String cityCode;
    @Schema(description = "城市名称")
    private String cityName;

    @Schema(description = "行业（规则抽取结果，非权威标签）")
    private String industry;
    @Schema(description = "行业代码（国标，当前多数为空）")
    private String industryCode;

    @Schema(description = "预算金额（万元）")
    private BigDecimal budget;
    @Schema(description = "最高限价（万元）")
    private BigDecimal priceCeiling;
    @Schema(description = "保证金（万元）")
    private BigDecimal depositAmount;
    @Schema(description = "中标金额（万元）")
    private BigDecimal winAmount;

    @Schema(description = "招标人企业 ID")
    private Long tendererEnterpriseId;
    @Schema(description = "招标人 / 采购人")
    private String tendererName;
    @Schema(description = "代理机构企业 ID")
    private Long agencyEnterpriseId;
    @Schema(description = "代理机构")
    private String agencyName;
    @Schema(description = "中标人企业 ID")
    private Long winnerEnterpriseId;
    @Schema(description = "中标人")
    private String winnerName;

    @Schema(description = "联系人")
    private String contact;
    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "公告原文（HTML，仅详情接口返回）")
    private String content;

    @Schema(description = "源站 code")
    private String sourceSiteCode;
    @Schema(description = "来源平台名称")
    private String sourcePlatform;
    @Schema(description = "源站详情 URL")
    private String sourceUrl;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
    @Schema(description = "投标截止时间")
    private LocalDateTime bidDeadline;
    @Schema(description = "开标时间")
    private LocalDateTime openTime;
    @Schema(description = "文件获取开始时间")
    private LocalDateTime fileGetStartTime;
    @Schema(description = "文件获取截止时间")
    private LocalDateTime fileGetEndTime;
    @Schema(description = "答疑截止时间")
    private LocalDateTime qaDeadline;
    @Schema(description = "报名截止时间")
    private LocalDateTime signDeadline;
    @Schema(description = "公示开始时间")
    private LocalDateTime publicityStartTime;
    @Schema(description = "公示结束时间")
    private LocalDateTime publicityEndTime;
    @Schema(description = "开标地点")
    private String openPlace;

    @Schema(description = "资格要求")
    private String qualificationRequirement;
    @Schema(description = "特定资格")
    private String specificQualification;
    @Schema(description = "业绩要求")
    private String performanceRequirement;
    @Schema(description = "是否接受联合体；null 未知")
    private Boolean consortiumAllowed;
    @Schema(description = "资质等级代码")
    private String qualificationLevelCode;
    @Schema(description = "资质等级名称")
    private String qualificationLevelName;

    @Schema(description = "是否有附件")
    private Boolean hasAttachment;
    @Schema(description = "浏览次数")
    private Integer viewCount;
    @Schema(description = "公告状态：ACTIVE / OFFLINE")
    private String status;
    @Schema(description = "数据质量状态：OK / PARTIAL / BROKEN / UNKNOWN")
    private String dataQualityStatus;

    @Schema(description = "附件列表（源站直链，仅详情接口返回）")
    private List<NoticeAttachmentRespVO> attachments;

    @Schema(description = "澄清/修改记录（仅详情接口返回）")
    private List<NoticeChangeRespVO> changes;

    @Schema(description = "关联企业及角色（仅详情接口返回）")
    private List<NoticeEnterpriseRespVO> enterprises;
}
