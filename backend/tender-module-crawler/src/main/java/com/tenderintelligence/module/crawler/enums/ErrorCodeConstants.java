package com.tenderintelligence.module.crawler.enums;

import com.tenderintelligence.framework.common.exception.ErrorCode;

/**
 * Crawler 错误码枚举
 */
public interface ErrorCodeConstants {

    /** 爬虫站点不存在 */
    ErrorCode CRAWLER_SITE_NOT_EXISTS = new ErrorCode(1_080_100_000, "爬虫站点不存在");

    /** 爬虫站点未启用 */
    ErrorCode CRAWLER_SITE_DISABLED = new ErrorCode(1_080_100_001, "爬虫站点未启用");
}
