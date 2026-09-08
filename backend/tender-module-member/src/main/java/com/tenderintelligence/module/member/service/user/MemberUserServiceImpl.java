package com.tenderintelligence.module.member.service.user;

import cn.hutool.core.util.StrUtil;
import com.tenderintelligence.framework.security.config.SecurityProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.tenderintelligence.module.member.controller.app.auth.vo.MemberRegisterReqVO;
import com.tenderintelligence.module.member.dal.dataobject.MemberUserDO;
import com.tenderintelligence.module.member.dal.mysql.MemberUserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.tenderintelligence.framework.common.enums.CommonStatusEnum.ENABLE;

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

}
