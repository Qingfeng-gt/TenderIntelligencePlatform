package com.tenderintelligence.module.notice.api.notice.dto;

import lombok.Data;

/**
 * 公告附件 DTO（读取侧）
 */
@Data
public class NoticeAttachmentRespDTO {

    /**
     * 附件编号
     */
    private Long id;

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
     * 文件大小（源站给的展示值，字符串）
     */
    private String fileSize;

    /**
     * 排序
     */
    private Integer sort;

}
