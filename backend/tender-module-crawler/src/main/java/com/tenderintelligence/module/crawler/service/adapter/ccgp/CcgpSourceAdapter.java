package com.tenderintelligence.module.crawler.service.adapter.ccgp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerChannelDO;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerSiteDO;
import com.tenderintelligence.module.crawler.dal.mysql.CrawlerChannelMapper;
import com.tenderintelligence.module.crawler.service.CrawlStats;
import com.tenderintelligence.module.crawler.service.CrawlTelemetryService;
import com.tenderintelligence.module.crawler.service.adapter.SourceAdapter;
import com.tenderintelligence.module.notice.api.notice.NoticeApi;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeDTO;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeUpsertRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中国政府采购网(www.ccgp.gov.cn)数据源适配器
 *
 * 实现要点(2026-09-08 / 2026-09-11 实测):
 * 1. 必须先用浏览器 UA 访问首页拿 Cookie,再访问列表/详情(否则 403/频繁访问限流)
 * 2. 列表页为静态 HTML: <a href="./202609/t20260908_27283358.htm" title="标题">
 *    两个分组(cggg/dfgg 地方公告、cggg/zygg 中央公告)列表页结构一致, 每页 20 条, 分页 index_N.htm
 * 3. 详情页有两个模板, 由频道配置的 template 字段选择解析器:
 *    dfgg → {@link CcgpNoticeParser}(#noticeArea);zygg → {@link CcgpZyggNoticeParser}(表单表格)
 * 4. 详情页请求间隔 ≥ site.intervalMs(默认 3s),防反爬
 * 5. 源站 CDN 会间歇性返回 200 + 0 字节, fetch() 按失败处理并重试, 详见 {@link #fetch}
 */
@Slf4j
@Component
public class CcgpSourceAdapter implements SourceAdapter {

    /** 站点标识 */
    private static final String CODE = "ccgp";
    /** 默认 UA */
    private static final String DEFAULT_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    /** 列表项链接: <a href="./202609/t20260908_27283358.htm" title="标题"> */
    private static final Pattern LIST_ITEM =
            Pattern.compile("<a href=\"(\\./[^\"?]+\\.htm)\"[^>]*title=\"([^\"]+)\"");
    /** 详情页模板标识(与 crawler_site.channels[].template 对应) */
    private static final String TEMPLATE_DFGG = "dfgg";
    private static final String TEMPLATE_ZYGG = "zygg";

    @Resource
    private NoticeApi noticeApi;
    @Resource
    private CrawlerChannelMapper crawlerChannelMapper;
    @Resource
    private CrawlTelemetryService crawlTelemetryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public void crawl(CrawlerSiteDO site, CrawlStats stats) throws Exception {
        // 1) 站点配置
        JsonNode configNode = parseFirstJson(site.getConfig());
        String baseUrl = configNode != null && configNode.has("baseUrl")
                ? configNode.get("baseUrl").asText() : "http://www.ccgp.gov.cn";
        String initialUrl = configNode != null && configNode.has("initialUrl")
                ? configNode.get("initialUrl").asText() : baseUrl + "/";
        String ua = configNode != null && configNode.has("userAgent")
                ? configNode.get("userAgent").asText() : DEFAULT_UA;
        long intervalMs = site.getIntervalMs() == null ? 3000 : site.getIntervalMs();

        // 2) HTTP 会话(本任务一个 CookieManager, 首页 → 列表 → 详情共享)
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        String referer = null;
        fetch(client, initialUrl, ua, referer);
        log.info("[crawler] ccgp 会话初始化完成: {}", initialUrl);

        // 3) 频道循环（crawler_channel 表，替代旧 crawler_site.channels JSON）
        List<CrawlerChannelDO> channels = crawlerChannelMapper.selectEnabledBySiteCode(site.getCode());
        if (channels.isEmpty()) {
            throw new IllegalStateException("ccgp 未配置任何启用频道(crawler_channel)");
        }
        for (CrawlerChannelDO channel : channels) {
            String channelPath = channel.getPath();
            String channelType = firstNonBlank(channel.getNoticeTypeCode(), "tender");
            int pageCount = channel.getPageCount() == null ? 1 : Math.max(1, channel.getPageCount());
            String template = channelTemplate(channel);
            Set<String> candidateUrls = new LinkedHashSet<>();
            for (int page = 1; page <= pageCount; page++) {
                String listUrl = baseUrl + channelPath + (page == 1 ? "" : "index_" + page + ".htm");
                long start = System.currentTimeMillis();
                String listHtml = fetch(client, listUrl, ua, referer);
                long cost = System.currentTimeMillis() - start;
                crawlTelemetryService.recordFetch(stats.getTaskId(), site.getCode(), channelPath, "LIST",
                        listUrl, listHtml == null ? null : 200, cost, 0,
                        listHtml == null ? "FAILED" : "SUCCESS",
                        listHtml == null ? "FETCH_FAILED" : "", "");
                if (listHtml == null) {
                    log.warn("[crawler] 列表页抓取失败: {}", listUrl);
                    continue;
                }
                crawlTelemetryService.recordRaw(stats.getTaskId(), site.getCode(), listUrl, "LIST",
                        listHtml, "CRAWL-V2", "PARSED");
                stats.setListFetched(stats.getListFetched() + 1);
                Matcher matcher = LIST_ITEM.matcher(listHtml);
                int parsed = 0;
                while (matcher.find()) {
                    // 相对路径解析: ./202609/t*.htm → baseUrl + channelPath + 202609/t*.htm
                    candidateUrls.add(baseUrl + channelPath + matcher.group(1).substring(2));
                    parsed++;
                }
                stats.setListParsed(stats.getListParsed() + parsed);
                referer = listUrl;
                log.info("[crawler] 频道 {} 第{}页, 解析出 {} 条公告", channelPath, page, parsed);
            }

            // 4) 详情页逐条抓取+解析+入库
            int fetched = 0;
            for (String detailUrl : candidateUrls) {
                // 限速(防反爬)
                if (fetched > 0) {
                    Thread.sleep(intervalMs);
                }
                long start = System.currentTimeMillis();
                String detailHtml = fetch(client, detailUrl, ua, referer);
                long cost = System.currentTimeMillis() - start;
                crawlTelemetryService.recordFetch(stats.getTaskId(), site.getCode(), channelPath, "DETAIL",
                        detailUrl, detailHtml == null ? null : 200, cost, 0,
                        detailHtml == null ? "FAILED" : "SUCCESS",
                        detailHtml == null ? "FETCH_FAILED" : "", "");
                if (detailHtml == null) {
                    stats.setDetailFailed(stats.getDetailFailed() + 1);
                    continue;
                }
                crawlTelemetryService.recordRaw(stats.getTaskId(), site.getCode(), detailUrl, "DETAIL",
                        detailHtml, "CRAWL-V2", "PARSED");
                fetched++;
                // 解析与入库分两段捕获: 两者的排查方向完全不同(字段锚点 vs 表结构/约束),
                // 合成一条"解析失败"日志会把库层错误(如 Data too long)误报成解析问题。
                NoticeDTO notice;
                try {
                    // 两个分组的详情页模板不同: 地方公告见 CcgpNoticeParser, 中央公告见 CcgpZyggNoticeParser
                    notice = TEMPLATE_ZYGG.equals(template)
                            ? CcgpZyggNoticeParser.parse(detailHtml, channelType, detailUrl, site.getName())
                            : CcgpNoticeParser.parse(detailHtml, channelType, detailUrl, site.getName());
                } catch (Exception ex) {
                    stats.setDetailFailed(stats.getDetailFailed() + 1);
                    log.warn("[crawler] 解析失败 {}: [{}] {}", detailUrl, ex.getClass().getSimpleName(), ex.getMessage());
                    continue;
                }
                try {
                    NoticeUpsertRespDTO result = noticeApi.upsert(notice);
                    if (Boolean.TRUE.equals(result.getCreated())) {
                        stats.setNewInsert(stats.getNewInsert() + 1);
                    } else if (Boolean.TRUE.equals(result.getUpdated())) {
                        stats.setUpdated(stats.getUpdated() + 1);
                    }
                } catch (Exception ex) {
                    stats.setDetailFailed(stats.getDetailFailed() + 1);
                    log.warn("[crawler] 入库失败 {}: [{}] {}", detailUrl, ex.getClass().getSimpleName(), ex.getMessage());
                }
            }
            stats.setDetailFetched(stats.getDetailFetched() + fetched);
            log.info("[crawler] 频道 {} 完成: 详情{}条/失败{}条", channelPath, fetched, stats.getDetailFailed());
        }
    }

    private String channelTemplate(CrawlerChannelDO channel) {
        if (channel.getConfig() == null || channel.getConfig().isBlank()) {
            return TEMPLATE_DFGG;
        }
        try {
            JsonNode config = objectMapper.readTree(channel.getConfig());
            return config.has("template") ? config.get("template").asText() : TEMPLATE_DFGG;
        } catch (Exception ex) {
            log.warn("[crawler] 频道 {} config 解析失败: {}", channel.getPath(), ex.getMessage());
            return TEMPLATE_DFGG;
        }
    }

    private static String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : fallback;
    }

    /**
     * GET 请求, 失败、非 2xx 或响应体为空均返回 null
     *
     * 空响应体重试一次: 2026-09-11 实测源站 CDN(openresty, 响应头带 x-via)会间歇性返回
     * 200 + 0 字节。若只判状态码, 这种情况会被当作抓取成功 —— 列表页记一次 listFetched
     * 却解析出 0 条, 整页公告被静默丢掉(与"漏标"同类)。故空响应体按失败处理并重试。
     */
    private String fetch(HttpClient client, String url, String ua, String referer) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", ua)
                        .header("Accept-Language", "zh-CN,zh;q=0.9")
                        .GET().timeout(Duration.ofSeconds(20));
                if (referer != null && !referer.isEmpty()) {
                    builder.header("Referer", referer);
                }
                HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    // 非 2xx 为确定性失败, 不重试
                    log.warn("[crawler] HTTP {}: {}", response.statusCode(), url);
                    return null;
                }
                String body = new String(response.body(), java.nio.charset.StandardCharsets.UTF_8);
                if (!body.isBlank()) {
                    return body;
                }
                log.warn("[crawler] HTTP 200 但响应体为空(第{}次): {}", attempt, url);
            } catch (Exception ex) {
                log.warn("[crawler] 请求失败(第{}次) {}: {}", attempt, url, ex.getMessage());
            }
            if (attempt == 1) {
                sleepQuietly(1000);
            }
        }
        return null;
    }

    /** 静默休眠(中断时恢复中断标记, 不吞掉) */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private JsonNode parseFirstJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }

}
