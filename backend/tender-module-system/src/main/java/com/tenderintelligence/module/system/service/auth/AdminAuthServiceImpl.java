package com.tenderintelligence.module.system.service.auth;

import cn.hutool.core.util.ObjectUtil;
import com.tenderintelligence.framework.common.enums.CommonStatusEnum;
import com.tenderintelligence.framework.common.enums.UserTypeEnum;
import com.tenderintelligence.framework.common.util.monitor.TracerUtils;
import com.tenderintelligence.framework.common.util.object.BeanUtils;
import com.tenderintelligence.framework.common.util.servlet.ServletUtils;
import com.tenderintelligence.framework.common.util.validation.ValidationUtils;
import com.tenderintelligence.framework.datapermission.core.annotation.DataPermission;
import com.tenderintelligence.module.system.api.logger.dto.LoginLogCreateReqDTO;
import com.tenderintelligence.module.system.controller.admin.auth.vo.*;
import com.tenderintelligence.module.system.convert.auth.AuthConvert;
import com.tenderintelligence.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.tenderintelligence.module.system.dal.dataobject.user.AdminUserDO;
import com.tenderintelligence.module.system.enums.logger.LoginLogTypeEnum;
import com.tenderintelligence.module.system.enums.logger.LoginResultEnum;
import com.tenderintelligence.module.system.enums.oauth2.OAuth2ClientConstants;
import com.tenderintelligence.module.system.service.logger.LoginLogService;
import com.tenderintelligence.module.system.service.member.MemberService;
import com.tenderintelligence.module.system.service.oauth2.OAuth2TokenService;
import com.tenderintelligence.module.system.service.user.AdminUserService;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import jakarta.validation.Validator;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.tenderintelligence.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.tenderintelligence.framework.common.util.servlet.ServletUtils.getClientIP;
import static com.tenderintelligence.module.system.enums.ErrorCodeConstants.*;

/**
 * Auth Service 实现类
 *
 * @author Tender Intelligence
 */
@Service
@Slf4j
public class AdminAuthServiceImpl implements AdminAuthService {

    @Resource
    private AdminUserService userService;
    @Resource
    private LoginLogService loginLogService;
    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private MemberService memberService;
    @Resource
    private Validator validator;
    @Resource
    private CaptchaService captchaService;

    /**
     * 验证码的开关，默认为 true
     */
    @Value("${tender.captcha.enable:true}")
    @Setter // 为了单测：开启或者关闭验证码
    private Boolean captchaEnable;

