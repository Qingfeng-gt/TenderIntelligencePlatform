package com.tenderintelligence.module.crawler.service;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 附件解析共用规则: 认哪些扩展名、怎么从 URL/文本里取扩展名与文件名、字段长度上限
 *
 * 两个适配器的附件来源完全不同 —— ccgp 从详情页正文的附件锚点取, ztb_gz 从详情 JSON 的
 * {@code UploadFile}/{@code PdfFile} 字段取 —— 但"什么算附件""附件类型怎么写"必须同一口径。
 * 分散在各解析器里会漂移(与 user-web 行业列表相对 {@code NoticeRegionExtractor} 漂移同类问题)。
 *
 * 平台只保存附件的元信息与源站直链, 不下载文件本体(边界见 doc/技术/06-数据采集设计.md §十)。
 */
public final class AttachmentSupport {

    private AttachmentSupport() {
    }

    /** 认作附件的扩展名; 不在表内的一律不认, 宁可少列不可列错 */
    private static final Set<String> FILE_TYPES = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "rar", "7z", "txt", "csv");

    /** URL 路径尾部的扩展名(查询串/锚点前) */
    private static final Pattern EXT_IN_URL =
            Pattern.compile("\\.([a-z0-9]{2,5})(?=$|[?#])", Pattern.CASE_INSENSITIVE);
    /** 文件名/链接文字**结尾**的扩展名 */
    private static final Pattern EXT_AT_END =
            Pattern.compile("\\.([a-z0-9]{2,5})\\s*$", Pattern.CASE_INSENSITIVE);

    /** 字段长度上限, 与 notice_attachment 表定义一致(超长会触发 Data too long 致整条公告入库失败) */
    public static final int MAX_NAME_LENGTH = 512;
    public static final int MAX_URL_LENGTH = 1024;

    /**
     * 从 URL 判定附件类型
     *
     * 注意不少源站的下载链接**不带扩展名**(如 ccgp 的 {@code /gpx-public-file?accessCode=…}、
     * ztb_gz 的 {@code downloadFileServlet?num=…}), 扩展名只在链接文字/文件名里 —— 调用方
     * 取不到时应回落到 {@link #typeFromName} 再判一次。
     *
     * @return 小写扩展名(如 {@code pdf}); 不是受支持的附件类型时返回 null
     */
    public static String typeFromUrl(String url) {
        return normalizeType(firstGroup(EXT_IN_URL, url));
    }

    /**
     * 从文件名(或链接文字)判定附件类型
     *
     * 只认**结尾**的扩展名: 链接文字可能是整句话("…采购项目(2).pdf"), 取到中间的点会把
     * 句子里的「1.2」之类误当扩展名。
     *
     * @return 小写扩展名; 不是受支持的附件类型时返回 null
     */
    public static String typeFromName(String name) {
        return normalizeType(firstGroup(EXT_AT_END, name));
    }

    /**
     * 归一化扩展名: 去空白与点、转小写后校验是否属于受支持类型
     *
     * @return 小写扩展名; 不受支持时返回 null
     */
    private static String normalizeType(String rawType) {
        if (rawType == null) {
            return null;
        }
        String type = rawType.strip().toLowerCase();
        while (type.startsWith(".")) {
            type = type.substring(1);
        }
        return FILE_TYPES.contains(type) ? type : null;
    }

    /**
     * 从 URL 末段取文件名(源站未给文件名时的兜底)
     */
    public static String fileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        int query = url.indexOf('?');
        String path = query >= 0 ? url.substring(0, query) : url;
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.isEmpty() ? url : name;
    }

    public static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String firstGroup(Pattern pattern, String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

}
