package com.tenderintelligence.module.notice.api.notice.dto;

import lombok.Data;

/**
 * 公告附件 DTO（写入侧）
 *
 * <p>由采集适配器解析出的附件，随 {@link NoticeDTO} 一起入库。注意<b>不带主键</b>——
 * 附件是整组重写的，主键由入库方生成。
 */
@Data
public class NoticeAttachmentDTO {

    /**
     * 文件名（源站展示名）
     */
    private String fileName;

    /**
     * 文件 URL
     */
    private String fileUrl;

    /**
     * 文件类型 / 后缀
     */
    private String fileType;

    /**
     * 文件大小（源站给的展示值，字符串，如 "1.2MB"）
     */
    private String fileSize;

    /**
     * 排序
     */
    private Integer sort;

}
