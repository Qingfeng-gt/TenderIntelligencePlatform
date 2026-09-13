package com.tenderintelligence.module.crawler.service.adapter.ztb_gz;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenderintelligence.module.crawler.service.AttachmentSupport;
import com.tenderintelligence.module.crawler.service.NoticeRegionExtractor;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeAttachmentDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 贵州省招标投标公共服务平台(ztb.guizhou.gov.cn)公告解析器
 *
 * 数据源为 JSON API: GET /api/trade/GetDetail/{Id}(2026-09-08 实测字段):
 * Id, AfficheId, Title, ReadCount, BTypeCategory(affiche/publicity), BTypeCode(A01..Z04),
 * ProjectType(A/Z/D/B/C/D4), RegionCode, PublishBy, PublishDate(yyyy-MM-dd), Source(发布机构),
 * Content(HTML), UnifiedDealCode(统一交易码), Summary, UploadFile, PdfFile 等
 *
 * 结构化字段(预算/截止时间/招标人等)源站无独立字段, 从正文纯文本用通用中文锚点提取;
 * 地区/行业复用 {@link NoticeRegionExtractor} 公共词典
 */
@Slf4j
public final class GuizhouZtbNoticeParser {

    private GuizhouZtbNoticeParser() {
    }

    /** 通用字段锚点(在去标签后的正文纯文本上匹配) */
    private static final Pattern PROJECT_NO =
            Pattern.compile("(项目编号|招标编号|交易编号|统一交易码)[：:]?\\s*([A-Za-z0-9][A-Za-z0-9-]{4,})");
    private static final Pattern BUDGET = Pattern.compile("(预算金额|项目预算|采购预算)[：:]?\\s*([\\d,]+(?:\\.[\\d]+)?)\\s*(元|万元|万)");
    private static final Pattern TENDER_PERSON = Pattern.compile("(招标人|采购人|建设单位|招标单位)[：:]\\s*([^\\s。；]{2,})");
    private static final Pattern AGENCY = Pattern.compile("(招标代理机构|代理机构|招标代理)[：:]\\s*([^\\s。；]{2,})");
    private static final Pattern CONTACT = Pattern.compile("(项目联系人|联系人)[：:]\\s*([^\\s。；]{2,})");
    private static final Pattern PHONE = Pattern.compile("(联系电话|电话)[：:]?\\s*([\\d-]{6,14})");
    private static final Pattern DEADLINE_ANCHOR =
            Pattern.compile("(投标截止时间|递交截止时间|截止时间)\\s*?(\\d{4}[年/-]\\d{1,2}[月/-]\\d{1,2}日?)");
    private static final Pattern OPEN_ANCHOR =
            Pattern.compile("(开标时间|开标日期)\\s*?(\\d{4}[年/-]\\d{1,2}[月/-]\\d{1,2}日?)");
    private static final Pattern DATE_FULL =
            Pattern.compile("(\\d{4})[年/-](\\d{1,2})[月/-](\\d{1,2})日?(?:\\s*(\\d{1,2})[时:]\\s*(\\d{1,2})分?)?");

    /**
     * 贵州 6 市 + 3 州(RegionCode 前 4 位 → 城市名; 见源站 regions 列表)
     */
    private static final String[][] GZ_REGION_CITY = {
            {"520100", "贵阳市"}, {"520200", "六盘水市"}, {"520300", "遵义市"}, {"520400", "安顺市"},
            {"520500", "毕节市"}, {"520600", "铜仁市"}, {"522300", "黔西南州"}, {"522600", "黔东南州"},
            {"522700", "黔南州"}, {"520000", ""}
    };

