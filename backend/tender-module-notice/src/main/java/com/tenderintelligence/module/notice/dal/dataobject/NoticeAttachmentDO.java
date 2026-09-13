package com.tenderintelligence.module.notice.dal.dataobject;

import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 标讯附件 DO(一条公告可有多个附件)
 *
 * 只保存**源站附件直链与元信息**, 不保存文件本体 —— 附件由投标人点链接回源站下载,
 * 平台不做文件的二次分发(边界见 doc/技术/06-数据采集设计.md §十)。
 * 数据由爬虫在解析详情页时写入, 见 NoticeUpsertService。
 */
@TableName("notice_attachment")
@KeySequence("notice_attachment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NoticeAttachmentDO extends BaseDO {

    /**
     * 附件编号
     */
    @TableId
    private Long id;
    /**
     * 公告编号(notice.id)
     */
    private Long noticeId;
    /**
     * 附件文件名(取自源站锚文本)
     */
    private String fileName;
    /**
     * 源站附件直链(绝对 URL)
     */
    private String fileUrl;
    /**
     * 文件类型(pdf/doc/docx/xls/xlsx/zip 等, 小写不含点)
     */
    private String fileType;
    /**
     * 源站标注的文件大小(源站为自由文本, 如 "546.3K"/"3.7M"; 源站未标注则为空)
     */
    private String fileSize;
    /**
     * 同一公告内的展示顺序(按源站出现次序)
     */
    private Integer sort;
}
