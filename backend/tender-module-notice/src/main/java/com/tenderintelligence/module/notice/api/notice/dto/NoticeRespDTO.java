package com.tenderintelligence.module.notice.api.notice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告 DTO（读取侧）
 *
 * <p>给丁（M4 检索）、戊（M5 打标）消费。字段与 {@code notice} 表一一对应，
 * 附件在 {@link #attachments} 里一并带出。
 */
@Data
public class NoticeRespDTO {

    /**
     * 公告编号
     */
    private Long id;

    /** 项目 ID */
    private Long projectId;
    /** 招标项目 ID */
    private Long tenderProjectId;
    /** 标段 ID */
    private Long lotId;

    /**
     * 公告标题
     */
    private String title;

    /** 公告类型代码 */
    private String noticeTypeCode;
    /** 公告类型名称 */
    private String noticeTypeName;
    /** 项目名称 */
    private String projectName;
    /** 招标项目名称 */
    private String tenderProjectName;
    /** 采购方式名称 */
    private String procurementMethodName;
    /** 省级代码 */
    private String provinceCode;
    /** 省份名称 */
    private String provinceName;
    /** 市级代码 */
    private String cityCode;
    /** 城市名称 */
    private String cityName;
    /** 最高限价（万元） */
    private BigDecimal priceCeiling;
    /** 保证金（万元） */
    private BigDecimal depositAmount;
    /** 招标人名称 */
    private String tendererName;
    /** 代理机构名称 */
    private String agencyName;
    /** 投标截止时间（国标列） */
    private LocalDateTime bidDeadline;

    /**
     * 项目编号（源站）
     */
    private String projectNo;

    /**
     * 行业（规则抽取结果，非权威标签）
     */
    private String industry;

    /** 行业代码（国标，当前多数为空） */
    private String industryCode;

    /**
     * 行政区划码
     */
    private String regionCode;

    /**
     * 预算金额（万元）
     */
    private BigDecimal budget;

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
     * 来源平台名称
     */
    private String sourcePlatform;

    /**
     * 源站详情 URL（唯一去重键）
     */
    private String sourceUrl;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 开标时间
     */
    private LocalDateTime openTime;

    /**
     * 附件列表
     */
    private List<NoticeAttachmentRespDTO> attachments;

}
