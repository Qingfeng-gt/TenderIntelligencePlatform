package com.tenderintelligence.module.crawler.service.adapter.ccgp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中国政府采购网各模板解析器共用的取值助手
 *
 * cggg 下两个分组的详情页模板不同, 但"截正文 + 取值"的基础逻辑一致:
 * <ul>
 *   <li>dfgg(地方公告, 全国省级公告聚合流): {@code #noticeArea} 模板, 见 {@link CcgpNoticeParser}</li>
 *   <li>zygg(中央公告): {@code vF_deail_maincontent} 表单表格模板, 见 {@link CcgpZyggNoticeParser}</li>
 * </ul>
 */
final class CcgpHtmlSupport {

    private CcgpHtmlSupport() {
    }

    /** 日期: 2026年09月11日 */
    private static final Pattern DATE = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日");
    /** 时间: 兼容 dfgg 的 "09时30分" 与 zygg 的 "14:00" 两种写法 */
    private static final Pattern TIME_HM = Pattern.compile("(\\d{1,2})[时:](\\d{1,2})");

    /**
     * 项目编号(两套模板共用)
     *
     * 源站写作「项目编号：XXXXX」并常与后续正文同处一段、中间无标签隔断，例如
     * {@code 项目编号：YNZC2026-G3-...-0517）”，本项目递交投标文件的投标人不足3家，根据《…》}。
     * 若用宽松的 {@code [^<\s]+}, 会一路吃到整句正文(实测取到 135 字符), 既污染字段值,
     * 又会超过 {@code notice.project_no varchar(128)} 触发 Data too long, 导致**整条公告入库失败**。
     * 故限定为「非空白、非小于号、非中文标点」且长度 ≤ 64 —— 中文标点是句读边界, 编号本身不含。
     */
    static final Pattern PROJECT_NO =
            Pattern.compile("项目编号[：:]\\s*([^\\s<，。；：、！？（）【】《》“”‘’·—…]{1,64})");

    /**
     * 正文容器收尾锚点: 源站给正文容器的闭合 div 缀了一段注释, 全文唯一, 是可靠的收尾位置
     */
    private static final String CONTENT_END_COMMENT = "<!--vF_deail_maincontent-->";
    /** 收尾锚点(容忍闭合标签与注释之间的空白) */
    private static final Pattern CONTENT_END =
            Pattern.compile("</div>\\s*<!--\\s*vF_deail_maincontent\\s*-->");
    /** 正文区内的 {@code <style>} 块(属页面骨架, 不入库; 成对删除不影响 div 嵌套) */
    private static final Pattern STYLE_BLOCK = Pattern.compile("<style[^>]*>.*?</style>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /**
     * 截取正文片段(原始页面 HTML 不入库, 只留公告正文区段)
     *
     * 起点按模板二选一: 新模板 {@code #noticeArea}, 旧模板 / 中央公告 {@code vF_deail_maincontent};
     * 两处都回退到"完整 div 标签的起点", 否则会在片段首尾残留半个标签。
     *
     * 收尾**必须**用 {@link #CONTENT_END_COMMENT} 锚点, 不能再用 {@code tail.rfind("</div></div>")}:
     * 源站正文尾部是 {@code </ul>\n            </div>\n        </div><!--vF_detail_content_container-->},
     * 两个闭合 div 之间**隔着换行**, 而 rfind 找的是相邻的 {@code </div></div>}, 于是命中的是
     * 正文内部更早的一处, 把其后内容整段丢掉。2026-09-13 实测 60 页: 5 页因此丢掉附件块,
     * 2 页各丢掉约 1.4 万字符(正文 80%, 含采购需求/资格要求整表)。
     */
    static String extractContent(String html) {
        if (html == null) {
            return "";
        }
        int commentAt = html.indexOf(CONTENT_END_COMMENT);
        if (commentAt < 0) {
            // 兜底: 源站若改版去掉该注释, 退回旧的 </div></div> 启发式(至少能截出正文主体)
            return extractContentByCloseTags(html);
        }
        int start = contentStart(html);
        if (start < 0) {
            return "";
        }
        String tail = html.substring(start, commentAt + CONTENT_END_COMMENT.length());
        Matcher end = CONTENT_END.matcher(tail);
        if (end.find()) {
            tail = tail.substring(0, end.end());
        }
        return STYLE_BLOCK.matcher(tail).replaceAll("").strip();
    }

    /**
     * 正文区段的起点下标; 找不到正文容器时返回 -1
     */
    private static int contentStart(String html) {
        int noticeArea = html.indexOf("id=\"noticeArea\"");
        if (noticeArea >= 0) {
            // 新模板: <div class="vF_detail_content_container"><div class="vF_detail_content"><div>
            //             <div class="protect" id="noticeArea">…
            // 从最外层正文容器起截, 尾部闭合标签才对得平(只从 noticeArea 起截会多出两个 </div>)
            int container = html.lastIndexOf("vF_detail_content_container", noticeArea);
            int containerDiv = container >= 0 ? html.lastIndexOf("<div", container) : -1;
            return containerDiv >= 0 ? containerDiv : html.lastIndexOf("<div", noticeArea);
        }
        // 旧模板 / 中央公告: <div class="vF_deail_maincontent">…
        int main = html.indexOf("vF_deail_maincontent");
        if (main < 0) {
            return -1;
        }
        int divStart = html.lastIndexOf("<div", main);
        // 回退到完整 <div 标签起点, 避免残留类名文本; 距离过大说明命中的是别处的同名文本
        return divStart >= 0 && main - divStart <= 60 ? divStart : main;
    }

    /**
     * 旧的收尾启发式, 仅在源站去掉 {@link #CONTENT_END_COMMENT} 注释时兜底
     */
    private static String extractContentByCloseTags(String html) {
        int start = html.indexOf("id=\"noticeArea\"");
        String tail;
        if (start >= 0) {
            tail = html.substring(start);
            int styleEnd = tail.indexOf("</style>");
            if (styleEnd >= 0) {
                tail = tail.substring(styleEnd + "</style>".length());
            }
        } else {
            start = contentStart(html);
            if (start < 0) {
                return "";
            }
            tail = html.substring(start);
            int relEnd = tail.indexOf("vF_detail_relcontent");
            if (relEnd >= 0) {
                int relDivStart = tail.lastIndexOf("<div", relEnd);
                tail = tail.substring(0, relDivStart >= 0 ? relDivStart : relEnd);
            }
        }
        int end = tail.lastIndexOf("</div></div>");
        if (end < 0) {
            return tail.strip();
        }
        return tail.substring(0, end + "</div></div>".length()).strip();
    }

    static String first(Pattern pattern, String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 去首尾空白
     *
     * 用 strip() 而非 trim(): 源站字段常带全角空格(U+3000, 如项目编号后的一串空白),
     * trim() 只去 U+0020 及以下, 会把它留在字段值里。
     */
    static String clean(String value) {
        return value == null ? null : value.strip();
    }

    /** "296,000.00元" → 29.60 万元(dfgg 模板预算以"元"发布) */
    static BigDecimal parseBudgetFromYuan(String amount) {
        BigDecimal yuan = parseNumber(amount);
        return yuan == null ? null : yuan.divide(BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP);
    }

    /** "85.500000" → 85.50 万元(zygg 模板预算已以"万元"发布, 只需归一标度) */
    static BigDecimal parseBudgetFromWan(String amount) {
        BigDecimal wan = parseNumber(amount);
        return wan == null ? null : wan.setScale(2, RoundingMode.HALF_UP);
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
     * 兼容 "2026年10月10日 09时30分00秒"(dfgg)与 "2026年10月08日  14:00"(zygg)。
     */
    static LocalDateTime parseFullDateTime(String value) {
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

    static LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = DATE.matcher(value);
        if (matcher.find()) {
            return LocalDate.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        }
        return null;
    }

}
