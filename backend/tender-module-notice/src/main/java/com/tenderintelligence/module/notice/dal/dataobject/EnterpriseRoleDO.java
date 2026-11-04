package com.tenderintelligence.module.notice.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 企业角色（一企多角色）
 *
 * <p>对应表 {@code enterprise_role}。
 */
@TableName("enterprise_role")
@KeySequence("enterprise_role_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseRoleDO extends BaseDO {

    @TableId
    private Long id;
    /** 企业 ID */
    private Long enterpriseId;
    /** 角色代码：TENDERER/AGENCY/SUPPLIER/WINNER */
    private String roleCode;
    /** 首次出现的公告 ID */
    private Long firstSeenNoticeId;
    /** 最近出现的公告 ID */
    private Long lastSeenNoticeId;
    /** 状态：ACTIVE/INACTIVE */
    private String status;
}
