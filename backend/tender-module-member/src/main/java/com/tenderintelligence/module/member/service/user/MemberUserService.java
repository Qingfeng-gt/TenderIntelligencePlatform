package com.tenderintelligence.module.member.service.user;

import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.module.member.controller.admin.user.vo.MemberUserPageReqVO;
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
     * 管理后台分页查询会员用户
     *
     * @param reqVO 查询条件
     * @return 分页结果
     */
    PageResult<MemberUserDO> getUserPage(MemberUserPageReqVO reqVO);

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

    /**
     * 修改会员状态
     *
     * @param id 会员编号
     * @param status 状态：0 正常 / 1 停用
     */
    void updateUserStatus(Long id, Integer status);

    /**
     * 重置会员密码
     *
     * @param id 会员编号
     * @param password 新密码（明文，入库前加密）
     */
    void resetUserPassword(Long id, String password);

}
