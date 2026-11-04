package com.tenderintelligence.module.member.controller.admin.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理后台 - 修改会员状态 Request VO
 */
@Schema(description = "管理后台 - 修改会员状态 Request VO")
@Data
public class MemberUserUpdateStatusReqVO {

    @Schema(description = "会员编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "会员编号不能为空")
    private Long id;

    @Schema(description = "状态：0 正常 / 1 停用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
