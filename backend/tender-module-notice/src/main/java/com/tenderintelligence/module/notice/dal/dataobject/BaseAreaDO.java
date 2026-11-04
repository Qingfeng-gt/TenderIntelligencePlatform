package com.tenderintelligence.module.notice.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

/**
 * 行政区划（GB/T 2260）
 *
 * <p>对应表 {@code base_area}。层级：1 省 / 2 市 / 3 区县。
 */
@TableName("base_area")
@KeySequence("base_area_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BaseAreaDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 行政区划代码
     */
    private String code;
    /**
     * 上级代码，省级为空
     */
    private String parentCode;
    /**
     * 层级：1 省 / 2 市 / 3 区县
     */
    private Integer level;
    /**
     * 名称
     */
    private String name;
    /**
     * 简称，如 湖北
     */
    private String shortName;
    /**
     * 全称，如 湖北省
     */
    private String fullName;
    /**
     * 拼音
     */
    private String pinyin;
    /**
     * province / city / district
     */
    private String regionType;
    /**
     * 生效日期
     */
    private LocalDate validFrom;
    /**
     * 失效日期
     */
    private LocalDate validTo;
    /**
     * 是否启用
     */
    private Boolean enabled;
    /**
     * 排序
     */
    private Integer sort;
}
