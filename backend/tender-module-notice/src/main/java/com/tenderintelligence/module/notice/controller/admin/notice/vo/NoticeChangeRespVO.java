package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告澄清/修改记录
 */
@Schema(description = "管理后台 - 公告澄清修改 Response VO")
@Data
public class NoticeChangeRespVO {

    @Schema(description = "主键", example = "1")
    private Long id;
    @Schema(description = "变更类型代码")
    private String changeTypeCode;
    @Schema(description = "变更标题")
    private String changeTitle;
    @Schema(description = "变更内容")
    private String changeContent;
    @Schema(description = "变更时间")
    private LocalDateTime changeTime;
    @Schema(description = "变更公告 URL")
    private String sourceUrl;
}
