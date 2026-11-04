package com.tenderintelligence.module.crawler.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 原始页面快照
 *
 * <p>对应表 {@code crawl_raw}，只增不改，供规范化表重放解析。
 */
@TableName("crawl_raw")
@KeySequence("crawl_raw_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrawlRawDO extends BaseDO {

    @TableId
    private Long id;
    /** 任务 ID */
    private Long taskId;
    /** 站点 code */
    private String siteCode;
    /** 原始 URL */
    private String sourceUrl;
    /** LIST/DETAIL */
    private String pageType;
    /** 内容 SHA-256 */
    private String contentHash;
    /** 原始 HTML/JSON */
    private String content;
    /** 编码 */
    private String contentEncoding;
    /** 解析器版本 */
    private String parserVersion;
    /** PENDING/PARSED/FAILED */
    private String parseStatus;
    /** 抓取时间 */
    private LocalDateTime fetchedAt;
}
