package com.tenderintelligence.module.notice.api.notice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告 DTO（写入侧）
 *
 * <p>由采集适配器解析产出，交给 {@code NoticeApi.upsert(..)} 去重入库。
 *
 * <p>字段与现状的 {@code NoticePortalDO} 对齐——迁移时把 adapter 里 new 的 DO 换成这个 DTO 即可。
 */
@Data
public class NoticeDTO {

    /**
     * 公告标题
     */
    private String title;

    /**
     * 项目编号（源站）
     */
    private String projectNo;

    /**
     * 公告类型：tender / win / change / explore
     */
    private String type;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 行业
     */
    private String industry;

    /**
     * 行政区划码
     */
    private String regionCode;

    /**
     * 预算金额（万元）
     */
    private BigDecimal budget;

    /**
     * 招标人 / 采购人
     */
    private String tenderPerson;

    /**
     * 代理机构
     */
    private String agency;

    /**
     * 联系人
     */
    private String contact;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 公告原文（HTML）
     */
    private String content;

    /**
     * 来源网站标识
     */
    private String source;

    /**
     * 源站详情 URL。<b>唯一去重键，必填</b>
     */
    private String sourceUrl;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 报名 / 投标截止时间
     */
    private LocalDateTime deadline;

    /**
     * 开标时间
     */
    private LocalDateTime openTime;

    // ==================== P0 国标字段（解析器可逐步填充，未填由入库服务兜底） ====================

    /**
     * 统一交易标识码
     */
    private String unifiedDealCode;
    /**
     * 招标项目编号
     */
    private String tenderProjectNo;
    /**
     * 标段（包）编号
     */
    private String lotNo;
    /**
     * 公告标识/编号
     */
    private String noticeNo;
    /**
     * 公告子类型代码
     */
    private String noticeSubtypeCode;
    /**
     * 源站 code
     */
    private String sourceSiteCode;
    /**
     * 来源平台名称
     */
    private String sourcePlatform;
    /**
     * 文件获取开始时间
     */
    private LocalDateTime fileGetStartTime;
    /**
     * 文件获取截止时间
     */
    private LocalDateTime fileGetEndTime;
    /**
     * 答疑/澄清截止时间
     */
    private LocalDateTime qaDeadline;
    /**
     * 报名截止时间
     */
    private LocalDateTime signDeadline;
    /**
     * 投标截止时间（国标列；为空时入库服务回落到 deadline）
     */
    private LocalDateTime bidDeadline;
    /**
     * 开标地点
     */
    private String openPlace;
    /**
     * 公示开始时间
     */
    private LocalDateTime publicityStartTime;
    /**
     * 公示结束时间
     */
    private LocalDateTime publicityEndTime;
    /**
     * 最高限价（万元）
     */
    private BigDecimal priceCeiling;
    /**
     * 保证金（万元）
     */
    private BigDecimal depositAmount;
    /**
     * 文件售价（万元）
     */
    private BigDecimal filePrice;
    /**
     * 中标金额（万元）
     */
    private BigDecimal winAmount;
    /**
     * 采购方式代码
     */
    private String procurementMethodCode;
    /**
     * 资金来源代码
     */
    private String fundingSourceCode;
    /**
     * 评标办法代码
     */
    private String evaluationMethodCode;
    /**
     * 资格要求
     */
    private String qualificationRequirement;
    /**
     * 特定资格
     */
    private String specificQualification;
    /**
     * 是否接受联合体；null 表示未知
     */
    private Boolean consortiumAllowed;
    /**
     * 业绩要求
     */
    private String performanceRequirement;
    /**
     * 资质等级代码
     */
    private String qualificationLevelCode;
    /**
     * 交易分类代码
     */
    private String tradeCategoryCode;
    /**
     * 省级代码
     */
    private String provinceCode;
    /**
     * 市级代码
     */
    private String cityCode;
    /**
     * 区县代码
     */
    private String districtCode;
    /**
     * 行业代码
     */
    private String industryCode;
    /**
     * 解析器版本
     */
    private String parserVersion;

    /**
     * 附件列表。入库时<b>整组重写</b>（先删后写），传 null 或空列表表示该公告没有附件
     */
    private List<NoticeAttachmentDTO> attachments;

}
