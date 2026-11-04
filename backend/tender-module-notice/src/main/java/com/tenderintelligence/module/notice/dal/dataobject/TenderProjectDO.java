package com.tenderintelligence.module.notice.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 招标项目数据集（四层模型第二层）
 *
 * <p>对应表 {@code tender_project}。
 */
@TableName("tender_project")
@KeySequence("tender_project_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TenderProjectDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 项目 ID
     */
    private Long projectId;
    /**
     * 招标项目编号
     */
    private String tenderProjectNo;
    /**
     * 招标项目名称
     */
    private String tenderProjectName;
    /**
     * 采购方式代码
     */
    private String procurementMethodCode;
    /**
     * 预算（万元）
     */
    private BigDecimal budget;
    /**
     * 最高限价（万元）
     */
    private BigDecimal priceCeiling;
    /**
     * 资金来源代码
     */
    private String fundingSourceCode;
    /**
     * 标段数量
     */
    private Integer lotCount;
    /**
     * 合同履行期限（天）
     */
    private Integer contractPeriodDays;
    /**
     * 合同期限原文
     */
    private String contractPeriodText;
    /**
     * 资格条件摘要
     */
    private String qualificationSummary;
    /**
     * 评标办法代码
     */
    private String evaluationMethodCode;
    /**
     * 招标人企业 ID
     */
    private Long tendererEnterpriseId;
    /**
     * 代理机构企业 ID
     */
    private Long agencyEnterpriseId;
    /**
     * 行政区划代码
     */
    private String regionCode;
    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
    /**
     * 状态：ACTIVE / CLOSED / CANCELLED
     */
    private String status;
    /**
     * 缺失字段汇总（JSON）
     */
    private String missingFields;
}
