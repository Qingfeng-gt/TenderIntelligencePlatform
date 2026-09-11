package com.tenderintelligence.module.crawler.service.adapter.ccgp;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中国政府采购网(www.ccgp.gov.cn)数据源适配器
 *
 * 实现要点(2026-09-08 实测):
 * 1. 必须先用浏览器 UA 访问首页拿 Cookie,再访问列表/详情(否则 403/频繁访问限流)
 * 2. 列表页为静态 HTML: <a href="./202609/t20260908_27283358.htm" title="标题">
 * 3. 详情页字段解析见 {@link CcgpNoticeParser}
 * 4. 详情页请求间隔 ≥ site.intervalMs(默认 3s),防反爬
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

        // 3) 频道循环(JSON: [{path,name,type,pageCount}])
        List<ChannelDef> channels = parseChannels(site.getChannels());
        for (ChannelDef channel : channels) {
            Set<String> candidateUrls = new LinkedHashSet<>();
            for (int page = 1; page <= channel.pageCount; page++) {
                String listUrl = baseUrl + channel.path + (page == 1 ? "" : "index_" + page + ".htm");
                String listHtml = fetch(client, listUrl, ua, referer);
                if (listHtml == null) {
                    log.warn("[crawler] 列表页抓取失败: {}", listUrl);
                    continue;
                }
                stats.setListFetched(stats.getListFetched() + 1);
                Matcher matcher = LIST_ITEM.matcher(listHtml);
                int parsed = 0;
                while (matcher.find()) {
                    // 相对路径解析: ./202609/t*.htm → baseUrl + channelPath + 202609/t*.htm
                    candidateUrls.add(baseUrl + channel.path + matcher.group(1).substring(2));
                    parsed++;
                }
                stats.setListParsed(stats.getListParsed() + parsed);
                referer = listUrl;
                log.info("[crawler] 频道 {} 第{}页, 解析出 {} 条公告", channel.path, page, parsed);
            }

            // 4) 详情页逐条抓取+解析+入库
            int fetched = 0;
            for (String detailUrl : candidateUrls) {
                // 限速(防反爬)
                if (fetched > 0) {
                    Thread.sleep(intervalMs);
                }
                String detailHtml = fetch(client, detailUrl, ua, referer);
                if (detailHtml == null) {
                    stats.setDetailFailed(stats.getDetailFailed() + 1);
                    continue;
                }
                fetched++;
                try {
                    NoticePortalDO notice = CcgpNoticeParser.parse(detailHtml, channel.type, detailUrl, site.getName());
                    noticeUpsertService.upsert(notice, stats);
                } catch (Exception ex) {
                    stats.setDetailFailed(stats.getDetailFailed() + 1);
                    log.warn("[crawler] 解析失败 {}: {}", detailUrl, ex.getMessage());
                }
            }
            stats.setDetailFetched(stats.getDetailFetched() + fetched);
            log.info("[crawler] 频道 {} 完成: 详情{}条/失败{}条", channel.path, fetched, stats.getDetailFailed());
        }
    }

    /** GET 请求, 失败或非 2xx 返回 null */
    private String fetch(HttpClient client, String url, String ua, String referer) {
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
                def.path = node.get("path").asText();
                def.type = node.has("type") ? node.get("type").asText() : "tender";
                def.pageCount = node.has("pageCount") ? node.get("pageCount").asInt(1) : 1;
                result.add(def);
            }
        } catch (Exception ex) {
            log.warn("[crawler] channels 配置解析失败: {}", ex.getMessage());
        }
        if (result.isEmpty()) {
            // 兜底: 默认公开招标频道
            ChannelDef def = new ChannelDef();
            def.path = "/cggg/dfgg/gkzb/";
            def.type = "tender";
            def.pageCount = 1;
            result.add(def);
        }
        return result;
    }

    /** 频道配置 */
    private static class ChannelDef {
        private String path;
        private String type;
        private int pageCount;
    }
}
