package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import com.tenderintelligence.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 公告分页查询条件（四层模型）
 */
@Schema(description = "管理后台 - 招投标公告分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NoticePageReqVO extends PageParam {

    @Schema(description = "关键词（标题 / 项目编号 / 公告编号模糊）", example = "市政")
    private String keyword;

    @Schema(description = "公告类型代码：tender / win / change / explore / qualify", example = "tender")
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

    @Schema(description = "发布时间 ≥（含）", example = "2026-09-07 00:00:00")
    private LocalDateTime publishTimeGreaterThan;

    @Schema(description = "发布时间 <（不含）", example = "2026-10-07 00:00:00")
    private LocalDateTime publishTimeLessThan;
}
