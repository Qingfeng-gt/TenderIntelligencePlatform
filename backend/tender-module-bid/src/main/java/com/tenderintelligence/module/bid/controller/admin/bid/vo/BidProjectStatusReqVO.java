package com.tenderintelligence.module.bid.controller.admin.bid.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 投标项目状态流转 Request VO")
@Data
public class BidProjectStatusReqVO {

    @Schema(description = "投标项目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "投标项目编号不能为空")
    private Long id;

    @Schema(description = "目标状态:OTB待开标/WON中标/LOST未中标/ABANDONED放弃",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "OTB")
    @NotEmpty(message = "目标状态不能为空")
    private String status;
}
