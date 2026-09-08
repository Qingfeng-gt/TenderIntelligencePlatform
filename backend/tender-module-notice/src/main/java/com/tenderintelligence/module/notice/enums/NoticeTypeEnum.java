package com.tenderintelligence.module.notice.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 公告类型枚举
 */
@Getter
@AllArgsConstructor
public enum NoticeTypeEnum {

    /** 招标公告 */
    TENDER("tender"),
    /** 中标公告 */
    WIN("win"),
    /** 变更公告 */
    CHANGE("change"),
    /** 采购公告 */
    EXPLORE("explore");

    /**
     * 类型
     */
    private final String type;
}
