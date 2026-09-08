package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import com.tenderintelligence.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 招投标公告分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NoticePageReqVO extends PageParam {

    @Schema(description = "关键词(标题模糊)", example = "市政")
    private String keyword;

    @Schema(description = "公告类型:tender / win / change / explore", example = "tender")
    private String type;

    @Schema(description = "省份", example = "湖北省")
    private String province;

    @Schema(description = "行业", example = "市政工程")
    private String industry;

    @Schema(description = "发布时间大于等于(时间筛选)", example = "2026-09-07 00:00:00")
    private LocalDateTime publishTimeGreaterThan;
}
