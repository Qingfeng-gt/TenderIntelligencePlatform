package com.tenderintelligence.module.member.controller.admin.user.vo;

import com.tenderintelligence.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 管理后台 - 会员用户分页 Request VO
 */
@Schema(description = "管理后台 - 会员用户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MemberUserPageReqVO extends PageParam {

    @Schema(description = "账号（模糊）", example = "demo")
    private String username;

    @Schema(description = "昵称（模糊）", example = "演示")
    private String nickname;

    @Schema(description = "手机号（模糊）", example = "138")
    private String mobile;

    @Schema(description = "状态：0 正常 / 1 停用", example = "0")
    private Integer status;
}
