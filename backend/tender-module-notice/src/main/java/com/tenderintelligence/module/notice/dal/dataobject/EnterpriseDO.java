package com.tenderintelligence.module.notice.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 企业主体（企业主体域）
 *
 * <p>对应表 {@code enterprise}。公告通过 {@code tenderer_enterprise_id} /
 * {@code agency_enterprise_id} / {@code winner_enterprise_id} 以及子表
 * {@link NoticeEnterpriseDO} 关联。
 */
@TableName("enterprise")
@KeySequence("enterprise_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 企业名称（源站原文）
     */
    private String name;
    /**
     * 归一化名称（去空格/全半角/括号）
     */
    private String normalizedName;
    /**
     * 归一化名称 SHA-256
     */
    private String nameHash;
    /**
     * 统一社会信用代码
     */
    private String unifiedSocialCreditCode;
    /**
     * 企业类型代码
     */
    private String enterpriseTypeCode;
    /**
     * 法定代表人
     */
    private String legalPerson;
    /**
     * 注册资本（万元）
     */
    private BigDecimal registeredCapital;
    /**
     * 成立日期
     */
    private LocalDate establishedDate;
    /**
     * 企业地址
     */
    private String address;
    /**
     * 行政区划代码
     */
    private String regionCode;
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
     * 默认联系人
     */
    private String contactName;
    /**
     * 默认联系电话
     */
    private String contactPhone;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 官网
     */
    private String website;
    /**
     * 来源类型：NOTICE / MANUAL / IMPORT / NOTICE_BACKFILL
     */
    private String sourceType;
    /**
     * 首次来源 URL
     */
    private String sourceUrl;
    /**
     * 状态：ACTIVE / MERGED / INVALID
     */
    private String status;
    /**
     * 被合并到的企业 ID
     */
    private Long mergedToId;
}
