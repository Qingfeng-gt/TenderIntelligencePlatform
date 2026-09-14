package com.tenderintelligence.module.notice.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 标讯附件 Response VO
 *
 * 只有源站直链与元信息, 没有平台侧下载地址 —— 平台不保存文件本体, 附件由用户点链接
 * 到源站下载(边界见 doc/技术/06-数据采集设计.md §十)。
 */
@Schema(description = "管理后台 - 标讯附件 Response VO")
@Data
public class NoticeAttachmentRespVO {

    @Schema(description = "附件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "附件文件名", example = "XX项目采购文件.pdf")
    private String fileName;

    @Schema(description = "源站附件直链", example = "https://sx2gov2open2doc.uos.sxzfcg.zcygov.cn/1024FPA/…/xx.pdf")
    private String fileUrl;

    @Schema(description = "文件类型(pdf/doc/docx/xls/xlsx/zip 等)", example = "pdf")
    private String fileType;

    @Schema(description = "源站标注的文件大小(源站自由文本,如 546.3K;源站未标注为空)", example = "546.3K")
    private String fileSize;

    @Schema(description = "同一公告内的展示顺序", example = "0")
    private Integer sort;
}
