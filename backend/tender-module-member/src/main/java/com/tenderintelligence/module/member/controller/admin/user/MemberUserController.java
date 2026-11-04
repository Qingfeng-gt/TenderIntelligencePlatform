package com.tenderintelligence.module.member.controller.admin.user;

import com.tenderintelligence.framework.common.pojo.CommonResult;
import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.framework.common.util.object.BeanUtils;
import com.tenderintelligence.framework.tenant.core.aop.TenantIgnore;
import com.tenderintelligence.module.member.controller.admin.user.vo.MemberUserPageReqVO;
import com.tenderintelligence.module.member.controller.admin.user.vo.MemberUserResetPasswordReqVO;
import com.tenderintelligence.module.member.controller.admin.user.vo.MemberUserRespVO;
import com.tenderintelligence.module.member.controller.admin.user.vo.MemberUserUpdateStatusReqVO;
import com.tenderintelligence.module.member.dal.dataobject.MemberUserDO;
import com.tenderintelligence.module.member.service.user.MemberUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.tenderintelligence.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.tenderintelligence.framework.common.pojo.CommonResult.success;
import static com.tenderintelligence.module.member.enums.ErrorCodeConstants.MEMBER_USER_NOT_EXISTS;

/**
 * 管理后台 - 会员用户管理
 *
 * <p>会员用户属于 user-web 账号体系（{@code member_user}），与后台管理员
 * （{@code system_users}）相互独立。本接口只供管理后台使用，需要 ADMIN Token。
 */
@Tag(name = "管理后台 - 会员用户")
@RestController
@TenantIgnore
@RequestMapping("/member/user")
@Validated
public class MemberUserController {

    @Resource
    private MemberUserService memberUserService;

    @GetMapping("/page")
    @Operation(summary = "分页查询会员用户")
    public CommonResult<PageResult<MemberUserRespVO>> getMemberUserPage(@Valid MemberUserPageReqVO reqVO) {
        PageResult<MemberUserDO> page = memberUserService.getUserPage(reqVO);
        return success(new PageResult<>(BeanUtils.toBean(page.getList(), MemberUserRespVO.class), page.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "获得会员用户")
    @Parameter(name = "id", description = "会员编号", required = true, example = "1")
    public CommonResult<MemberUserRespVO> getMemberUser(@RequestParam("id") Long id) {
        MemberUserDO user = memberUserService.getUser(id);
        if (user == null) {
            throw exception(MEMBER_USER_NOT_EXISTS);
        }
        return success(BeanUtils.toBean(user, MemberUserRespVO.class));
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改会员状态")
    public CommonResult<Boolean> updateMemberUserStatus(@Valid @RequestBody MemberUserUpdateStatusReqVO reqVO) {
        memberUserService.updateUserStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @PutMapping("/reset-password")
    @Operation(summary = "重置会员密码")
    public CommonResult<Boolean> resetMemberUserPassword(@Valid @RequestBody MemberUserResetPasswordReqVO reqVO) {
        memberUserService.resetUserPassword(reqVO.getId(), reqVO.getPassword());
        return success(true);
    }
}
