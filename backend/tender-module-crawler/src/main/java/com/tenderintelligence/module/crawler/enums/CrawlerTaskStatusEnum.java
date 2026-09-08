package com.tenderintelligence.module.crawler.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 采集任务状态枚举
 */
@Getter
@AllArgsConstructor
public enum CrawlerTaskStatusEnum {

    /** 运行中 */
    RUNNING("RUNNING"),
    /** 成功 */
    SUCCESS("SUCCESS"),
    /** 失败 */
    FAILED("FAILED");

    private final String status;
}