    /**
     * 解析详情 JSON → 公告 DO
     *
     * @param detail   GetDetail API 返回的 JSON 对象
     * @param fallback 频道兜底类型(仅当 BTypeCode 无法识别时使用)
     * @param sourceUrl 源站展示页 URL(唯一去重键)
     * @param source   来源网站名称
     */
    public static NoticePortalDO parse(JsonNode detail, String fallback, String sourceUrl, String source) {
        NoticePortalDO notice = new NoticePortalDO();
        notice.setSourceUrl(sourceUrl);
        notice.setSource(source);

        String title = text(detail, "Title");
        String content = text(detail, "Content");
        notice.setTitle(title == null ? "" : title.trim());
        notice.setContent(content == null ? "" : content);
        notice.setRegionCode(text(detail, "RegionCode"));

        // 1) 类型: 优先 BTypeCode, 兜底 BTypeCategory / 频道类型
        notice.setType(detectType(text(detail, "BTypeCode"), text(detail, "BTypeCategory"), fallback));

        // 2) 结构化字段(正文纯文本锚点; ProjectNo 优先源站统一交易码)
        String plain = stripTags(content);
        notice.setProjectNo(clean(notEmpty(text(detail, "UnifiedDealCode")) ? text(detail, "UnifiedDealCode")
                : first(PROJECT_NO, plain)));
        notice.setBudget(parseBudget(plain));
        notice.setTenderPerson(clean(first(TENDER_PERSON, plain)));
        notice.setAgency(clean(notEmpty(first(AGENCY, plain)) ? first(AGENCY, plain)
                : text(detail, "Source"))); // 兜底: 源站的发布机构字段
        notice.setContact(clean(first(CONTACT, plain)));
        notice.setContactPhone(clean(first(PHONE, plain)));

        // 3) 截止/开标时间
        notice.setDeadline(parseDateTime(first(DEADLINE_ANCHOR, plain)));
        notice.setOpenTime(parseDateTime(first(OPEN_ANCHOR, plain)));

        // 4) 发布时间(源站字段 yyyy-MM-dd; 缺省用当前时间)
        notice.setPublishTime(parseDate(text(detail, "PublishDate")));

        // 5) 地区(固定贵州, 城市按 RegionCode)/行业(公共词典)
        notice.setProvince("贵州");
        notice.setCity(regionCity(notice.getRegionCode()));
        String detectSource = notice.getTitle() + " " + plain;
        notice.setIndustry(NoticeRegionExtractor.detectIndustry(detectSource));

        // 6) 附件(源站 UploadFile / PdfFile 字段)
        notice.setAttachments(parseAttachments(detail));

        return notice;
    }

    // ==================== 附件(源站 UploadFile / PdfFile 字段) ====================

    /**
     * 承载附件的源站字段
     *
     * ⚠️ 2026-09-13 复测: search 列表接口仍返回 {@code totalNum = 0}(列表数据未对外开放),
     * 取不到真实详情报文, 故这两个字段的**实际结构未经实测**, 只能按常见形态容错:
     * 纯字符串 URL、URL 字符串数组、对象数组(文件名与地址的键名见下)。源站恢复后若日志出现
     * 「附件字段无法识别」告警, 按告警报文里的真实结构收敛下面的候选键。
     */
    private static final String[] ATTACHMENT_FIELDS = {"UploadFile", "PdfFile"};
    /** 附件对象里可能承载文件名的键 */
    private static final String[] ATTACHMENT_NAME_KEYS =
            {"FileName", "Name", "OldFileName", "Title", "fileName", "name", "title"};
    /** 附件对象里可能承载下载地址的键 */
    private static final String[] ATTACHMENT_URL_KEYS =
            {"Url", "FileUrl", "FilePath", "Path", "DownloadUrl", "File", "url", "fileUrl", "filePath", "path"};

    /** 解析详情 JSON 里的附件字段 */
    private static List<NoticeAttachmentDO> parseAttachments(JsonNode detail) {
        List<NoticeAttachmentDO> attachments = new ArrayList<>();
        if (detail == null) {
            return attachments;
        }
        Set<String> seenUrls = new HashSet<>();
        for (String field : ATTACHMENT_FIELDS) {
            JsonNode node = detail.get(field);
            if (node == null || node.isNull()) {
                continue;
            }
            collectAttachments(field, node, attachments, seenUrls);
        }
        return attachments;
    }

