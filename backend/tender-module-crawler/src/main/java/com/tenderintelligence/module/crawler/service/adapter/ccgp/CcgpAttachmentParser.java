package com.tenderintelligence.module.crawler.service.adapter.ccgp;

import com.tenderintelligence.module.crawler.service.AttachmentSupport;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeAttachmentDO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中国政府采购网 —— 附件链接解析
 *
 * 2026-09-13 实测 60 页(地方 dfgg / 中央 zygg 两套模板, 22 页即 37% 带附件):
 * <ul>
 *   <li>附件锚点统一带 {@code ignore=1} 属性 —— 这是源站给附件链接打的标记,
 *       正文里的其它链接(相关政策、源站导航)都没有, 故以它作为筛选入口</li>
 *   <li>旧模板(表格式)在正文尾部以「附件信息：」标题加 {@code ul.fjxx} 列表列出附件;
 *       锚文本是文件名, 同一条目内紧跟一个写文件大小的元素</li>
 *   <li>新模板({@code #noticeArea})写作「附件：」加 {@code ignore=1} 链接,
 *       <b>URL 不含扩展名</b>(形如 {@code /gpx-public-file?accessCode=…}), 扩展名只在锚文本里 ——
 *       只看 URL 会漏掉这一整类</li>
 *   <li>少数 {@code ignore=1} 链接是"获取采购文件"之类的源站落地页(锚文本与 URL 都无扩展名),
 *       按"必须有扩展名证据"过滤 —— 平台只列真正可下载的附件</li>
 * </ul>
 *
 * 只解析文件名/直链/类型/大小, <b>不下载文件本体</b>(边界见 doc/技术/06-数据采集设计.md §十)。
 */
final class CcgpAttachmentParser {

    private CcgpAttachmentParser() {
    }

    /** 附件锚点(带 ignore=1), 锚文本即文件名 */
    private static final Pattern ATTACHMENT_ANCHOR =
            Pattern.compile("<a\\b([^>]*\\bignore\\s*=\\s*1[^>]*)>(.*?)</a>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern HREF = Pattern.compile("href\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    /** 旧模板附件条目里的文件大小: <p style="display:inline-block;margin-left:20px">546.3K</p> */
    private static final Pattern FILE_SIZE =
            Pattern.compile(">\\s*(\\d+(?:\\.\\d+)?)\\s*([KMG])\\s*B?\\s*<", Pattern.CASE_INSENSITIVE);
    /** 文件大小只在该锚点之后的一小段内找(同一条目), 免得不小心读到正文里的数字 */
    private static final int SIZE_WINDOW = 200;

    /**
     * 从正文片段解析附件列表
     *
     * 传入的应是 {@link CcgpHtmlSupport#extractContent} 截出的正文区段 —— 附件块位于正文尾部,
     * 而"相关公告"推荐位里的链接也在正文之外, 用正文片段做输入天然把它们排除掉。
     *
     * @param content 正文 HTML 片段
     * @return 附件列表(按源站出现次序; 无附件时为空列表)
     */
    static List<NoticeAttachmentDO> parse(String content) {
        List<NoticeAttachmentDO> attachments = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return attachments;
        }
        Set<String> seenUrls = new HashSet<>();
        Matcher matcher = ATTACHMENT_ANCHOR.matcher(content);
        while (matcher.find()) {
            Matcher href = HREF.matcher(matcher.group(1));
            if (!href.find()) {
                continue;
            }
            String url = unescape(href.group(1)).strip();
            if (url.isEmpty() || url.startsWith("#") || url.startsWith("javascript:")) {
                continue;
            }
            String name = toText(matcher.group(2));
            // 扩展名优先取自锚文本: 新模板的 URL 是 ?accessCode=… 形态, 不含扩展名
            String type = AttachmentSupport.typeFromName(name);
            if (type == null) {
                type = AttachmentSupport.typeFromUrl(url);
            }
            if (type == null) {
                // 无扩展名证据的 ignore=1 链接(如"获取采购文件"的源站落地页)不是附件
                continue;
            }
            if (!seenUrls.add(url)) {
                continue;
            }
            NoticeAttachmentDO attachment = new NoticeAttachmentDO();
            String fileName = name.isEmpty() ? AttachmentSupport.fileNameFromUrl(url) : name;
            attachment.setFileName(AttachmentSupport.truncate(fileName, AttachmentSupport.MAX_NAME_LENGTH));
            attachment.setFileUrl(AttachmentSupport.truncate(url, AttachmentSupport.MAX_URL_LENGTH));
            attachment.setFileType(type);
            attachment.setFileSize(fileSize(content, matcher.end()));
            attachment.setSort(attachments.size());
            attachments.add(attachment);
        }
        return attachments;
    }

    /**
     * 锚点之后的同条目文件大小
     *
     * 只在 {@link #SIZE_WINDOW} 字符内找, 且遇到 {@code </li>} 即截断 —— 旧模板一个 {@code <ul>}
     * 可能只有一条 {@code <li>}, 但新模板会连着列多个 {@code <ul>}, 不设边界会读到下一条目的大小。
     * 源站未标注大小(新模板就是如此)时返回空串。
     */
    private static String fileSize(String content, int from) {
        String window = content.substring(from, Math.min(content.length(), from + SIZE_WINDOW));
        int itemEnd = window.indexOf("</li>");
        if (itemEnd >= 0) {
            window = window.substring(0, itemEnd);
        }
        Matcher matcher = FILE_SIZE.matcher(window);
        return matcher.find() ? matcher.group(1) + matcher.group(2).toUpperCase() : "";
    }

    private static String toText(String html) {
        return unescape(html.replaceAll("<[^>]+>", "")).strip();
    }

    /**
     * 还原属性/文本里的常见实体
     *
     * {@code &amp;} 放最后替换: 放前面会把 {@code &amp;lt;} 解成 {@code <}, 或者把刚还原出的
     * {@code &} 再当成实体开头重扫一遍。反序替换只走一趟, 无此问题。
     */
    private static String unescape(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf('&') < 0) {
            return value;
        }
        return value.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&nbsp;", " ").replace("&amp;", "&");
    }

}
