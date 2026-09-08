package com.tenderintelligence.module.member.controller.app.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户端 - 会员信息 Response VO")
@Data
public class MemberProfileRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "demo123")
    private String username;

    @Schema(description = "昵称", example = "张工")
    private String nickname;

    @Schema(description = "手机号码", example = "13800000000")
    private String mobile;

    @Schema(description = "头像", example = "https://...")
    private String avatar;

}
