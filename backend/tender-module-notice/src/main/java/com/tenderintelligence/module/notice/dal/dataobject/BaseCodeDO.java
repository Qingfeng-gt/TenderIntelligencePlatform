package com.tenderintelligence.module.notice.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 标准编码集
 *
 * <p>对应表 {@code base_code}。按 {@code codeSet} 分组，如 notice_type、procurement_method。
 */
@TableName("base_code")
@KeySequence("base_code_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BaseCodeDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 编码集
     */
    private String codeSet;
    /**
     * 代码
     */
    private String code;
    /**
     * 名称
     */
    private String name;
    /**
     * 父代码，支持层级
     */
    private String parentCode;
    /**
     * 层级
     */
    private Integer level;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 是否启用
     */
    private Boolean enabled;
    /**
     * 备注
     */
    private String remark;
}
