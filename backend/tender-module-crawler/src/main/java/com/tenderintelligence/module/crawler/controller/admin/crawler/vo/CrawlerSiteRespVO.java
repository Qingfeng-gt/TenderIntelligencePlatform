package com.tenderintelligence.module.crawler.controller.admin.crawler.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 爬虫站点 Response VO")
@Data
public class CrawlerSiteRespVO {

    @Schema(description = "站点编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "站点名称", example = "中国政府采购网")
    private String name;

    @Schema(description = "站点标识(对应 SourceAdapter)", example = "ccgp")
    private String code;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "频道JSON")
    private String channels;

    @Schema(description = "详情页请求间隔(ms)")
    private Long intervalMs;
}
