package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 行政区划选项
 */
@Schema(description = "管理后台 - 行政区划 Response VO")
@Data
public class RegionRespVO {

    @Schema(description = "行政区划代码", example = "420000")
    private String code;
    @Schema(description = "上级代码，省级为空")
    private String parentCode;
    @Schema(description = "层级：1 省 / 2 市 / 3 区县", example = "1")
    private Integer level;
    @Schema(description = "名称", example = "湖北省")
    private String name;
    @Schema(description = "简称", example = "湖北")
    private String shortName;
    @Schema(description = "全称", example = "湖北省")
    private String fullName;
}
