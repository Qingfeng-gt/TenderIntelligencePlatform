package com.tenderintelligence.module.notice.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 公告-企业关联（多对多 + 角色）
 *
 * <p>对应表 {@code notice_enterprise}。角色：TENDERER / AGENCY / WINNER / SUPPLIER。
 */
@TableName("notice_enterprise")
@KeySequence("notice_enterprise_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NoticeEnterpriseDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 公告 ID
     */
    private Long noticeId;
    /**
     * 企业 ID
     */
    private Long enterpriseId;
    /**
     * 角色代码：TENDERER / AGENCY / WINNER / SUPPLIER
     */
    private String roleCode;
    /**
     * 公告中的原文
     */
    private String sourceText;
    /**
     * 抽取置信度
     */
    private BigDecimal confidence;
    /**
     * 状态：AUTO / CONFIRMED / REJECTED
     */
    private String status;
}
