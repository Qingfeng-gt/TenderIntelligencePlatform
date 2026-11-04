package com.tenderintelligence.module.crawler.service.adapter.ztb_gz;

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

/**
 * 贵州省招标投标公共服务平台(ztb.guizhou.gov.cn)数据源适配器
 *
 * 数据流(2026-09-08 实测契约):
 * 1. 列表: GET /api/trade/search?pubDate=all&pubType=all&region=all&industry=all&prjType=all
 *    &noticeType={公告类别A01..Z04}&noticeClassify=all&pageIndex={n}&args=
 * 2. 详情: GET /api/trade/GetDetail/{Id}
 * 3. 字段映射: 详见 {@link GuizhouZtbNoticeParser}
 *
 * ⚠️ 2026-09-08 实测: search 接口对外返回 totalNum=0(源站列表数据下线),
 * GetDetail/{Id} 可用。源站恢复后本适配器零改动即可采集。
 * 需要 Cookie 会话(首页预热)+ Referer + X-Requested-With 头。
 */
@Slf4j
@Component
public class GuizhouZtbSourceAdapter implements SourceAdapter {

    /** 站点标识 */
    private static final String CODE = "ztb_gz";
    /** 默认 UA */
    private static final String DEFAULT_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    /** 列表查询前缀(noticeType 由频道决定) */
    private static final String SEARCH_PATH = "/api/trade/search";
    private static final String DETAIL_PATH = "/api/trade/GetDetail/";
    private static final String BULLETIN_PATH = "/trade/bulletin/?id=";

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
                ? configNode.get("baseUrl").asText() : "http://ztb.guizhou.gov.cn";
        String initialUrl = configNode != null && configNode.has("initialUrl")
                ? configNode.get("initialUrl").asText() : baseUrl + "/";
        String ua = configNode != null && configNode.has("userAgent")
                ? configNode.get("userAgent").asText() : DEFAULT_UA;
        String xhrReferer = configNode != null && configNode.has("xhrReferer")
                ? configNode.get("xhrReferer").asText() : baseUrl + "/trade/?category=affiche";
        long intervalMs = site.getIntervalMs() == null ? 3000 : site.getIntervalMs();

        // 2) HTTP 会话(首页预热拿 Cookie, 列表/详情共享会话)
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        if (fetch(client, initialUrl, ua, xhrReferer, false) == null) {
            log.warn("[crawler] ztb_gz 首页预热失败(不影响详情接口): {}", initialUrl);
        }

