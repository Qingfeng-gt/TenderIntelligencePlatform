package com.tenderintelligence.module.bid.dal.dataobject;

import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 投标项目
 */
@TableName("bid_project")
@KeySequence("bid_project_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BidProjectDO extends BaseDO {

    /** 投标项目编号 */
    @TableId
    private Long id;
    /** 发起用户(演示默认1,正式接会员) */
    private Long userId;
    /** 源公告编号(notice.id) */
    private Long noticeId;
    /** 项目名称(冗余自公告) */
    private String projectName;
    /** 投标截止时间(冗余自公告) */
    private LocalDateTime deadline;
    /** 拟投标金额(万元) */
    private BigDecimal bidAmount;
    /** 投标文件名(演示文本) */
    private String bidFileName;
    /** 状态:SUBMITTED/OTB/WON/LOST/ABANDONED */
    private String status;
    /** 备注 */
    private String remark;
}
