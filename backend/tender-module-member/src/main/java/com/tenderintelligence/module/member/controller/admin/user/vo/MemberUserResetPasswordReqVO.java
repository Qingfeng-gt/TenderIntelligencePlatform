package com.tenderintelligence.module.member.controller.admin.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理后台 - 重置会员密码 Request VO
 */
@Schema(description = "管理后台 - 重置会员密码 Request VO")
@Data
public class MemberUserResetPasswordReqVO {

    @Schema(description = "会员编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "会员编号不能为空")
    private Long id;

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 4, max = 32, message = "密码长度为 4-32 位")
    private String password;
}
