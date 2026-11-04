package com.tenderintelligence.module.crawler.service;

import com.tenderintelligence.module.crawler.dal.dataobject.CrawlRawDO;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerFetchLogDO;
import com.tenderintelligence.module.crawler.dal.mysql.CrawlRawMapper;
import com.tenderintelligence.module.crawler.dal.mysql.CrawlerFetchLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

/**
 * 采集遥测：逐 URL 抓取日志 + 原始页面快照
 *
 * <p>写入 {@code crawler_fetch_log} 与 {@code crawl_raw}。遥测失败只记 WARN，不阻断采集主链路。
 */
@Service
@Slf4j
public class CrawlTelemetryService {

    private static final String DEFAULT_PARSER_VERSION = "CRAWL-V2";
    private static final int MAX_ERROR_LENGTH = 500;

    @Resource
    private CrawlerFetchLogMapper fetchLogMapper;
    @Resource
    private CrawlRawMapper crawlRawMapper;

    /**
     * 记录一次 URL 抓取
     */
    public void recordFetch(Long taskId, String siteCode, String channelPath, String pageType,
                            String url, Integer httpStatus, long durationMs, int retryCount,
                            String status, String errorType, String errorMsg) {
        try {
            CrawlerFetchLogDO logRow = new CrawlerFetchLogDO();
            logRow.setTaskId(taskId == null ? 0L : taskId);
            logRow.setSiteCode(safe(siteCode));
            logRow.setChannelPath(safe(channelPath));
            logRow.setPageType(safe(pageType));
            logRow.setUrl(truncate(safe(url), 1024));
            logRow.setHttpStatus(httpStatus);
            logRow.setDurationMs((int) Math.min(Integer.MAX_VALUE, Math.max(0, durationMs)));
            logRow.setRetryCount(Math.max(0, retryCount));
            logRow.setStatus(safe(status));
            logRow.setErrorType(truncate(safe(errorType), 32));
            logRow.setErrorMsg(truncate(safe(errorMsg), MAX_ERROR_LENGTH));
            logRow.setFetchedAt(LocalDateTime.now());
            fetchLogMapper.insert(logRow);
        } catch (Exception ex) {
            log.warn("[crawler] 写 crawler_fetch_log 失败: {}", ex.getMessage());
        }
    }

    /**
     * 保存原始页面快照
     */
    public void recordRaw(Long taskId, String siteCode, String url, String pageType,
                          String content, String parserVersion, String parseStatus) {
        if (content == null || content.isEmpty()) {
            return;
        }
        try {
            String sourceUrl = truncate(safe(url), 1024);
            String contentHash = sha256Hex(content);
            // 同一 URL 的同一内容只留一份，避免每轮重采把原始层撑爆
            if (crawlRawMapper.existsByUrlAndHash(sourceUrl, contentHash)) {
                return;
            }
            CrawlRawDO raw = new CrawlRawDO();
            raw.setTaskId(taskId);
            raw.setSiteCode(safe(siteCode));
            raw.setSourceUrl(sourceUrl);
            raw.setPageType(safe(pageType));
            raw.setContentHash(contentHash);
            raw.setContent(content);
            raw.setContentEncoding("utf-8");
            raw.setParserVersion(parserVersion == null || parserVersion.isBlank()
                    ? DEFAULT_PARSER_VERSION : parserVersion);
            raw.setParseStatus(parseStatus == null || parseStatus.isBlank() ? "PARSED" : parseStatus);
            raw.setFetchedAt(LocalDateTime.now());
            crawlRawMapper.insert(raw);
        } catch (Exception ex) {
            log.warn("[crawler] 写 crawl_raw 失败: {}", ex.getMessage());
        }
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(Character.forDigit((b >> 4) & 0xF, 16));
                builder.append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
