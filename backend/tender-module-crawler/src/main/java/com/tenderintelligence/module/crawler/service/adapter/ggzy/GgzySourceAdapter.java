package com.tenderintelligence.module.crawler.service.adapter.ggzy;

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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全国公共资源交易平台(www.ggzy.gov.cn)数据源适配器
 *
 * 数据流(2026-09-14 实测契约):
 * <ol>
 *   <li>列表: POST {@code /information/pubTradingInfo/getTradList}, 表单
 *       {@code DEAL_TIME=01&DEAL_CLASSIFY={类别码}&PAGENUMBER={n}}<br>
 *       —— <b>{@code DEAL_TIME} 与 {@code DEAL_CLASSIFY} 均为必填</b>: 缺 DEAL_TIME 接口直接返回
 *       <b>404</b>(不是空列表); DEAL_CLASSIFY 取 {@code 00}「全部」返回 0 条, 故必须按类别分频道</li>
 *   <li>详情: 列表给的 {@code url} 是 {@code /html/a/} 外壳页, 正文在同路径的 {@code /html/b/} 页
 *       (服务端直出, 见 {@link GgzyHtmlSupport})</li>
 *   <li>字段映射: 见 {@link GgzyNoticeParser}</li>
 * </ol>
 *
 * ⚠️ <b>该站无需 headless 渲染</b> —— 项目文档此前判定「列表页 JS 二次加载, 需 headless 浏览器」,
 * 本次实测推翻: 列表是普通表单 POST 的 JSON 接口, 正文页是服务端直出的 HTML。
 *
 * ⚠️ <b>{@code total} 在 1000 处封顶</b> —— 实测只有 DEAL_TIME=01「当天」窗口对所有类别都安全
 * (近三天起政府采购即触顶, 近十天起 4 个类别触顶), 故 {@code dealTime} 默认 {@code "01"} 且做成
 * 可配置项 —— 一旦某频道触顶, 运维可在不改码的前提下收紧窗口。
 */
@Slf4j
@Component
public class GgzySourceAdapter implements SourceAdapter {

    /** 站点标识 */
    private static final String CODE = "ggzy";
    /** 默认 UA */
    private static final String DEFAULT_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    /** 列表接口 */
    private static final String LIST_PATH = "/information/pubTradingInfo/getTradList";
    /** 默认站点根地址 */
    private static final String DEFAULT_BASE_URL = "https://www.ggzy.gov.cn";
    /** 列表每页条数(源站固定 20) */
    private static final int PAGE_SIZE = 20;
    /** 源站 total 上限: 达到该值说明结果集被截断 */
    private static final int TOTAL_CAP = 1000;
    /** 默认时间窗: 当天(唯一对所有类别都不触顶的窗口, 见类注释) */
    private static final String DEFAULT_DEAL_TIME = "01";

    /** 响应码: 成功 */
    private static final int RESP_OK = 200;
    /** 响应码: 操作过于频繁 */
    private static final int RESP_TOO_FREQUENT = 800;
    /** 响应码: 需验证码 */
    private static final int RESP_CAPTCHA = 829;

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
                ? configNode.get("baseUrl").asText() : DEFAULT_BASE_URL;
        String initialUrl = configNode != null && configNode.has("initialUrl")
                ? configNode.get("initialUrl").asText() : baseUrl + "/";
        String ua = configNode != null && configNode.has("userAgent")
                ? configNode.get("userAgent").asText() : DEFAULT_UA;
        String dealTime = configNode != null && configNode.has("dealTime")
                ? configNode.get("dealTime").asText() : DEFAULT_DEAL_TIME;
        long intervalMs = site.getIntervalMs() == null ? 3000 : site.getIntervalMs();

        // 2) HTTP 会话(首页预热拿 Cookie, 列表/详情共享)
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        String listReferer = baseUrl + "/deal/dealList.html";
        if (fetchGet(client, initialUrl, ua, null) == null) {
            log.warn("[crawler] ggzy 首页预热失败(不影响接口调用): {}", initialUrl);
        }

