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
 * 标段（包）数据集（四层模型第三层）
 *
 * <p>对应表 {@code tender_lot}。
 */
@TableName("tender_lot")
@KeySequence("tender_lot_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TenderLotDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 招标项目 ID
     */
    private Long tenderProjectId;
    /**
     * 项目 ID（冗余）
     */
    private Long projectId;
    /**
     * 标段（包）编号
     */
    private String lotNo;
    /**
     * 标段名称
     */
    private String lotName;
    /**
     * 标段内容/范围
     */
    private String lotContent;
    /**
     * 标段预算（万元）
     */
    private BigDecimal budget;
    /**
     * 最高限价（万元）
     */
    private BigDecimal priceCeiling;
    /**
     * 保证金（万元）
     */
    private BigDecimal depositAmount;
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
     * 评标办法代码
     */
    private String evaluationMethodCode;
    /**
     * 行政区划代码
     */
    private String regionCode;
    /**
     * 状态：ACTIVE / CLOSED / CANCELLED
     */
    private String status;
    /**
     * 缺失字段汇总（JSON）
     */
    private String missingFields;
}