    @Override
    public AdminUserDO authenticate(String username, String password) {
        final LoginLogTypeEnum logTypeEnum = LoginLogTypeEnum.LOGIN_USERNAME;
        // 校验账号是否存在
        AdminUserDO user = userService.getUserByUsername(username);
        if (user == null) {
            createLoginLog(null, username, logTypeEnum, LoginResultEnum.BAD_CREDENTIALS);
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        if (!userService.isPasswordMatch(password, user.getPassword())) {
            createLoginLog(user.getId(), username, logTypeEnum, LoginResultEnum.BAD_CREDENTIALS);
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        // 校验是否禁用
        validateUserStatus(user, username, logTypeEnum);
        return user;
    }

    @Override
    @DataPermission(enable = false)
    public AuthLoginRespVO login(AuthLoginReqVO reqVO) {
        // 校验验证码
        validateCaptcha(reqVO);

        // 使用账号密码，进行登录
        AdminUserDO user = authenticate(reqVO.getUsername(), reqVO.getPassword());

        // 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(user, reqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME);
    }

    private void createLoginLog(Long userId, String username,
                                LoginLogTypeEnum logTypeEnum, LoginResultEnum loginResult) {
        // 插入登录日志
        LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
        reqDTO.setLogType(logTypeEnum.getType());
        reqDTO.setTraceId(TracerUtils.getTraceId());
        reqDTO.setUserId(userId);
        reqDTO.setUserType(getUserType().getValue());
        reqDTO.setUsername(username);
        reqDTO.setUserAgent(ServletUtils.getUserAgent());
        reqDTO.setUserIp(ServletUtils.getClientIP());
        reqDTO.setResult(loginResult.getResult());
        loginLogService.createLoginLog(reqDTO);
        // 更新最后登录时间
        if (userId != null && Objects.equals(LoginResultEnum.SUCCESS.getResult(), loginResult.getResult())) {
            userService.updateUserLogin(userId, ServletUtils.getClientIP());
        }
    }

    @VisibleForTesting
    void validateCaptcha(AuthLoginReqVO reqVO) {
        ResponseModel response = doValidateCaptcha(reqVO);
        // 校验验证码
        if (!response.isSuccess()) {
            // 创建登录失败日志（验证码不正确)
            createLoginLog(null, reqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME, LoginResultEnum.CAPTCHA_CODE_ERROR);
            throw exception(AUTH_LOGIN_CAPTCHA_CODE_ERROR, response.getRepMsg());
        }
    }

    private ResponseModel doValidateCaptcha(CaptchaVerificationReqVO reqVO) {
        // 如果验证码关闭，则不进行校验
        if (!captchaEnable) {
            return ResponseModel.success();
        }
        ValidationUtils.validate(validator, reqVO, CaptchaVerificationReqVO.CodeEnableGroup.class);
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(reqVO.getCaptchaVerification());
        return captchaService.verification(captchaVO);
    }

    private AuthLoginRespVO createTokenAfterLoginSuccess(AdminUserDO user, String username, LoginLogTypeEnum logType) {
        // 统一校验用户状态，避免短信、社交等登录方式遗漏
        validateUserStatus(user, username, logType);

        // 插入登陆日志
        createLoginLog(user.getId(), username, logType, LoginResultEnum.SUCCESS);
        // 创建访问令牌
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.createAccessToken(user.getId(), getUserType().getValue(),
                OAuth2ClientConstants.CLIENT_ID_DEFAULT, null);
        // 构建返回结果
        return BeanUtils.toBean(accessTokenDO, AuthLoginRespVO.class);
    }

    private void validateUserStatus(AdminUserDO user, String username, LoginLogTypeEnum logType) {
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            createLoginLog(user.getId(), username, logType, LoginResultEnum.USER_DISABLED);
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }
    }

    @Override
    public AuthLoginRespVO refreshToken(String refreshToken) {
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.refreshAccessToken(refreshToken, OAuth2ClientConstants.CLIENT_ID_DEFAULT);
        return BeanUtils.toBean(accessTokenDO, AuthLoginRespVO.class);
    }

    @Override
    public void logout(String token, Integer logType) {
        // 删除访问令牌
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.removeAccessToken(token);
        if (accessTokenDO == null) {
            return;
        }
        // 删除成功，则记录登出日志
        createLogoutLog(accessTokenDO.getUserId(), accessTokenDO.getUserType(), logType);
    }

    private void createLogoutLog(Long userId, Integer userType, Integer logType) {
        LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
        reqDTO.setLogType(logType);
        reqDTO.setTraceId(TracerUtils.getTraceId());
        reqDTO.setUserId(userId);
        reqDTO.setUserType(userType);
        if (ObjectUtil.equal(getUserType().getValue(), userType)) {
            reqDTO.setUsername(getUsername(userId));
        } else {
            reqDTO.setUsername(memberService.getMemberUserMobile(userId));
        }
        reqDTO.setUserAgent(ServletUtils.getUserAgent());
        reqDTO.setUserIp(ServletUtils.getClientIP());
        reqDTO.setResult(LoginResultEnum.SUCCESS.getResult());
        loginLogService.createLoginLog(reqDTO);
    }

    private String getUsername(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserDO user = userService.getUser(userId);
        return user != null ? user.getUsername() : null;
    }

    private UserTypeEnum getUserType() {
        return UserTypeEnum.ADMIN;
    }
}
