package com.tenderintelligence.module.infra.enums.codegen;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 代码生成的前端类型枚举
 *
 * @author Tender Intelligence
 */
@AllArgsConstructor
@Getter
public enum CodegenFrontTypeEnum {

    VUE3_ELEMENT_PLUS(20), // Vue3 Element Plus 标准模版
    ;

    /**
     * 类型
     */
    private final Integer type;

}
