package com.tenderintelligence.module.crawler.controller.admin.crawler.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 采集任务 Response VO")
@Data
public class CrawlerTaskRespVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long id;

    @Schema(description = "站点标识", example = "ccgp")
    private String siteCode;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "状态:RUNNING/SUCCESS/FAILED")
    private String status;

    @Schema(description = "列表页抓取数")
    private Integer listFetched;

    @Schema(description = "列表解析出公告数")
    private Integer listParsed;

    @Schema(description = "详情页抓取成功数")
    private Integer detailFetched;

    @Schema(description = "详情页抓取失败数")
    private Integer detailFailed;

    @Schema(description = "新增入库数")
    private Integer newInsert;

    @Schema(description = "变更更新数")
    private Integer updated;

    @Schema(description = "失败原因")
    private String errorMsg;
}
