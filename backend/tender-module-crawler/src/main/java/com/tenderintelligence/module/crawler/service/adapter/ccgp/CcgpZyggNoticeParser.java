package com.tenderintelligence.module.crawler.service.adapter.ccgp;

import com.tenderintelligence.module.crawler.service.NoticeRegionExtractor;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中国政府采购网 —— 中央公告(zygg)详情页解析器
 *
 * 与地方公告(dfgg, 见 {@link CcgpNoticeParser})模板不同, 2026-09-11 实测:
 * <ul>
 *   <li>标题: {@code <h2 class="tc">}, 与 dfgg 同款</li>
 *   <li>正文: {@code <div class="vF_deail_maincontent">} → "相关公告"推荐位前;<b>没有</b> {@code #noticeArea}</li>
 *   <li>结构化字段: 表单式表格 {@code <td class='title'>标签</td><td>值</td>}(类名用单引号),
 *       <b>没有</b> dfgg 的 {@code noticePurchase-*} / {@code bidFileSubmitTime3} 等 span 锚点</li>
 *   <li>发布时间: 表格 {@code 公告时间} 行(页面另有 {@code <span id="pubTime">})</li>
 * </ul>
 *
 * 11 个中央频道(公开招标/中标/成交/更正/终止/竞争性磋商/竞争性谈判/询价/单一来源/邀请招标/资格预审)
 * 模板一致; 但<b>字段标签随公告类型变化</b>, 故金额与时间按候选标签优先级取值:
 * 金额取 预算金额 → 总成交金额 → 总中标金额; 时间取 开标时间 → 响应文件开启时间。
 *
 * 地区(省/市)与行业仍为规则提取(见 {@link NoticeRegionExtractor}), 非源站官方字段。
 */
public final class CcgpZyggNoticeParser {

    private CcgpZyggNoticeParser() {
    }

    // ==================== 正则锚点(实测) ====================
    private static final Pattern TITLE = Pattern.compile("<h2[^>]*class=\"tc\"[^>]*>([^<]+)</h2>");
    /** 表单表格行: <td class='title'>标签</td><td ...>值</td> */
    private static final Pattern TABLE_ROW = Pattern.compile(
            "<td[^>]*class=['\"]title['\"][^>]*>(.*?)</td>\\s*<td[^>]*>(.*?)</td>", Pattern.DOTALL);
    /** 金额: 源站以"万元"发布, 如 ￥85.500000万元（人民币） */
    private static final Pattern AMOUNT_WAN = Pattern.compile("([\\d,]+(?:\\.\\d+)?)\\s*万");
    /** 兜底: 若某频道改以"元"发布 */
    private static final Pattern AMOUNT_YUAN = Pattern.compile("([\\d,]+(?:\\.\\d+)?)\\s*元");

    private static final String[] BUDGET_LABELS = {"预算金额", "总成交金额", "总中标金额"};
    private static final String[] OPEN_TIME_LABELS = {"开标时间", "响应文件开启时间"};
    private static final String LABEL_PURCHASER = "采购单位";
    private static final String LABEL_AGENCY = "代理机构名称";
    private static final String LABEL_CONTACT = "项目联系人";
    private static final String LABEL_PHONE = "项目联系电话";
    private static final String LABEL_PUBLISH_TIME = "公告时间";

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

        // 1) 正文片段(与 dfgg 共用切取逻辑: vF_deail_maincontent → 推荐位前)
        String content = CcgpHtmlSupport.extractContent(html);
        notice.setContent(content);

        // 1.1) 附件: 附件块位于正文尾部, 故在截出的正文片段上解析(见 CcgpAttachmentParser)
        notice.setAttachments(CcgpAttachmentParser.parse(content));

        // 2) 表单表格字段(首次出现的标签为准, 同一标签在页内可能重复)
        Map<String, String> fields = parseFieldTable(html);

        // 3) 标题
        String title = CcgpHtmlSupport.first(TITLE, html);
        if (title == null) {
            title = fields.getOrDefault("采购项目名称", "");
        }
        notice.setTitle(title.strip());

        // 4) 结构化字段(项目编号在正文与表格里都找一遍)
        String projectNo = CcgpHtmlSupport.first(CcgpHtmlSupport.PROJECT_NO, content);
        notice.setProjectNo(CcgpHtmlSupport.clean(projectNo != null ? projectNo : fields.get("项目编号")));
        notice.setBudget(resolveBudget(fields));
        notice.setTenderPerson(fields.get(LABEL_PURCHASER));
        notice.setAgency(fields.get(LABEL_AGENCY));
        notice.setContact(fields.get(LABEL_CONTACT));
        notice.setContactPhone(fields.get(LABEL_PHONE));

        // 5) 开标时间(中央公告同口径: 开标时间=投标截止)
        String openTimeText = firstLabel(fields, OPEN_TIME_LABELS);
        LocalDateTime openTime = CcgpHtmlSupport.parseFullDateTime(openTimeText);
        notice.setDeadline(openTime);
        notice.setOpenTime(openTime);

        // 6) 发布时间(表格"公告时间"; 缺失时兜底取当前时间, 与 dfgg 同策略)
        LocalDateTime publishTime = CcgpHtmlSupport.parseFullDateTime(fields.get(LABEL_PUBLISH_TIME));
        notice.setPublishTime(publishTime == null ? LocalDateTime.now() : publishTime);

        // 7) 地区/行业(规则提取, 公共词典)
        String detectSource = notice.getTitle() + " " + NoticeRegionExtractor.toPlainText(content);
        String[] region = NoticeRegionExtractor.extractRegion(detectSource);
        notice.setProvince(region[0]);
        notice.setCity(region[1]);
        notice.setIndustry(NoticeRegionExtractor.detectIndustry(detectSource));

        return notice;
    }

    // ==================== 内部实现 ====================

    /** 取表单表格的"标签 → 值"映射; 同一标签重复出现时保留首次 */
    private static Map<String, String> parseFieldTable(String html) {
        Map<String, String> fields = new HashMap<>();
        if (html == null) {
            return fields;
        }
        Matcher matcher = TABLE_ROW.matcher(html);
        while (matcher.find()) {
            String label = NoticeRegionExtractor.toPlainText(matcher.group(1)).strip();
            if (label.isEmpty() || fields.containsKey(label)) {
                continue;
            }
            fields.put(label, NoticeRegionExtractor.toPlainText(matcher.group(2)).strip());
        }
        return fields;
    }

    /** 按候选标签优先级取第一个非空值 */
    private static String firstLabel(Map<String, String> fields, String[] labels) {
        for (String label : labels) {
            String value = fields.get(label);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /** 金额: 优先按"万元"解析, 若该频道以"元"发布则换算 */
    private static BigDecimal resolveBudget(Map<String, String> fields) {
        String text = firstLabel(fields, BUDGET_LABELS);
        if (text == null) {
            return null;
        }
        Matcher wan = AMOUNT_WAN.matcher(text);
        if (wan.find()) {
            return CcgpHtmlSupport.parseBudgetFromWan(wan.group(1));
        }
        Matcher yuan = AMOUNT_YUAN.matcher(text);
        return yuan.find() ? CcgpHtmlSupport.parseBudgetFromYuan(yuan.group(1)) : null;
    }

}
