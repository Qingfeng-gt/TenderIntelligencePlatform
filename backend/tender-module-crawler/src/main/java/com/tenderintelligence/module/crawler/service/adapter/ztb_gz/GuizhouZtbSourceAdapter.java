package com.tenderintelligence.module.crawler.service.adapter.ztb_gz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerSiteDO;
import com.tenderintelligence.module.crawler.service.CrawlStats;
import com.tenderintelligence.module.crawler.service.NoticeUpsertService;
import com.tenderintelligence.module.crawler.service.adapter.SourceAdapter;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
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
    private NoticeUpsertService noticeUpsertService;

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

        // 3) 频道循环(JSON: [{path: noticeType查询值, name, type, pageCount}])
        List<ChannelDef> channels = parseChannels(site.getChannels());
        for (ChannelDef channel : channels) {
            // 列表 → 收集详情 URL(先全部列表页, 再统一抓详情, 与 Ccgp 流程一致)
            Set<String> detailUrls = new LinkedHashSet<>();
            boolean listEmpty = false;
            for (int page = 1; page <= channel.pageCount; page++) {
                String listUrl = baseUrl + SEARCH_PATH
                        + "?pubDate=all&pubType=all&region=all&industry=all&prjType=all"
                        + "&noticeType=" + channel.noticeType + "&noticeClassify=all"
                        + "&pageIndex=" + page + "&args=";
                String listJson = fetch(client, listUrl, ua, xhrReferer, true);
                stats.setListFetched(stats.getListFetched() + 1);
                if (listJson == null) {
                    continue;
                }
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
                log.info("[crawler] ztb_gz 频道 {} 第{}页: 解析出 {} 条", channel.name, page, parsed);
                if (page < channel.pageCount) {
                    Thread.sleep(intervalMs);
                }
            }
            if (listEmpty) {
                log.info("[crawler] ztb_gz 频道 {}: 源站 search 返回 0 条(该站列表数据可能未对外开放/已下线)",
                        channel.name);
            }

            // 4) 详情页逐条抓取+解析+入库
            int fetched = 0;
            int idx = 0;
            for (String detailUrl : detailUrls) {
                if (idx > 0 || fetched > 0) {
                    Thread.sleep(intervalMs);
                }
                idx++;
                String detailJson = fetch(client, detailUrl, ua, xhrReferer, true);
                if (detailJson == null) {
                    // 重试一次(限流场景)
                    Thread.sleep(intervalMs);
                    detailJson = fetch(client, detailUrl, ua, xhrReferer, true);
                }
                if (detailJson == null) {
                    stats.setDetailFailed(stats.getDetailFailed() + 1);
                    continue;
                }
                fetched++;
                try {
                    JsonNode detail = objectMapper.readTree(detailJson);
                    String id = detail.path("Id").asText("");
                    NoticePortalDO notice = GuizhouZtbNoticeParser.parse(
                            detail, channel.type, baseUrl + BULLETIN_PATH + id, site.getName());
                    noticeUpsertService.upsert(notice, stats);
                } catch (Exception ex) {
                    stats.setDetailFailed(stats.getDetailFailed() + 1);
                    log.warn("[crawler] ztb_gz 解析失败 {}: {}", detailUrl, ex.getMessage());
                }
            }
            stats.setDetailFetched(stats.getDetailFetched() + fetched);
            log.info("[crawler] ztb_gz 频道 {} 完成: 详情{}条/失败{}条", channel.name, fetched, stats.getDetailFailed());
        }
    }

    /** GET 请求(JSON: 带 Referer + X-Requested-With), 失败或非 2xx 返回 null */
    private String fetch(HttpClient client, String url, String ua, String referer, boolean xhr) {
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
                log.warn("[crawler] HTTP {}: {}", response.statusCode(), url);
                return null;
            }
            return new String(response.body(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.warn("[crawler] 请求失败 {}: {}", url, ex.getMessage());
            return null;
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

    private List<ChannelDef> parseChannels(String json) {
        List<ChannelDef> result = new ArrayList<>();
        try {
            JsonNode array = objectMapper.readTree(json != null ? json : "[]");
            for (JsonNode node : array) {
                ChannelDef def = new ChannelDef();
                def.noticeType = node.has("path") ? node.get("path").asText() : "all";
                def.name = node.has("name") ? node.get("name").asText() : def.noticeType;
                def.type = node.has("type") ? node.get("type").asText() : "tender";
                def.pageCount = node.has("pageCount") ? node.get("pageCount").asInt(1) : 1;
                result.add(def);
            }
        } catch (Exception ex) {
            log.warn("[crawler] channels 配置解析失败: {}", ex.getMessage());
        }
        if (result.isEmpty()) {
            // 兜底: 全部公告
            ChannelDef def = new ChannelDef();
            def.noticeType = "all";
            def.name = "全部公告";
            def.type = "tender";
            def.pageCount = 1;
            result.add(def);
        }
        return result;
    }

    /** 频道配置 */
    private static class ChannelDef {
        private String noticeType;
        private String name;
        private String type;
        private int pageCount;
    }
}