        // 3) 频道循环（crawler_channel.path = noticeType 查询值）
        List<CrawlerChannelDO> channels = crawlerChannelMapper.selectEnabledBySiteCode(site.getCode());
        if (channels.isEmpty()) {
            throw new IllegalStateException("ztb_gz 未配置任何启用频道(crawler_channel)");
        }
        for (CrawlerChannelDO channel : channels) {
            // 列表 → 收集详情 URL(先全部列表页, 再统一抓详情, 与 Ccgp 流程一致)
            Set<String> detailUrls = new LinkedHashSet<>();
            boolean listEmpty = false;
            int pageCount = channel.getPageCount() == null ? 1 : Math.max(1, channel.getPageCount());
            for (int page = 1; page <= pageCount; page++) {
                String listUrl = baseUrl + SEARCH_PATH
                        + "?pubDate=all&pubType=all&region=all&industry=all&prjType=all"
                        + "&noticeType=" + channel.getPath() + "&noticeClassify=all"
                        + "&pageIndex=" + page + "&args=";
                long start = System.currentTimeMillis();
                String listJson = fetch(client, listUrl, ua, xhrReferer, true);
                long cost = System.currentTimeMillis() - start;
                crawlTelemetryService.recordFetch(stats.getTaskId(), site.getCode(), channel.getPath(), "LIST",
                        listUrl, listJson == null ? null : 200, cost, 0,
                        listJson == null ? "FAILED" : "SUCCESS",
                        listJson == null ? "FETCH_FAILED" : "", "");
                stats.setListFetched(stats.getListFetched() + 1);
                if (listJson == null) {
                    continue;
                }
                crawlTelemetryService.recordRaw(stats.getTaskId(), site.getCode(), listUrl, "LIST",
                        listJson, "CRAWL-V2", "PARSED");
                int parsed = 0;
                try {
                    JsonNode root = objectMapper.readTree(listJson);
                    int totalNum = root.path("totalNum").asInt(0);
                    if (totalNum == 0) {
                        listEmpty = true;
                    }
                    JsonNode data = root.path("data");
                    if (data.isArray()) {
                        for (JsonNode item : data) {
                            // 列表项字段: Id / Title / BTypeName / RegionName(见源站 tradeviewmodel)
                            JsonNode idNode = item.get("Id");
                            if (idNode == null || idNode.isNull()) {
                                continue;
                            }
                            detailUrls.add(baseUrl + DETAIL_PATH + idNode.asText());
                            parsed++;
                        }
                    }
                } catch (Exception ex) {
                    log.warn("[crawler] ztb_gz 列表解析失败 {}: {}", listUrl, ex.getMessage());
                }
                stats.setListParsed(stats.getListParsed() + parsed);
                log.info("[crawler] ztb_gz 频道 {} 第{}页: 解析出 {} 条", channel.getName(), page, parsed);
                if (page < pageCount) {
                    Thread.sleep(intervalMs);
                }
            }
            if (listEmpty) {
                log.info("[crawler] ztb_gz 频道 {}: 源站 search 返回 0 条(该站列表数据可能未对外开放/已下线)",
                        channel.getName());
            }

            // 4) 详情页逐条抓取+解析+入库
            int fetched = 0;
            int idx = 0;
            for (String detailUrl : detailUrls) {
                if (idx > 0 || fetched > 0) {
                    Thread.sleep(intervalMs);
                }
                idx++;
                long start = System.currentTimeMillis();
                String detailJson = fetch(client, detailUrl, ua, xhrReferer, true);
                int retryCount = 0;
                if (detailJson == null) {
                    // 重试一次(限流场景)
                    Thread.sleep(intervalMs);
                    detailJson = fetch(client, detailUrl, ua, xhrReferer, true);
                    retryCount = 1;
                }
                long cost = System.currentTimeMillis() - start;
                crawlTelemetryService.recordFetch(stats.getTaskId(), site.getCode(), channel.getPath(), "DETAIL",
                        detailUrl, detailJson == null ? null : 200, cost, retryCount,
                        detailJson == null ? "FAILED" : "SUCCESS",
                        detailJson == null ? "FETCH_FAILED" : "", "");
                if (detailJson == null) {
                    stats.setDetailFailed(stats.getDetailFailed() + 1);
                    continue;
                }
                crawlTelemetryService.recordRaw(stats.getTaskId(), site.getCode(), detailUrl, "DETAIL",
                        detailJson, "CRAWL-V2", "PARSED");
                fetched++;
                try {
                    JsonNode detail = objectMapper.readTree(detailJson);
                    String id = detail.path("Id").asText("");
                    NoticeDTO notice = GuizhouZtbNoticeParser.parse(
                            detail, channel.getNoticeTypeCode(), baseUrl + BULLETIN_PATH + id, site.getName());
                    NoticeUpsertRespDTO result = noticeApi.upsert(notice);
                    if (Boolean.TRUE.equals(result.getCreated())) {
                        stats.setNewInsert(stats.getNewInsert() + 1);
                    } else if (Boolean.TRUE.equals(result.getUpdated())) {
                        stats.setUpdated(stats.getUpdated() + 1);
                    }
                } catch (Exception ex) {
                    // 同 CcgpSourceAdapter: 这里同时覆盖 JSON 解析与入库两步, 日志带上异常类型便于区分
                    stats.setDetailFailed(stats.getDetailFailed() + 1);
                    log.warn("[crawler] ztb_gz 解析或入库失败 {}: [{}] {}",
                            detailUrl, ex.getClass().getSimpleName(), ex.getMessage());
                }
            }
            stats.setDetailFetched(stats.getDetailFetched() + fetched);
            log.info("[crawler] ztb_gz 频道 {} 完成: 详情{}条/失败{}条", channel.getName(), fetched, stats.getDetailFailed());
        }
    }

    /**
     * GET 请求(JSON: 带 Referer + X-Requested-With), 失败、非 2xx 或响应体为空均返回 null
     *
     * 空响应体重试一次: 与 {@code CcgpSourceAdapter#fetch} 同因 —— CDN 会间歇性返回
     * 200 + 0 字节, 只判状态码会把整页列表静默丢掉。
     */
    private String fetch(HttpClient client, String url, String ua, String referer, boolean xhr) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", ua)
                        .header("Accept", "application/json, text/javascript, */*; q=0.01")
                        .header("Accept-Language", "zh-CN,zh;q=0.9");
                if (referer != null && !referer.isEmpty()) {
                    builder.header("Referer", referer);
                }
                if (xhr) {
                    builder.header("X-Requested-With", "XMLHttpRequest");
                }
                HttpResponse<byte[]> response = client.send(builder.GET().timeout(Duration.ofSeconds(20)).build(),
                        HttpResponse.BodyHandlers.ofByteArray());
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
