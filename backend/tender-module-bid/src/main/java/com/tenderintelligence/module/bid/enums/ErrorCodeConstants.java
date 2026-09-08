package com.tenderintelligence.module.bid.enums;

import com.tenderintelligence.framework.common.exception.ErrorCode;

/**
 * Bid 错误码枚举
 */
public interface ErrorCodeConstants {

    /** 投标项目不存在 */
    ErrorCode BID_PROJECT_NOT_EXISTS = new ErrorCode(1_070_100_000, "投标项目不存在");

    /** 投标项目已存在(同一公告重复发起) */
    ErrorCode BID_PROJECT_ALREADY_EXISTS = new ErrorCode(1_070_100_001, "您已提交过该项目的投标,不可重复发起");

    /** 状态迁移非法 */
    ErrorCode BID_PROJECT_STATUS_ILLEGAL = new ErrorCode(1_070_100_002, "当前状态不允许此操作");
}
