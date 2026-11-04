package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import com.tenderintelligence.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目分页查询条件（project-centric）
 */
@Schema(description = "管理后台 - 项目分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProjectPageReqVO extends PageParam {

    @Schema(description = "关键词（项目名称 / 项目编号模糊）", example = "市政")
    private String keyword;

    @Schema(description = "公告类型代码；只返回包含该类型公告的项目", example = "tender")
    private String noticeTypeCode;

    @Schema(description = "省级行政区划代码", example = "420000")
    private String provinceCode;

    @Schema(description = "市级行政区划代码", example = "420100")
    private String cityCode;

    @Schema(description = "行业（规则抽取结果）", example = "市政工程")
    private String industry;

    @Schema(description = "预算下限（万元）", example = "100")
    private BigDecimal budgetMin;

    @Schema(description = "预算上限（万元）", example = "500")
    private BigDecimal budgetMax;

    @Schema(description = "最近公告时间 ≥（含）", example = "2026-09-07 00:00:00")
    private LocalDateTime publishTimeGreaterThan;

    @Schema(description = "最近公告时间 <（不含）", example = "2026-10-07 00:00:00")
    private LocalDateTime publishTimeLessThan;
}
