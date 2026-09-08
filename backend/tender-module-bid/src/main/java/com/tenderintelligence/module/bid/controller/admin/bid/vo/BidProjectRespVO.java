package com.tenderintelligence.module.bid.controller.admin.bid.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 投标项目 Response VO")
@Data
public class BidProjectRespVO {

    @Schema(description = "投标项目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "发起用户", example = "1")
    private Long userId;

    @Schema(description = "源公告编号(notice.id)", example = "100")
    private Long noticeId;

    @Schema(description = "项目名称", example = "2026年市政道路提升改造工程")
    private String projectName;

    @Schema(description = "投标截止时间")
    private LocalDateTime deadline;

    @Schema(description = "拟投标金额(万元)", example = "1250.50")
    private BigDecimal bidAmount;

    @Schema(description = "投标文件名(演示文本)", example = "标书-市政道路改造.docx")
    private String bidFileName;

    @Schema(description = "状态:SUBMITTED已递交/OTB待开标/WON中标/LOST未中标/ABANDONED放弃")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
