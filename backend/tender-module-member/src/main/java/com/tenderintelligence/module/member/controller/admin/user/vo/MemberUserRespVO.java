package com.tenderintelligence.module.member.controller.admin.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台 - 会员用户 Response VO
 *
 * <p>不返回密码字段。
 */
@Schema(description = "管理后台 - 会员用户 Response VO")
@Data
public class MemberUserRespVO {

    @Schema(description = "会员编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "demo123")
    private String username;

    @Schema(description = "昵称", example = "演示会员")
    private String nickname;

    @Schema(description = "手机号", example = "13800001111")
    private String mobile;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "状态：0 正常 / 1 停用", example = "0")
    private Integer status;

    @Schema(description = "最后登录 IP", example = "127.0.0.1")
    private String loginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime loginDate;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
