package com.tenderintelligence.module.bid.controller.admin.bid.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 投标项目创建 Request VO")
@Data
public class BidProjectCreateReqVO {

    @Schema(description = "源公告编号(notice.id)", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "公告编号不能为空")
    private Long noticeId;

    @Schema(description = "拟投标金额(万元)", example = "1250.50")
    private BigDecimal bidAmount;

    @Schema(description = "投标文件名(演示文本)", example = "标书-市政道路改造.docx")
    private String bidFileName;

    @Schema(description = "备注")
    private String remark;
}