    /** 收一个附件字段下的所有条目(字符串 / 数组 / 对象三种形态都吃) */
    private static void collectAttachments(String field, JsonNode node, List<NoticeAttachmentDO> attachments,
                                           Set<String> seenUrls) {
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectAttachments(field, item, attachments, seenUrls);
            }
            return;
        }
        String url;
        String name = null;
        if (node.isObject()) {
            url = firstText(node, ATTACHMENT_URL_KEYS);
            name = firstText(node, ATTACHMENT_NAME_KEYS);
        } else {
            url = node.asText();
        }
        if (url == null || url.isBlank()) {
            return;
        }
        url = url.strip();
        // 与 ccgp 同口径: 扩展名先看文件名, 再看 URL(zcygov 的 downloadFileServlet 链接不带扩展名)
        String type = AttachmentSupport.typeFromName(name);
        if (type == null) {
            type = AttachmentSupport.typeFromUrl(url);
        }
        if (type == null) {
            // 源站字段结构未经实测, 认不出时留下原文, 便于源站恢复后按真实报文收敛解析
            log.warn("[crawler] ztb_gz {} 字段存在但认不出附件类型, 原文: {}", field, abbreviate(url));
            return;
        }
        if (!seenUrls.add(url)) {
            return;
        }
        NoticeAttachmentDO attachment = new NoticeAttachmentDO();
        String fileName = name == null || name.isBlank() ? AttachmentSupport.fileNameFromUrl(url) : name.strip();
        attachment.setFileName(AttachmentSupport.truncate(fileName, AttachmentSupport.MAX_NAME_LENGTH));
        attachment.setFileUrl(AttachmentSupport.truncate(url, AttachmentSupport.MAX_URL_LENGTH));
        attachment.setFileType(type);
        // 源站未标注文件大小(该站结构如此), 留空
        attachment.setFileSize("");
        attachment.setSort(attachments.size());
        attachments.add(attachment);
    }

    /** 按候选键取第一个非空文本值 */
    private static String firstText(JsonNode node, String[] keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && value.isValueNode() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private static String abbreviate(String value) {
        return value.length() <= 200 ? value : value.substring(0, 200) + "…";
    }

    // ==================== 类型映射(源站 allInfoCategorys) ====================
    private static final String[] TENDER_CODES = {"A01", "A00", "D01", "D00", "D0A", "D41", "D40", "D4A", "Z01", "B01", "C01"};
    private static final String[] CHANGE_CODES = {"A02", "A05", "D02", "D04", "D42", "D44", "Z02", "Z04", "AP2"};
    private static final String[] WIN_CODES = {"A03", "A04", "D03", "D43", "Z03", "B02", "C02"};

    private static String detectType(String btypeCode, String btypeCategory, String fallback) {
        if (btypeCode != null) {
            for (String code : TENDER_CODES) {
                if (code.equals(btypeCode)) {
                    return "tender";
                }
            }
            for (String code : CHANGE_CODES) {
                if (code.equals(btypeCode)) {
                    return "change";
                }
            }
            for (String code : WIN_CODES) {
                if (code.equals(btypeCode)) {
                    return "win";
                }
            }
        }
        if ("publicity".equals(btypeCategory)) {
            return "win";
        }
        if ("affiche".equals(btypeCategory)) {
            return "tender";
        }
        return fallback != null ? fallback : "tender";
    }

    // ==================== 通用工具 ====================

    /** 删除全部 HTML 标签(ztb 正文 <span> 会打断纯文本, 必须移除而非替换空格) */
    private static String stripTags(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&ldquo;", "“").replace("&rdquo;", "”");
    }

    private static String first(Pattern pattern, String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(matcher.groupCount() >= 2 ? 2 : 1) : null;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        String value = node.get(field).asText();
        return value.isEmpty() ? null : value;
    }

    private static String clean(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean notEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    /** "预算金额：296000.00元" → 29.60 万元; "预算金额：120万元" → 120.00 万元 */
    private static BigDecimal parseBudget(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = BUDGET.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(matcher.group(2).replace(",", ""));
            String unit = matcher.group(3);
            if ("元".equals(unit)) {
                amount = amount.divide(BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP);
            }
            return amount;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** 只接收已匹配的完整日期串("2017年12月25日" / "2017-12-25") */
    private static LocalDateTime parseDateTime(String matched) {
        if (matched == null) {
            return null;
        }
        Matcher matcher = DATE_FULL.matcher(matched);
        if (!matcher.find()) {
            return null;
        }
        LocalDate date = LocalDate.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)));
        if (matcher.group(4) != null) {
            return date.atTime(Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)));
        }
        return date.atStartOfDay();
    }

    /** yyyy-MM-dd */
    private static LocalDateTime parseDate(String value) {
        if (value == null) {
            return LocalDateTime.now();
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (Exception ex) {
            return LocalDateTime.now();
        }
    }

    private static String regionCity(String regionCode) {
        if (regionCode != null && regionCode.length() >= 4) {
            String prefix = regionCode.substring(0, 6);
            for (String[] entry : GZ_REGION_CITY) {
                if (entry[0].equals(prefix)) {
                    return entry[1];
                }
            }
            String provincePrefix = regionCode.substring(0, 4);
            for (String[] entry : GZ_REGION_CITY) {
                if (entry[0].equals(provincePrefix + "00")) {
                    return entry[1];
                }
            }
        }
        return "";
    }
}
