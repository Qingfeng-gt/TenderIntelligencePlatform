package com.tenderintelligence.module.member.enums;

import com.tenderintelligence.framework.common.exception.ErrorCode;

/**
 * Member 错误码枚举
 *
 * member 模块,错误码区间 [1-004-000-000 ~ 1-005-000-000)
 */
public interface ErrorCodeConstants {

    // ========== 认证模块 1-004-000-000 ==========
    ErrorCode AUTH_REGISTER_USERNAME_EXISTS = new ErrorCode(1_004_000_001, "注册用户,用户名已存在");
    ErrorCode AUTH_MEMBER_NOT_EXISTS = new ErrorCode(1_004_000_002, "会员不存在");

    ErrorCode MEMBER_USER_NOT_EXISTS = new ErrorCode(1_004_001_000, "会员用户不存在");

}
