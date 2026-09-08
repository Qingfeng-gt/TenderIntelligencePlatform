package com.tenderintelligence.module.member.controller.app.auth.vo;

import com.tenderintelligence.module.system.controller.admin.auth.vo.CaptchaVerificationReqVO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Schema(description = "用户端 - 注册 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRegisterReqVO extends CaptchaVerificationReqVO {

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "demo123")
    @NotEmpty(message = "注册账号不能为空")
    @Length(min = 4, max = 30, message = "账号长度为 4-30 位")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotEmpty(message = "密码不能为空")
    @Length(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;

    @Schema(description = "昵称", example = "张工")
    @Length(max = 30, message = "昵称长度不能超过 30 位")
    private String nickname;

}
