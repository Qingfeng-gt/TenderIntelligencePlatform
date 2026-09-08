package com.tenderintelligence.module.member.service.user;

import com.tenderintelligence.module.member.controller.app.auth.vo.MemberRegisterReqVO;
import com.tenderintelligence.module.member.dal.dataobject.MemberUserDO;

/**
 * 会员用户 Service 接口
 */
public interface MemberUserService {

    /**
     * 获得单个会员用户
     *
     * @param id 会员编号
     * @return 会员用户
     */
    MemberUserDO getUser(Long id);

    /**
     * 通过账号获得会员用户
     *
     * @param username 账号
     * @return 会员用户
     */
    MemberUserDO getUserByUsername(String username);

    /**
     * 创建会员用户(注册)
     *
     * @param registerReqVO 注册 VO
     * @return 会员用户
     */
    MemberUserDO createUser(MemberRegisterReqVO registerReqVO);

    /**
     * 更新最后登录信息
     *
     * @param id 会员编号
     * @param loginIp 登录 IP
     */
    void updateUserLogin(Long id, String loginIp);

}
