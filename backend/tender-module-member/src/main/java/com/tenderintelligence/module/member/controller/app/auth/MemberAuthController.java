package com.tenderintelligence.module.member.controller.app.auth;

import cn.hutool.core.util.StrUtil;
import com.tenderintelligence.framework.common.pojo.CommonResult;
import com.tenderintelligence.framework.security.config.SecurityProperties;
import com.tenderintelligence.framework.security.core.util.SecurityFrameworkUtils;
import com.tenderintelligence.framework.tenant.core.aop.TenantIgnore;
import com.tenderintelligence.module.member.controller.app.auth.vo.AppAuthLoginRespVO;
import com.tenderintelligence.module.member.controller.app.auth.vo.MemberLoginReqVO;
import com.tenderintelligence.module.member.controller.app.auth.vo.MemberProfileRespVO;
import com.tenderintelligence.module.member.controller.app.auth.vo.MemberRegisterReqVO;
import com.tenderintelligence.module.member.service.auth.MemberAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tenderintelligence.framework.common.pojo.CommonResult.success;
import static com.tenderintelligence.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 用户端 - 会员认证接口(账号密码 + 滑块验证码)
 *
 * 挂载在 /app-api 前缀,令牌类型为 MEMBER,与后台 admin-api 互相隔离
 */
@Tag(name = "用户端 - 会员认证")
@RestController
@RequestMapping("/member/auth")
@Validated
@TenantIgnore
public class MemberAuthController {

    @Resource
    private MemberAuthService authService;
    @Resource
    private SecurityProperties securityProperties;

    @PostMapping("/login")
    @PermitAll
    @Operation(summary = "会员账号密码登录")
    public CommonResult<AppAuthLoginRespVO> login(@RequestBody @Valid MemberLoginReqVO reqVO) {
        return success(authService.login(reqVO));
    }

    @PostMapping("/register")
    @PermitAll
    @Operation(summary = "会员注册(成功后自动登录)")
    public CommonResult<AppAuthLoginRespVO> register(@RequestBody @Valid MemberRegisterReqVO reqVO) {
        return success(authService.register(reqVO));
    }

    @PostMapping("/logout")
    @PermitAll
    @Operation(summary = "会员登出")
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        String token = SecurityFrameworkUtils.obtainAuthorization(request,
                securityProperties.getTokenHeader(), securityProperties.getTokenParameter());
        if (StrUtil.isNotBlank(token)) {
            authService.logout(token);
        }
        return success(true);
    }

    @GetMapping("/get-profile")
    @Operation(summary = "获得会员个人信息")
    public CommonResult<MemberProfileRespVO> getProfile() {
        return success(authService.getProfile(getLoginUserId()));
    }

}
