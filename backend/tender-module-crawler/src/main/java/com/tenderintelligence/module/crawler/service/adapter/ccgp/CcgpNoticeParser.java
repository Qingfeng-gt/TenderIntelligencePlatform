package com.tenderintelligence.module.crawler.service.adapter.ccgp;

import com.tenderintelligence.module.crawler.service.NoticeRegionExtractor;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 中国政府采购网 —— 地方公告(dfgg)详情页解析器
 *
 * dfgg 是"全国省级公告聚合流"(单页混排各省, 非按省分列), 因此本解析器一套即覆盖全国省级采购公告。
 * 中央公告(zygg)是另一套表格式模板, 见 {@link CcgpZyggNoticeParser}。
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
    private static final Pattern BUDGET = Pattern.compile("预算金额[：:]\\s*([\\d,]+(?:\\.\\d+)?)\\s*元");
    private static final Pattern PURCHASER = Pattern.compile("noticePurchase-purchaserOrgName[^>]*>\\s*([^<]+)<");
    private static final Pattern PURCHASER_TEL = Pattern.compile("noticePurchase-purchaserLinkTel[^>]*>\\s*([^<]+)<");
    private static final Pattern AGENCY = Pattern.compile("noticeAgency-agencyName[^>]*>\\s*([^<]+)<");
    private static final Pattern CONTACT = Pattern.compile("projectContact-managerName[^>]*>\\s*([^<]+)<");
    private static final Pattern DEADLINE = Pattern.compile("bidFileSubmitTime3[^>]*>\\s*([^<]+)<");
    private static final Pattern RELEASE_DATE = Pattern.compile("name=\"releaseDateSpan\"[^>]*>\\s*([^<]+)<");

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
        String content = CcgpHtmlSupport.extractContent(html);
        notice.setContent(content);

        // 1.1) 附件: 附件块位于正文尾部, 故在截出的正文片段上解析(见 CcgpAttachmentParser)
        notice.setAttachments(CcgpAttachmentParser.parse(content));

        // 2) 标题
        String title = CcgpHtmlSupport.first(TITLE, html);
        if (title == null) {
            String projectName = CcgpHtmlSupport.first(Pattern.compile("项目名称[：:]\\s*([^<\\n]+)"), content);
            title = projectName != null ? projectName : "";
        }
        notice.setTitle(title.strip());

        // 3) 结构化字段(在正文全文上做正则)
        notice.setProjectNo(CcgpHtmlSupport.clean(CcgpHtmlSupport.first(CcgpHtmlSupport.PROJECT_NO, content)));
        notice.setBudget(CcgpHtmlSupport.parseBudgetFromYuan(CcgpHtmlSupport.first(BUDGET, content)));
        notice.setTenderPerson(CcgpHtmlSupport.clean(CcgpHtmlSupport.first(PURCHASER, content)));
        notice.setAgency(CcgpHtmlSupport.clean(CcgpHtmlSupport.first(AGENCY, content)));
        notice.setContact(CcgpHtmlSupport.clean(CcgpHtmlSupport.first(CONTACT, content)));
        notice.setContactPhone(CcgpHtmlSupport.clean(CcgpHtmlSupport.first(PURCHASER_TEL, content)));

        // 4) 投标截止/开标时间(CCGP 公开招标: 开标时间=投标截止)
        LocalDateTime deadline = CcgpHtmlSupport.parseFullDateTime(CcgpHtmlSupport.first(DEADLINE, content));
        notice.setDeadline(deadline);
        notice.setOpenTime(deadline);

        // 5) 发布日期(详情页 releaseDateSpan)
        LocalDate publishDate = CcgpHtmlSupport.parseDate(CcgpHtmlSupport.first(RELEASE_DATE, html));
        notice.setPublishTime(publishDate == null ? LocalDateTime.now() : publishDate.atStartOfDay());

        // 6) 地区/行业(规则提取, 公共词典)
        String detectSource = notice.getTitle() + " " + NoticeRegionExtractor.toPlainText(content);
        String[] region = NoticeRegionExtractor.extractRegion(detectSource);
        notice.setProvince(region[0]);
        notice.setCity(region[1]);
        notice.setIndustry(NoticeRegionExtractor.detectIndustry(detectSource));

        return notice;
    }

}
