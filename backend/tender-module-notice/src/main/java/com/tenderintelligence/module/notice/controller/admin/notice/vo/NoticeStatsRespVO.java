package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 门户首页统计
 */
@Schema(description = "管理后台 - 门户首页统计 Response VO")
@Data
public class NoticeStatsRespVO {

    @Schema(description = "今日新增公告数", example = "120")
    private Long todayCount;
    @Schema(description = "公告总数", example = "728")
    private Long totalCount;
    @Schema(description = "覆盖省份数", example = "20")
    private Long provinces;
    @Schema(description = "行业分类数", example = "12")
    private Long industries;
    @Schema(description = "项目总数", example = "706")
    private Long projects;
}
