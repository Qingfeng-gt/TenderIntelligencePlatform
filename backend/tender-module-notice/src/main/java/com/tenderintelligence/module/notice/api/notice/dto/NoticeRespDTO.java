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
     * 行业。⚠️ 这是 crawler 的规则抽取结果，不是权威标签——见
     * {@link NoticeQueryDTO#getIndustry()} 的说明
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
     * 源站详情 URL（唯一去重键）
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

    /**
     * 附件列表
     */
    private List<NoticeAttachmentRespDTO> attachments;

}
