package com.tenderintelligence.module.notice.enums;

import com.tenderintelligence.framework.common.exception.ErrorCode;

/**
 * Notice 错误码枚举
 */
public interface ErrorCodeConstants {

    /** 公告不存在 */
    ErrorCode NOTICE_NOT_EXISTS = new ErrorCode(1_060_001_000, "公告不存在");

}
