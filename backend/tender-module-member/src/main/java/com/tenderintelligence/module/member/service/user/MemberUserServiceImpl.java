package com.tenderintelligence.module.member.service.user;

import cn.hutool.core.util.StrUtil;
import com.tenderintelligence.framework.common.enums.CommonStatusEnum;
import com.tenderintelligence.framework.common.pojo.PageResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.tenderintelligence.module.member.controller.admin.user.vo.MemberUserPageReqVO;
import com.tenderintelligence.module.member.controller.app.auth.vo.MemberRegisterReqVO;
import com.tenderintelligence.module.member.dal.dataobject.MemberUserDO;
import com.tenderintelligence.module.member.dal.mysql.MemberUserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.tenderintelligence.framework.common.enums.CommonStatusEnum.ENABLE;
import static com.tenderintelligence.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.tenderintelligence.module.member.enums.ErrorCodeConstants.MEMBER_USER_NOT_EXISTS;
import static com.tenderintelligence.module.member.enums.ErrorCodeConstants.MEMBER_USER_STATUS_INVALID;

/**
 * 会员用户 Service 实现
 */
@Service
@Slf4j
public class MemberUserServiceImpl implements MemberUserService {

    @Resource
    private MemberUserMapper memberUserMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public MemberUserDO getUser(Long id) {
        return memberUserMapper.selectById(id);
    }

    @Override
    public MemberUserDO getUserByUsername(String username) {
        return memberUserMapper.selectByUsername(username);
    }

    @Override
    public PageResult<MemberUserDO> getUserPage(MemberUserPageReqVO reqVO) {
        return memberUserMapper.selectPage(reqVO);
    }

    @Override
    public MemberUserDO createUser(MemberRegisterReqVO registerReqVO) {
        MemberUserDO user = new MemberUserDO();
        user.setUsername(registerReqVO.getUsername());
        user.setPassword(passwordEncoder.encode(registerReqVO.getPassword()));
        user.setNickname(StrUtil.isBlank(registerReqVO.getNickname())
                ? registerReqVO.getUsername() : registerReqVO.getNickname());
        user.setStatus(ENABLE.getStatus());
        memberUserMapper.insert(user);
        return user;
    }

    @Override
    public void updateUserLogin(Long id, String loginIp) {
        MemberUserDO user = new MemberUserDO();
        user.setId(id);
        user.setLoginIp(loginIp);
        user.setLoginDate(LocalDateTime.now());
        memberUserMapper.updateById(user);
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        validateUserExists(id);
        if (!CommonStatusEnum.isEnable(status) && !CommonStatusEnum.isDisable(status)) {
            throw exception(MEMBER_USER_STATUS_INVALID);
        }
        MemberUserDO update = new MemberUserDO();
        update.setId(id);
        update.setStatus(status);
        memberUserMapper.updateById(update);
    }

    @Override
    public void resetUserPassword(Long id, String password) {
        validateUserExists(id);
        MemberUserDO update = new MemberUserDO();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(password));
        memberUserMapper.updateById(update);
    }

    private MemberUserDO validateUserExists(Long id) {
        MemberUserDO user = memberUserMapper.selectById(id);
        if (user == null) {
            throw exception(MEMBER_USER_NOT_EXISTS);
        }
        return user;
    }

}
