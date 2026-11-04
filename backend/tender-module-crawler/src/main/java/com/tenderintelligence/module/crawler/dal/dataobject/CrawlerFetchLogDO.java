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
 * 逐 URL 抓取日志
 *
 * <p>对应表 {@code crawler_fetch_log}。
 */
@TableName("crawler_fetch_log")
@KeySequence("crawler_fetch_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrawlerFetchLogDO extends BaseDO {

    @TableId
    private Long id;
    /** 任务 ID */
    private Long taskId;
    /** 站点 code */
    private String siteCode;
    /** 频道 */
    private String channelPath;
    /** LIST/DETAIL */
    private String pageType;
    /** 请求 URL */
    private String url;
    /** HTTP 状态 */
    private Integer httpStatus;
    /** 耗时（ms） */
    private Integer durationMs;
    /** 重试次数 */
    private Integer retryCount;
    /** SUCCESS/FAILED/SKIPPED */
    private String status;
    /** 超时/403/解析失败等 */
    private String errorType;
    /** 错误摘要 */
    private String errorMsg;
    /** 抓取时间 */
    private LocalDateTime fetchedAt;
}