        // 3) 频道循环(channels[].path = DEAL_CLASSIFY 类别码)
        List<ChannelDef> channels = parseChannels(site.getChannels());
        for (ChannelDef channel : channels) {
            // 3.1) 列表 → 收集详情 URL(连同列表记录本身: 类型/省份/行业以列表字段为准)
            Map<String, JsonNode> candidates = new LinkedHashMap<>();
            for (int page = 1; page <= channel.pageCount; page++) {
                JsonNode root = fetchList(client, baseUrl, listReferer, ua, dealTime, channel, page, stats);
                if (root == null) {
                    continue;
                }
                int code = root.path("code").asInt(0);
                if (code == RESP_CAPTCHA) {
                    // 合规边界: 不模拟登录、不破解验证码。源站要求验证码说明已触发风控,
                    // 继续请求只会加重, 故中止本轮该站点的采集, 留待下次调度。
                    log.error("[crawler] ggzy 源站要求验证码(code={}), 已中止本站点本轮采集"
                            + "(不进行验证码绕过); 若反复出现请下调采集频率或停用该站点", RESP_CAPTCHA);
                    return;
                }
                if (code != RESP_OK) {
                    // 801 错误 / 804 请细化查询条件 等属**确定性**失败(可恢复的 800 已在 fetchList 里重试过),
                    // 换下一页是同一个查询条件, 结果只会一样, 故终止本频道翻页并留日志
                    log.warn("[crawler] ggzy 列表响应异常 code={} message={} (类别 {} 第{}页), 终止本频道翻页",
                            code, root.path("message").asText(""), channel.path, page);
                    break;
                }
                JsonNode data = root.path("data");
                int parsed = collectRecords(baseUrl, data, candidates);
                stats.setListParsed(stats.getListParsed() + parsed);
                log.info("[crawler] ggzy 频道 {} 第{}页: 解析出 {} 条", channel.name, page, parsed);

                if (isCapReached(data, channel)) {
                    break;
                }
                // 源站报告的总页数更小时提前收手, 避免对短频道发无效请求
                int pages = data.path("pages").asInt(0);
                if (pages > 0 && page >= pages) {
                    break;
                }
                if (page < channel.pageCount) {
                    Thread.sleep(intervalMs);
                }
            }

            // 3.2) 详情逐条抓取 + 解析 + 入库
            int fetched = 0;
            int emptyBody = 0;
            int idx = 0;
            for (Map.Entry<String, JsonNode> candidate : candidates.entrySet()) {
                String detailUrl = candidate.getKey();
                if (idx++ > 0) {
                    Thread.sleep(intervalMs);
                }
                // 详情页的 Referer 用其外壳页(a 页), 与浏览器 iframe 的实际加载一致
                String html = fetchGet(client, detailUrl, ua, detailUrl.replace("/html/b/", "/html/a/"));
                if (html == null) {
                    stats.setDetailFailed(stats.getDetailFailed() + 1);
                    continue;
                }
                // 空正文记录不入库: 该平台只做元数据汇聚, 正文留在原交易平台。
                // 实测正文可得率按类别差异极大(工程建设 7/10、政府采购 2/10、国有产权 0/10),
                // 入库这些记录只会得到标题可点、正文空白的详情页, 故跳过并计入失败数。
                if (GgzyHtmlSupport.extractContent(html).isBlank()) {
                    stats.setDetailFailed(stats.getDetailFailed() + 1);
                    emptyBody++;
                    continue;
                }
                fetched++;
                NoticePortalDO notice;
                try {
                    notice = GgzyNoticeParser.parse(candidate.getValue(), html, channel.type, detailUrl,
                            site.getName());
                } catch (Exception ex) {
                    stats.setDetailFailed(stats.getDetailFailed() + 1);
                    log.warn("[crawler] ggzy 解析失败 {}: [{}] {}", detailUrl, ex.getClass().getSimpleName(),
                            ex.getMessage());
                    continue;
                }
                try {
                    noticeUpsertService.upsert(notice, stats);
                } catch (Exception ex) {
                    stats.setDetailFailed(stats.getDetailFailed() + 1);
                    log.warn("[crawler] ggzy 入库失败 {}: [{}] {}", detailUrl, ex.getClass().getSimpleName(),
                            ex.getMessage());
                }
            }
            stats.setDetailFetched(stats.getDetailFetched() + fetched);
            log.info("[crawler] ggzy 频道 {} 完成: 详情{}条/无正文跳过{}条/失败{}条",
                    channel.name, fetched, emptyBody, stats.getDetailFailed());
        }
    }

    // ==================== 列表 ====================

    /**
     * 取一页列表(POST 表单)
     *
     * **只要拿到可解析的 JSON 就原样返回, 业务响应码一律交给调用方分支处理** —— 尤其是
     * {@code 829}(需验证码)必须能被 {@link #crawl} 看见并中止本轮采集; 若在这里就把非 200
     * 的响应码吞成 null, 验证码分支将永远走不到(源站一旦开始要求验证码, 采集只会静默地返回 0 条,
     * 反而更糟)。仅在传输失败或响应体不是 JSON 时返回 null。
     *
     * {@code 800}(操作过于频繁)休眠后重试一次: 实测详情接口对高频请求会拒连, 列表接口虽未观察到
     * 频控, 但源站确有该响应码, 故按可恢复错误处理。
     */
    private JsonNode fetchList(HttpClient client, String baseUrl, String referer, String ua,
                               String dealTime, ChannelDef channel, int page, CrawlStats stats) {
        String body = "DEAL_TIME=" + encode(dealTime) + "&DEAL_CLASSIFY=" + encode(channel.path)
                + "&PAGENUMBER=" + page;
        for (int attempt = 1; attempt <= 2; attempt++) {
            String text = post(client, baseUrl + LIST_PATH, body, ua, referer);
            stats.setListFetched(stats.getListFetched() + 1);
            if (text == null) {
                return null;
            }
            JsonNode root;
            try {
                root = objectMapper.readTree(text);
            } catch (Exception ex) {
                // 返回的是 HTML(如缺 DEAL_TIME 时源站给的 404 页面)说明请求形态有变, 需人工核对
                log.warn("[crawler] ggzy 列表响应不是 JSON(类别 {} 第{}页), 响应体前 200 字符: {}",
                        channel.path, page, GgzyHtmlSupport.abbreviate(text));
                return null;
            }
            if (root.path("code").asInt(0) == RESP_TOO_FREQUENT && attempt == 1) {
                log.warn("[crawler] ggzy 列表响应「操作过于频繁」(类别 {} 第{}页), 休眠后重试", channel.path, page);
                sleepQuietly(3000);
                continue;
            }
            return root;
        }
        return null;
    }

    /** 收集本页的详情 URL(→ 列表记录), 返回解析出的条数 */
    private int collectRecords(String baseUrl, JsonNode data, Map<String, JsonNode> candidates) {
        JsonNode records = data.path("records");
        if (!records.isArray()) {
            return 0;
        }
        int parsed = 0;
        for (JsonNode item : records) {
            String url = item.path("url").asText("");
            if (url.isBlank()) {
                continue;
            }
            String detail = GgzyHtmlSupport.toDetailUrl(url);
            if (detail == null) {
                continue;
            }
            // 分页间可能重复(源站按时间倒序, 新数据会挤动分页), putIfAbsent 顺带去重
            if (candidates.putIfAbsent(detail.startsWith("http") ? detail : baseUrl + detail, item) == null) {
                parsed++;
            }
        }
        return parsed;
    }

    /**
     * 结果集是否已被源站的 1000 条上限截断
     *
     * 截断是**静默的**(接口不报错, 只是少给数据), 与「整页公告被静默丢掉」同类, 故必须告警。
     * 缓解手段是收紧 {@code dealTime}(改用更窄的时间窗), 而不是加大 pageCount —— pageCount 再大
     * 也翻不出被源站截掉的记录。
     */
    private boolean isCapReached(JsonNode data, ChannelDef channel) {
        int total = data.path("total").asInt(0);
        if (total < TOTAL_CAP) {
            return false;
        }
        log.warn("[crawler] ggzy 频道 {} 结果集触及源站 {} 条上限(total={}, 每页{}条), "
                        + "本轮该频道数据可能被截断; 缓解办法是在 crawler_site.config 里把 dealTime 换到更窄的时间窗",
                channel.name, TOTAL_CAP, total, PAGE_SIZE);
        return true;
    }

    // ==================== HTTP ====================

    /** POST 表单, 失败、非 2xx 或响应体为空均返回 null */
    private String post(HttpClient client, String url, String body, String ua, String referer) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", ua)
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    // 无验证码时源站前端固定发空值, 这里照发以保持请求形态一致
                    .header("X-Pass-Token", "");
            if (referer != null && !referer.isEmpty()) {
                builder.header("Referer", referer);
            }
            HttpResponse<byte[]> response = client.send(
                    builder.POST(HttpRequest.BodyPublishers.ofString(body)).timeout(Duration.ofSeconds(20)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[crawler] ggzy HTTP {}: {}", response.statusCode(), url);
                return null;
            }
            String text = new String(response.body(), StandardCharsets.UTF_8);
            // 缺 DEAL_TIME 时源站返回 404 页面(而非 JSON), 走到这里说明路径/参数有变, 需人工核对
            return text.isBlank() ? null : text;
        } catch (Exception ex) {
            log.warn("[crawler] ggzy 请求失败 {}: {}", url, ex.getMessage());
            return null;
        }
    }

    /**
     * GET, 失败、非 2xx 或响应体为空均返回 null
     *
     * 空响应体重试一次: 与另两个适配器同因 —— CDN 会间歇性返回 200 + 0 字节, 只判状态码会把整页
     * 内容静默丢掉。详情接口对高频请求会**直接拒连**(curl 表现为连不上而非 4xx), 故重试间隔取 1s。
     */
    private String fetchGet(HttpClient client, String url, String ua, String referer) {
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
                    log.warn("[crawler] ggzy HTTP {}: {}", response.statusCode(), url);
                    return null;
                }
                String body = new String(response.body(), StandardCharsets.UTF_8);
                if (!body.isBlank()) {
                    return body;
                }
                log.warn("[crawler] ggzy HTTP 200 但响应体为空(第{}次): {}", attempt, url);
            } catch (Exception ex) {
                log.warn("[crawler] ggzy 请求失败(第{}次) {}: {}", attempt, url, ex.getMessage());
            }
            if (attempt == 1) {
                sleepQuietly(1000);
            }
        }
        return null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /** 静默休眠(中断时恢复中断标记, 不吞掉) */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 配置 ====================

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

    /** channels[].path = DEAL_CLASSIFY 类别码 */
    private List<ChannelDef> parseChannels(String json) {
        List<ChannelDef> result = new ArrayList<>();
        try {
            JsonNode array = objectMapper.readTree(json != null ? json : "[]");
            for (JsonNode node : array) {
                if (!node.hasNonNull("path")) {
                    continue;
                }
                ChannelDef def = new ChannelDef();
                def.path = node.get("path").asText();
                def.name = node.has("name") ? node.get("name").asText() : def.path;
                def.type = node.has("type") ? node.get("type").asText() : "tender";
                def.pageCount = node.has("pageCount") ? node.get("pageCount").asInt(1) : 1;
                result.add(def);
            }
        } catch (Exception ex) {
            log.warn("[crawler] ggzy channels 配置解析失败: {}", ex.getMessage());
        }
        if (result.isEmpty()) {
            // 兜底: 工程建设(该站对本项目的最大增量, ccgp 未覆盖)
            ChannelDef def = new ChannelDef();
            def.path = "01";
            def.name = "工程建设";
            def.type = "tender";
            def.pageCount = 2;
            result.add(def);
        }
        return result;
    }

    /** 频道配置 */
    private static class ChannelDef {
        /** DEAL_CLASSIFY 类别码(01 工程建设 / 03 土地使用权 / 05 国有产权 …) */
        private String path;
        private String name;
        /** 兜底类型: 仅当类型码与文本关键词都识别不出时使用 */
        private String type;
        private int pageCount;
    }
}
