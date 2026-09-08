package com.tenderintelligence.module.member.service.auth;

import cn.hutool.core.util.StrUtil;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.tenderintelligence.framework.common.enums.CommonStatusEnum;
import com.tenderintelligence.framework.common.enums.UserTypeEnum;
import com.tenderintelligence.framework.common.exception.util.ServiceExceptionUtil;
import com.tenderintelligence.framework.common.util.servlet.ServletUtils;
import com.tenderintelligence.framework.common.util.monitor.TracerUtils;
import com.tenderintelligence.framework.common.util.validation.ValidationUtils;
import com.tenderintelligence.framework.security.config.SecurityProperties;
import com.tenderintelligence.module.member.controller.app.auth.vo.AppAuthLoginRespVO;
import com.tenderintelligence.module.member.controller.app.auth.vo.MemberLoginReqVO;
import com.tenderintelligence.module.member.controller.app.auth.vo.MemberProfileRespVO;
import com.tenderintelligence.module.member.controller.app.auth.vo.MemberRegisterReqVO;
import com.tenderintelligence.module.member.dal.dataobject.MemberUserDO;
import com.tenderintelligence.module.member.service.user.MemberUserService;
import com.tenderintelligence.module.system.api.logger.dto.LoginLogCreateReqDTO;
import com.tenderintelligence.module.system.controller.admin.auth.vo.CaptchaVerificationReqVO;
import com.tenderintelligence.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.tenderintelligence.module.system.enums.logger.LoginLogTypeEnum;
import com.tenderintelligence.module.system.enums.logger.LoginResultEnum;
import com.tenderintelligence.module.system.enums.oauth2.OAuth2ClientConstants;
import com.tenderintelligence.module.system.service.logger.LoginLogService;
import com.tenderintelligence.module.system.service.oauth2.OAuth2TokenService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.validation.Validator;

import static com.tenderintelligence.framework.common.util.object.BeanUtils.toBean;
import static com.tenderintelligence.module.member.enums.ErrorCodeConstants.AUTH_MEMBER_NOT_EXISTS;
import static com.tenderintelligence.module.member.enums.ErrorCodeConstants.AUTH_REGISTER_USERNAME_EXISTS;
import static com.tenderintelligence.module.system.enums.ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS;
import static com.tenderintelligence.module.system.enums.ErrorCodeConstants.AUTH_LOGIN_CAPTCHA_CODE_ERROR;
import static com.tenderintelligence.module.system.enums.ErrorCodeConstants.AUTH_LOGIN_USER_DISABLED;

/**
 * 用户端 - 认证 Service 实现
 *
 * 复用 system 模块的 OAuth2 Token 体系:以 MEMBER 类型创建令牌,
 * 由 TokenAuthenticationFilter 校验 /app-api 前缀请求的用户类型
 */
@Service
@Slf4j
public class MemberAuthServiceImpl implements MemberAuthService {

    @Resource
    private MemberUserService memberUserService;
    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private CaptchaService captchaService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private SecurityProperties securityProperties;
    @Resource
    private LoginLogService loginLogService;
    @Resource
    private Validator validator;

    @Value("${tender.captcha.enable:true}")
    private Boolean captchaEnable;

    @Override
    public AppAuthLoginRespVO login(MemberLoginReqVO reqVO) {
        // 校验验证码
        validateCaptcha(reqVO);

        // 使用账号密码，进行登录
        MemberUserDO user = authenticate(reqVO.getUsername(), reqVO.getPassword());

        // 创建令牌，返回结果
        return createTokenAfterLoginSuccess(user, reqVO.getUsername());
    }

    @Override
    public AppAuthLoginRespVO register(MemberRegisterReqVO reqVO) {
        // 校验验证码
        validateCaptcha(reqVO);

        // 校验用户名唯一
        if (memberUserService.getUserByUsername(reqVO.getUsername()) != null) {
            throw ServiceExceptionUtil.exception(AUTH_REGISTER_USERNAME_EXISTS);
        }

        // 注册用户
        MemberUserDO user = memberUserService.createUser(reqVO);

        // 注册成功后,自动登录
        return createTokenAfterLoginSuccess(user, reqVO.getUsername());
    }

    @Override
    public void logout(String token) {
        // 删除访问令牌
        oauth2TokenService.removeAccessToken(token);
    }

    @Override
    public MemberProfileRespVO getProfile(Long userId) {
        MemberUserDO user = memberUserService.getUser(userId);
        if (user == null) {
            throw ServiceExceptionUtil.exception(AUTH_MEMBER_NOT_EXISTS);
        }
        return toBean(user, MemberProfileRespVO.class);
    }

    // ========== 私有方法 ==========

    private MemberUserDO authenticate(String username, String password) {
        MemberUserDO user = memberUserService.getUserByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            // 登录失败,账号密码不正确
            throw ServiceExceptionUtil.exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        return user;
    }

    private void validateCaptcha(CaptchaVerificationReqVO reqVO) {
        ResponseModel response = doValidateCaptcha(reqVO);
        // 校验验证码
        if (!response.isSuccess()) {
            throw ServiceExceptionUtil.exception(AUTH_LOGIN_CAPTCHA_CODE_ERROR, response.getRepMsg());
        }
    }

    private ResponseModel doValidateCaptcha(CaptchaVerificationReqVO reqVO) {
        // 如果验证码关闭，则不进行校验
        if (!captchaEnable) {
            return ResponseModel.success();
        }
        // 校验验证码参数
        ValidationUtils.validate(validator, reqVO, CaptchaVerificationReqVO.CodeEnableGroup.class);
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(reqVO.getCaptchaVerification());
        return captchaService.verification(captchaVO);
    }

    private AppAuthLoginRespVO createTokenAfterLoginSuccess(MemberUserDO user, String username) {
        // 统一校验用户状态
        checkUserStatus(user);
        // 记录登录日志,更新最后登录信息
        createLoginLog(user.getId(), username, LoginLogTypeEnum.LOGIN_USERNAME.getType(),
                LoginResultEnum.SUCCESS.getResult());
        memberUserService.updateUserLogin(user.getId(), ServletUtils.getClientIP());

        // 创建访问令牌
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.createAccessToken(user.getId(),
                UserTypeEnum.MEMBER.getValue(), OAuth2ClientConstants.CLIENT_ID_DEFAULT, null);
        // 构建返回结果
        return toBean(accessTokenDO, AppAuthLoginRespVO.class);
    }

    /**
     * 创建登录日志
     */
    private void createLoginLog(Long userId, String username, Integer logType, Integer result) {
        LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
        reqDTO.setLogType(logType);
        reqDTO.setTraceId(TracerUtils.getTraceId());
        reqDTO.setUserId(userId);
        reqDTO.setUserType(UserTypeEnum.MEMBER.getValue());
        reqDTO.setUsername(username);
        reqDTO.setUserAgent(ServletUtils.getUserAgent());
        reqDTO.setUserIp(ServletUtils.getClientIP());
        reqDTO.setResult(result);
        loginLogService.createLoginLog(reqDTO);
    }

    private void checkUserStatus(MemberUserDO user) {
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            throw ServiceExceptionUtil.exception(AUTH_LOGIN_USER_DISABLED);
        }
    }

}
