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

    /**
     * 附件列表。入库时<b>整组重写</b>（先删后写），传 null 或空列表表示该公告没有附件
     */
    private List<NoticeAttachmentDTO> attachments;

}
