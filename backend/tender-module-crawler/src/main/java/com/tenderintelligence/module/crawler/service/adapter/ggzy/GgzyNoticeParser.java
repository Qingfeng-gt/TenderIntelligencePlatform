package com.tenderintelligence.module.crawler.service.adapter.ggzy;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenderintelligence.module.crawler.service.AttachmentSupport;
import com.tenderintelligence.module.crawler.service.NoticeRegionExtractor;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeAttachmentDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 全国公共资源交易平台(www.ggzy.gov.cn)公告解析器
 *
 * 两段数据源(2026-09-14 实测):
 * <ol>
 *   <li><b>列表接口</b> getTradList —— 结构化字段丰富, 是 province / regionCode / industry /
 *       informationType 的**首选来源**(比正则可靠, 故优先于正文锚点)</li>
 *   <li><b>正文页</b> /information/deal/html/b/… —— 提供正文与更精确的发布时间</li>
 * </ol>
 *
 * ⚠️ **正文缺失是常态**: 该平台只做元数据汇聚, 正文留在原交易平台。2026-09-14 实测每类抽 10 条:
 * 工程建设 7/10 有正文、政府采购 2/10、国有产权 0/10。无法在列表阶段预筛(类型码与有无正文无稳定
 * 对应关系), 必须抓详情才知道 —— 故「空正文跳过」的判断放在适配器里, 见 {@code GgzySourceAdapter}。
 *
 * 结构化字段(预算/截止时间/招标人等)该站无独立字段, 且在**去标签后的正文纯文本**上用通用中文锚点
 * 提取 —— 正文来自全国各省级交易平台, 模板极不统一, 这些字段属**尽力而为**, 取不到即为 null。
 * 类型映射用显式码表(见 {@link #TYPE_BY_CODE}), 理由见该常量注释。
 */
public final class GgzyNoticeParser {

    private GgzyNoticeParser() {
    }

    // ==================== 类型映射 ====================

    /**
     * 源站 informationType 码 → 本项目 notice.type
     *
     * 用**显式码表**而非「后两位编码规律」: 实测规律不成立 —— {@code 0102}(开标记录)、
     * {@code 2302}(交易公告)都破例(同为 "02" 结尾, 前者不是中标, 后者不是中标而是招标)。
     * 下表的 16 个码是 2026-09-14 遍历 9 个类别实测枚举出来的全集。
     */
    private static final Map<String, String> TYPE_BY_CODE = Map.ofEntries(
            Map.entry("0101", "tender"),  // 招标/资审公告
            Map.entry("0102", "tender"),  // 开标记录(过程记录, 通常无正文, 会被适配器跳过)
            Map.entry("0104", "win"),     // 中标结果公示 / 交易结果公示
            Map.entry("0105", "change"),  // 招标/资审文件澄清
            Map.entry("0201", "tender"),  // 公告信息(政府采购)
            Map.entry("0301", "tender"),  // 出让公示(土地使用权)
            Map.entry("0302", "win"),     // 成交宗地
            Map.entry("0401", "tender"),  // 出让公告(矿业权)
            Map.entry("0402", "win"),     // 出让结果
            Map.entry("0501", "tender"),  // 挂牌披露(国有产权)
            Map.entry("2102", "win"),     // 结果公示(碳排放权)
            Map.entry("2302", "tender"),  // 交易公告(药品采购)
            Map.entry("2501", "tender"),  // 信息披露(林权)
            Map.entry("2502", "win"),     // 成交公告
            Map.entry("9001", "tender"),  // 交易公告(其他)
            Map.entry("9002", "win")      // 成交公示
    );

    // ==================== 字段锚点(实测) ====================

    /** 项目编号: "招标项目编号:5501-264CDBHW0172" / "采购项目编号：HBSW20260728" */
    private static final Pattern PROJECT_NO = Pattern.compile(
            "(?:招标项目编号|采购项目编号|项目编号|招标编号|交易编号|标段编号)\\s*[：:]\\s*([^\\s<，。；：、！？（）【】《》“”‘’·—…]{1,64})");
    private static final Pattern BUDGET = Pattern.compile(
            "(?:预算金额|项目预算|采购预算|最高限价|招标控制价)\\s*[：:]?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(元|万元|万)");
    private static final Pattern TENDER_PERSON = Pattern.compile(
            "(?:招标人|采购人|建设单位|招标单位|采购单位)\\s*[：:]\\s*([^\\s。；，]{2,64})");
    private static final Pattern AGENCY = Pattern.compile(
            "(?:招标代理机构|采购代理机构|代理机构|招标代理)\\s*[：:]\\s*([^\\s。；，]{2,64})");
    /** 联系人: 排除「联系方式」等近义词误命中 */
    private static final Pattern CONTACT = Pattern.compile(
            "(?:项目联系人|联系人)(?![方式电话])\\s*[：:]\\s*([^\\s。；，]{2,32})");
    private static final Pattern PHONE = Pattern.compile(
            "(?:联系电话|联系方式|电话)\\s*[：:]?\\s*([\\d\\-()（）]{6,24})");
    /**
     * 投标截止时间 / 开标时间
     *
     * 源站模板常把两者合并写作「投标截止时间（开标时间）:2026-10-10 10:00」,
     * 故锚点后允许一段括号注释 —— 与 ccgp「deadline 与 openTime 同值」同源同理。
     */
    private static final Pattern DEADLINE = Pattern.compile(
            "(?:投标截止时间|投标文件递交截止时间|递交截止时间|响应文件递交截止时间)"
                    + "[（(]?[^）)\\n]{0,20}[）)]?\\s*[：:]?\\s*([^<\\n]{4,40})");
    private static final Pattern OPEN_TIME = Pattern.compile(
            "(?:开标时间|开标日期)[（(]?[^）)\\n]{0,20}[）)]?\\s*[：:]?\\s*([^<\\n]{4,40})");

    /** 正文内附件锚点 */
    private static final Pattern ANCHOR =
            Pattern.compile("<a[^>]*href\\s*=\\s*\"([^\"]+)\"[^>]*>(.*?)</a>",
                    Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    // ==================== 对外入口 ====================

    /**
     * 解析列表项 + 正文页 HTML → 公告 DO
     *
     * 调用方须先确认正文非空(空正文的公告不入库, 判断见 {@code GgzySourceAdapter})。
     *
     * @param record    列表接口 getTradList 返回的单条记录
     * @param html      正文页(/html/b/…)原始 HTML
     * @param fallback  频道兜底类型(仅当类型码与关键词都识别不出时使用)
     * @param sourceUrl 正文页 URL(唯一去重键)
     * @param source    来源网站名称
     */
    public static NoticePortalDO parse(JsonNode record, String html, String fallback, String sourceUrl, String source) {
        NoticePortalDO notice = new NoticePortalDO();
        notice.setSourceUrl(sourceUrl);
        notice.setSource(source);

        // 1) 正文(判空由调用方负责, 这里只做截取)
        String content = GgzyHtmlSupport.extractContent(html);
        notice.setContent(content);

        // 2) 标题: 列表字段优先, 兜底正文页 h4_o
        String title = text(record, "title");
        if (title == null) {
            title = GgzyHtmlSupport.extractTitle(html);
        }
        notice.setTitle(title == null ? "" : title);

        // 3) 类型: 码表 → 正文/文本关键词 → 频道兜底
        notice.setType(detectType(text(record, "informationType"),
                text(record, "informationTypeText"), fallback));

        // 4) 发布时间: 正文页精确到分, 兜底列表的日期(只有日粒度)
        //    notice.publish_time 为 NOT NULL 且无默认值, 故必须保证有值
        LocalDateTime publishTime = GgzyHtmlSupport.extractPublishTime(html);
        if (publishTime == null) {
            publishTime = GgzyHtmlSupport.parseDateTime(text(record, "publishTime"));
        }
        notice.setPublishTime(publishTime == null ? LocalDateTime.now() : publishTime);

        // 5) 地区: 源站结构化字段优先(权威), 兜底公共词典的规则提取
        notice.setRegionCode(text(record, "province"));
        String province = normalizeProvince(text(record, "provinceText"));
        String city = normalizeCity(text(record, "cityText"));
        String plain = GgzyHtmlSupport.stripTags(content);
        String detectSource = notice.getTitle() + " " + plain;
        if (province == null || city == null) {
            String[] region = NoticeRegionExtractor.extractRegion(detectSource);
            if (province == null) {
                province = region[0];
            }
            if (city == null) {
                city = region[1];
            }
        }
        notice.setProvince(province);
        notice.setCity(city);

        // 6) 行业: 源站字段优先, 兜底关键词词典
        String industry = text(record, "industryTypeText");
        notice.setIndustry(industry != null ? industry : NoticeRegionExtractor.detectIndustry(detectSource));

        // 7) 结构化字段(正文纯文本锚点, 尽力而为)
        //    项目编号优先取源站结构化字段(比正则可靠, 与 ztb_gz 优先取 UnifiedDealCode 同口径)
        String projectNo = text(record, "tenderProjectCode");
        if (projectNo == null) {
            projectNo = GgzyHtmlSupport.firstGroup(PROJECT_NO, plain);
        }
        notice.setProjectNo(GgzyHtmlSupport.clean(projectNo));
        notice.setBudget(parseBudget(plain));
        notice.setTenderPerson(GgzyHtmlSupport.clean(GgzyHtmlSupport.firstGroup(TENDER_PERSON, plain)));
        notice.setAgency(GgzyHtmlSupport.clean(GgzyHtmlSupport.firstGroup(AGENCY, plain)));
        notice.setContact(GgzyHtmlSupport.clean(GgzyHtmlSupport.firstGroup(CONTACT, plain)));
        notice.setContactPhone(GgzyHtmlSupport.clean(GgzyHtmlSupport.firstGroup(PHONE, plain)));

        // 8) 截止/开标时间(源站常合并为一个锚点, 此时两者同值 —— 与 ccgp 同样的设计事实)
        notice.setDeadline(GgzyHtmlSupport.parseDateTime(GgzyHtmlSupport.firstGroup(DEADLINE, plain)));
        notice.setOpenTime(GgzyHtmlSupport.parseDateTime(GgzyHtmlSupport.firstGroup(OPEN_TIME, plain)));

        // 9) 附件(正文内锚点, 尽力而为 —— 正文来自各省平台, 无统一附件区块)
        notice.setAttachments(parseAttachments(content, sourceUrl));

        return notice;
    }

    // ==================== 类型判定 ====================

    /**
     * 三级降级: 显式码表 → 类型文本关键词 → 频道兜底
     *
     * 与 ztb_gz 的 {@code BTypeCode → BTypeCategory → 频道 type} 同一思路。
     */
    private static String detectType(String code, String text, String fallback) {
        if (code != null) {
            String mapped = TYPE_BY_CODE.get(code.strip());
            if (mapped != null) {
                return mapped;
            }
        }
        String byText = detectTypeByText(text);
        if (byText != null) {
            return byText;
        }
        return fallback != null ? fallback : "tender";
    }

    /** 类型文本关键词(码表未收录时的降级路径, 故要能容忍源站新增码) */
    private static String detectTypeByText(String text) {
        if (text == null) {
            return null;
        }
        if (text.contains("中标") || text.contains("成交") || text.contains("结果") || text.contains("评标")) {
            return "win";
        }
        if (text.contains("澄清") || text.contains("更正") || text.contains("变更") || text.contains("废标")
                || text.contains("终止") || text.contains("补遗") || text.contains("答疑")) {
            return "change";
        }
        if (text.contains("招标") || text.contains("资审") || text.contains("采购") || text.contains("出让")
                || text.contains("挂牌") || text.contains("披露") || text.contains("公告") || text.contains("询价")
                || text.contains("谈判") || text.contains("磋商")) {
            return "tender";
        }
        return null;
    }

    // ==================== 附件 ====================

    /**
     * 扫描正文内的附件锚点
     *
     * 与 ccgp 的差别: ccgp 的附件块有 {@code ignore=1} 标记可精确圈定, 该站没有 —— 正文来自全国
     * 各省级交易平台, 无统一附件区块。故只能保守判定: **href 或锚文本以白名单扩展名结尾**才算附件
     * (白名单见 {@link AttachmentSupport}), 其余一律丢弃, 宁可漏收也不能把普通链接当附件。
     */
    private static List<NoticeAttachmentDO> parseAttachments(String content, String sourceUrl) {
        List<NoticeAttachmentDO> attachments = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return attachments;
        }
        Set<String> seenUrls = new HashSet<>();
        Matcher matcher = ANCHOR.matcher(content);
        while (matcher.find()) {
            String href = matcher.group(1);
            String anchorText = GgzyHtmlSupport.clean(GgzyHtmlSupport.stripTags(matcher.group(2)));
            if (href == null || href.isBlank()) {
                continue;
            }
            String url = resolve(href.strip(), sourceUrl);
            if (url == null) {
                continue;
            }
            // 与另两源同口径: 扩展名先看锚文本, 再看 URL
            String type = AttachmentSupport.typeFromName(anchorText);
            if (type == null) {
                type = AttachmentSupport.typeFromUrl(url);
            }
            if (type == null) {
                continue;
            }
            if (!seenUrls.add(url)) {
                continue;
            }
            NoticeAttachmentDO attachment = new NoticeAttachmentDO();
            String fileName = anchorText == null || anchorText.isBlank()
                    ? AttachmentSupport.fileNameFromUrl(url) : anchorText;
            attachment.setFileName(AttachmentSupport.truncate(fileName, AttachmentSupport.MAX_NAME_LENGTH));
            attachment.setFileUrl(AttachmentSupport.truncate(url, AttachmentSupport.MAX_URL_LENGTH));
            attachment.setFileType(type);
            // 源站未标注文件大小, 留空(同 ztb_gz)
            attachment.setFileSize("");
            attachment.setSort(attachments.size());
            attachments.add(attachment);
        }
        return attachments;
    }

    /**
     * 相对链接 → 绝对链接(notice_attachment.file_url 要求绝对地址)
     *
     * 以正文页 URL 为基准解析, 这样 {@code /xx/a.pdf}(根相对)与 {@code ./a.pdf}(路径相对)都能正确处理。
     */
    private static String resolve(String href, String baseUrl) {
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        if (baseUrl == null) {
            return null;
        }
        try {
            return URI.create(baseUrl).resolve(href).toString();
        } catch (Exception ex) {
            // 源站脏链接(含空格/非法字符)不应让整条公告解析失败
            return null;
        }
    }

    // ==================== 地区口径归一 ====================

    /**
     * 源站省份**全称** → 库内统一口径的**简称**
     *
     * ggzy 的 {@code provinceText} 是全称(「四川省」「广西壮族自治区」), 而库内其他源(ccgp 走的
     * {@link NoticeRegionExtractor})存的是简称(「四川」「广西」), 前端省份下拉的 value 也是简称,
     * 且后端查询是**精确相等**匹配 —— 直接存全称会让这些记录按省份筛不出来。
     * (同一个坑 2026-09 已在 ccgp 上踩过一次, 见 user-web/src/views/NoticeListPage.vue 的注释。)
     *
     * 归一办法是**把全称原样喂回 {@link NoticeRegionExtractor#extractRegion}**: 它内部的 shorten()
     * 会给出简称, 这样就不必再抄一份 31 省的对照表。识别不出时退回原值, 宁可口径不同也不丢信息。
     */
    private static String normalizeProvince(String provinceText) {
        if (provinceText == null) {
            return null;
        }
        String[] region = NoticeRegionExtractor.extractRegion(provinceText);
        return "其他".equals(region[0]) ? provinceText : region[0];
    }

    /** 城市去掉「市」后缀 —— 与 {@link NoticeRegionExtractor} 的输出口径一致(前端展示「四川·成都」) */
    private static String normalizeCity(String cityText) {
        if (cityText == null) {
            return null;
        }
        String city = cityText.strip();
        if (city.length() > 1 && city.endsWith("市")) {
            city = city.substring(0, city.length() - 1);
        }
        return city.isEmpty() ? null : city;
    }

    // ==================== 通用工具 ====================

    /** "预算金额：296000.00元" → 29.60 万元; "预算金额：120万元" → 120.00 万元 */
    private static BigDecimal parseBudget(String plain) {
        Matcher matcher = BUDGET.matcher(plain == null ? "" : plain);
        if (!matcher.find()) {
            return null;
        }
        return "元".equals(matcher.group(2))
                ? GgzyHtmlSupport.parseBudgetFromYuan(matcher.group(1))
                : GgzyHtmlSupport.parseBudgetFromWan(matcher.group(1));
    }

    /** 取列表记录里的非空文本字段 */
    private static String text(JsonNode record, String field) {
        if (record == null || record.get(field) == null || record.get(field).isNull()) {
            return null;
        }
        return GgzyHtmlSupport.orNull(record.get(field).asText());
    }

}
