package com.tenderintelligence.module.crawler.service.adapter.ggzy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 全国公共资源交易平台详情页解析器共用的取值助手
 *
 * 详情页两段式(2026-09-14 实测):
 * <ul>
 *   <li>{@code /information/deal/html/<b>a</b>/{地区码}/{类型码}/{日期}/{hash}.html} —— **外壳页**,
 *       正文用 iframe 加载同路径的 b 页(外壳内 {@code var firstLastUrl} 即该 b 页地址)</li>
 *   <li>{@code /information/deal/html/<b>b</b>/…} —— **服务端直出正文页**, 本类解析的就是它</li>
 * </ul>
 *
 * 该站无需 headless 渲染 —— 外壳页虽然是 iframe, 但 b 页是完整直出的 HTML, 普通 HTTP GET 即可。
 * (项目文档此前判定该站「需 headless 渲染」, 本次实测已推翻, 见 doc/技术/06-数据采集设计.md §4.4)
 */
final class GgzyHtmlSupport {

    private GgzyHtmlSupport() {
    }

    /** 正文容器: <div class="detail_content"> */
    private static final Pattern CONTENT_OPEN =
            Pattern.compile("<div[^>]*class\\s*=\\s*\"[^\"]*\\bdetail_content\\b[^\"]*\"[^>]*>",
                    Pattern.CASE_INSENSITIVE);
    /** div 开合标签(用于按嵌套深度找正文容器的收尾位置) */
    private static final Pattern DIV_TOKEN = Pattern.compile("<div\\b|</div\\s*>", Pattern.CASE_INSENSITIVE);
    /** 正文内的 <style> 块(属页面骨架, 不入库) */
    private static final Pattern STYLE_BLOCK =
            Pattern.compile("<style[^>]*>.*?</style>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /** 标题 */
    private static final Pattern TITLE = Pattern.compile("<h4[^>]*class\\s*=\\s*\"h4_o\"[^>]*>(.*?)</h4>", Pattern.DOTALL);
    /** 发布时间: <span>发布时间：2026-09-11 18:39</span> */
    private static final Pattern PUBLISH_TIME = Pattern.compile("发布时间\\s*[：:]\\s*([^<\\n]{4,32})");
    /** 信息来源(原交易平台名): <label id="platformName">中国国际招标网</label> */
    private static final Pattern PLATFORM_NAME =
            Pattern.compile("<label[^>]*id\\s*=\\s*\"platformName\"[^>]*>(.*?)</label>", Pattern.DOTALL);

    /** 日期(两种写法): 2026-09-11 / 2026年09月11日 */
    private static final Pattern DATE = Pattern.compile("(\\d{4})[-年/](\\d{1,2})[-月/](\\d{1,2})日?");
    /** 时分: 兼容 "18:39" 与 "18时39分" */
    private static final Pattern TIME_HM = Pattern.compile("(\\d{1,2})[时:](\\d{1,2})");

    /**
     * 截取正文 HTML(detail_content 的内层)
     *
     * 收尾**必须按 div 嵌套深度配对**, 不能取第一个 {@code </div>}: 正文来自各省交易平台,
     * 模板不一, 常见嵌套 div/table。取第一个闭合标签会把正文截断到几十字符 —— 与
     * {@code CcgpHtmlSupport} 曾踩过的「正文截断丢段」是同一类缺陷。
     */
    static String extractContent(String html) {
        if (html == null) {
            return "";
        }
        Matcher open = CONTENT_OPEN.matcher(html);
        if (!open.find()) {
            return "";
        }
        int bodyStart = open.end();
        int bodyEnd = matchingDivEnd(html, bodyStart);
        if (bodyEnd < 0) {
            return "";
        }
        return STYLE_BLOCK.matcher(html.substring(bodyStart, bodyEnd)).replaceAll("").strip();
    }

    /**
     * 从 {@code from} 起, 返回与已开启的 div 配对的收尾 {@code </div>} 下标; 不配对时返回 -1
     *
     * {@code div} 名为 {@code detail_content} 的容器在 {@code from} 之前已经开启, 故深度从 0 起算,
     * 遇到深度要变成 -1 的那个 {@code </div>} 就是它的闭合标签。
     */
    private static int matchingDivEnd(String html, int from) {
        Matcher matcher = DIV_TOKEN.matcher(html);
        matcher.region(from, html.length());
        int depth = 0;
        while (matcher.find()) {
            if (matcher.group().startsWith("</")) {
                if (depth == 0) {
                    return matcher.start();
                }
                depth--;
            } else {
                depth++;
            }
        }
        return -1;
    }

    /** 标题(h4_o); 取不到返回 null(而非空串 —— 调用方靠 null 判断是否回落到列表字段) */
    static String extractTitle(String html) {
        String title = firstGroup(TITLE, html);
        return title == null ? null : clean(stripTags(title));
    }

    /**
     * 发布时间(精确到分)
     *
     * 比列表接口的 {@code publishTime}(只有日期)更精确, 故解析时优先取这里。
     */
    static LocalDateTime extractPublishTime(String html) {
        return parseDateTime(firstGroup(PUBLISH_TIME, html));
    }

    /** 信息来源(原交易平台名, 如「中国国际招标网」); 取不到返回空串 */
    static String extractPlatformName(String html) {
        String value = clean(stripTags(firstGroup(PLATFORM_NAME, html)));
        return value == null ? "" : value;
    }

    /**
     * 列表页 URL → 正文页 URL
     *
     * 列表接口给的是 {@code /information/deal/html/a/…} 外壳页地址, 正文在同路径的 {@code b/} 页。
     * 实测核对: 外壳页内 {@code var firstLastUrl = '/information/deal/html/b/…'} 与改写结果一致。
     */
    static String toDetailUrl(String listUrl) {
        if (listUrl == null) {
            return null;
        }
        return listUrl.replace("/information/deal/html/a/", "/information/deal/html/b/");
    }

    // ==================== 通用取值工具 ====================

    /** 取第一个捕获组 */
    static String firstGroup(Pattern pattern, String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** 取第 group 个捕获组 */
    static String group(Pattern pattern, String text, int group) {
        if (text == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(group) : null;
    }

    /**
     * 去首尾空白
     *
     * 用 strip() 而非 trim(): 源站字段常带全角空格(U+3000), trim() 只去 U+0020 及以下。
     */
    static String clean(String value) {
        return value == null ? null : value.strip();
    }

    /** 空串按缺省处理 */
    static String orNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 删除全部 HTML 标签(供字段锚点正则使用)
     *
     * 与 {@code NoticeRegionExtractor.toPlainText} 的差别是**本方法删标签、不补空格**:
     * 锚点词(如「投标截止时间」)常被 {@code <span>} 打断, 补空格会拆成「投标截止 时间」而匹配不上。
     * 行业/地区识别仍用 {@code toPlainText}(那里需要保留词边界)。
     */
    static String stripTags(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&ldquo;", "“").replace("&rdquo;", "”")
                .replace("&lt;", "<").replace("&gt;", ">");
    }

    /** "296,000.00元" → 29.60 万元 */
    static BigDecimal parseBudgetFromYuan(String amount) {
        return scale(parseNumber(amount), 10000);
    }

    /** "85.500000" → 85.50 万元 */
    static BigDecimal parseBudgetFromWan(String amount) {
        return scale(parseNumber(amount), 1);
    }

    private static BigDecimal scale(BigDecimal value, int divisor) {
        if (value == null) {
            return null;
        }
        if (divisor == 1) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }
        return value.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal parseNumber(String amount) {
        if (amount == null) {
            return null;
        }
        try {
            return new BigDecimal(amount.replace(",", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 解析带时间的日期, 无时间部分时取当日 0 点
     *
     * 兼容该站的 {@code yyyy-MM-dd HH:mm} 与各省模板的 {@code yyyy年MM月dd日 09时30分}。
     */
    static LocalDateTime parseDateTime(String value) {
        LocalDate date = parseDate(value);
        if (date == null) {
            return null;
        }
        // 只在日期之后找时间, 避免把日期之前(或无关段落)的数字误当时间
        Matcher afterDate = DATE.matcher(value);
        if (afterDate.find()) {
            Matcher time = TIME_HM.matcher(value.substring(afterDate.end()));
            if (time.find()) {
                int hour = Integer.parseInt(time.group(1));
                int minute = Integer.parseInt(time.group(2));
                if (hour < 24 && minute < 60) {
                    return date.atTime(hour, minute);
                }
            }
        }
        return date.atStartOfDay();
    }

    /** 解析日期部分(两种写法都吃); 取不到返回 null */
    static LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = DATE.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        try {
            return LocalDate.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        } catch (Exception ex) {
            // 源站脏数据(如 2026-13-45)不应让整条公告解析失败
            return null;
        }
    }

    /** 截断超长文本(仅用于日志展示) */
    static String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 200 ? value : value.substring(0, 200) + "…";
    }

}
