package com.tenderintelligence.module.member.service.auth;

import com.tenderintelligence.module.member.controller.app.auth.vo.AppAuthLoginRespVO;
import com.tenderintelligence.module.member.controller.app.auth.vo.MemberLoginReqVO;
import com.tenderintelligence.module.member.controller.app.auth.vo.MemberProfileRespVO;
import com.tenderintelligence.module.member.controller.app.auth.vo.MemberRegisterReqVO;

/**
 * 用户端 - 认证 Service 接口
 */
public interface MemberAuthService {

    /**
     * 账号密码登录
     *
     * @param reqVO 登录 VO
     * @return 登录结果
     */
    AppAuthLoginRespVO login(MemberLoginReqVO reqVO);

    /**
     * 注册用户并自动登录
     *
     * @param reqVO 注册 VO
     * @return 登录结果
     */
    AppAuthLoginRespVO register(MemberRegisterReqVO reqVO);

    /**
     * 退出登录
     *
     * @param token 访问令牌
     */
    void logout(String token);

    /**
     * 获得会员个人信息
     *
     * @param userId 会员编号
     * @return 个人信息
     */
    MemberProfileRespVO getProfile(Long userId);

}
