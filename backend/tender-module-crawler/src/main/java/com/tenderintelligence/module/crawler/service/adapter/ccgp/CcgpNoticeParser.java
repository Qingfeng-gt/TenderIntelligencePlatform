package com.tenderintelligence.module.crawler.service.adapter.ccgp;

import com.tenderintelligence.module.crawler.service.NoticeRegionExtractor;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中国政府采购网(ccgp)公告详情页解析器
 *
 * 字段锚点均为 2026-09-08 实测:
 * - 标题: <h2 class="tc">…
 * - 正文/字段: #noticeArea 内(项目编号/预算金额/采购人/代理机构/联系人/截止时间/开标地点)
 * - 发布日期: <p name="releaseDateSpan">2026年09月08日</p>
 * 地区(省/市)与行业为规则提取(标题+正文关键词词典),非官方标准字段,兜底"其他"
 */
public final class CcgpNoticeParser {

    private CcgpNoticeParser() {
    }

    // ==================== 正则锚点(实测) ====================
    private static final Pattern TITLE = Pattern.compile("<h2[^>]*class=\"tc\"[^>]*>([^<]+)</h2>");
    private static final Pattern PROJECT_NO = Pattern.compile("项目编号[：:]\\s*([^<\\s]+)");
    private static final Pattern BUDGET = Pattern.compile("预算金额[：:]\\s*([\\d,]+(?:\\.\\d+)?)\\s*元");
    private static final Pattern PURCHASER = Pattern.compile("noticePurchase-purchaserOrgName[^>]*>\\s*([^<]+)<");
    private static final Pattern PURCHASER_TEL = Pattern.compile("noticePurchase-purchaserLinkTel[^>]*>\\s*([^<]+)<");
    private static final Pattern AGENCY = Pattern.compile("noticeAgency-agencyName[^>]*>\\s*([^<]+)<");
    private static final Pattern CONTACT = Pattern.compile("projectContact-managerName[^>]*>\\s*([^<]+)<");
    private static final Pattern DEADLINE = Pattern.compile("bidFileSubmitTime3[^>]*>\\s*([^<]+)<");
    private static final Pattern RELEASE_DATE = Pattern.compile("name=\"releaseDateSpan\"[^>]*>\\s*([^<]+)<");

    private static final Pattern FULL_DATE_TIME =
            Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日\\s*(?:(\\d{1,2})时(\\d{1,2})分)?");

    // 地区/行业提取: 公共词典见 {@link NoticeRegionExtractor}
    // ==================== 对外入口 ====================

    /**
     * 解析详情页 HTML → 公告 DO
     *
     * @param html      详情页原始 HTML
     * @param type      平台公告类型(tender/win/change/explore)
     * @param sourceUrl 源站详情 URL(唯一去重键)
     * @param source    来源网站名称
     */
    public static NoticePortalDO parse(String html, String type, String sourceUrl, String source) {
        NoticePortalDO notice = new NoticePortalDO();
        notice.setType(type);
        notice.setSourceUrl(sourceUrl);
        notice.setSource(source);

        // 1) 正文: #noticeArea 内的公告内容(保留 HTML 供前端渲染)
        String content = extractContent(html);
        notice.setContent(content);

        // 2) 标题
        String title = first(TITLE, html);
        if (title == null) {
            String projectName = first(Pattern.compile("项目名称[：:]\\s*([^<\\n]+)"), content);
            title = projectName != null ? projectName : "";
        }
        notice.setTitle(title.trim());

        // 3) 结构化字段(在正文全文上做正则)
        notice.setProjectNo(clean(first(PROJECT_NO, content)));
        notice.setBudget(parseBudget(first(BUDGET, content)));
        notice.setTenderPerson(clean(first(PURCHASER, content)));
        notice.setAgency(clean(first(AGENCY, content)));
        notice.setContact(clean(first(CONTACT, content)));
        notice.setContactPhone(clean(first(PURCHASER_TEL, content)));

        // 4) 投标截止/开标时间(CCGP 公开招标: 开标时间=投标截止)
        LocalDateTime deadline = parseFullDateTime(first(DEADLINE, content));
        notice.setDeadline(deadline);
        notice.setOpenTime(deadline);

        // 5) 发布日期(详情页 releaseDateSpan)
        LocalDate publishDate = parseDate(first(RELEASE_DATE, html));
        notice.setPublishTime(publishDate == null ? LocalDateTime.now() : publishDate.atStartOfDay());

        // 6) 地区/行业(规则提取, 公共词典)
        String detectSource = notice.getTitle() + " " + NoticeRegionExtractor.toPlainText(content);
        String[] region = NoticeRegionExtractor.extractRegion(detectSource);
        notice.setProvince(region[0]);
        notice.setCity(region[1]);
        notice.setIndustry(NoticeRegionExtractor.detectIndustry(detectSource));

        return notice;
    }

    // ==================== 内部实现 ====================

    /** 截取正文: 优先 #noticeArea(新模板); 兜底 vF_deail_maincontent(更正公告旧模板) */
    private static String extractContent(String html) {
        if (html == null) {
            return "";
        }
        int start = html.indexOf("id=\"noticeArea\"");
        String tail;
        if (start >= 0) {
            tail = html.substring(start);
            int styleEnd = tail.indexOf("</style>");
            if (styleEnd >= 0) {
                tail = tail.substring(styleEnd + "</style>".length());
            }
        } else {
            // 旧模板(更正/询价等): <div class="vF_deail_maincontent"> → 相关推荐栏前
            start = html.indexOf("vF_deail_maincontent");
            if (start < 0) {
                return "";
            }
            // 回退到完整 <div 标签起点, 避免残留类名文本
            int divStart = html.lastIndexOf("<div", start);
            if (divStart >= 0 && start - divStart <= 60) {
                start = divStart;
            }
            tail = html.substring(start);
            int relEnd = tail.indexOf("vF_detail_relcontent");
            if (relEnd >= 0) {
                tail = tail.substring(0, relEnd);
            }
        }
        int end = tail.lastIndexOf("</div></div>");
        if (end < 0) {
            return tail.trim();
        }
        return tail.substring(0, end + "</div></div>".length()).trim();
    }

    private static String first(Pattern pattern, String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String clean(String value) {
        return value == null ? null : value.trim();
    }

    /** "296,000.00元" → 29.600000 万元 */
    private static BigDecimal parseBudget(String amount) {
        if (amount == null) {
            return null;
        }
        try {
            return new BigDecimal(amount.replace(",", "")).divide(BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static LocalDateTime parseFullDateTime(String value) {
        LocalDate date = parseDate(value);
        if (date == null) {
            return null;
        }
        Matcher matcher = FULL_DATE_TIME.matcher(value);
        if (matcher.find() && matcher.group(4) != null) {
            return date.atTime(Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)));
        }
        return date.atStartOfDay();
    }

    private static LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = FULL_DATE_TIME.matcher(value);
        if (matcher.find()) {
            return LocalDate.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        }
        return null;
    }

}
