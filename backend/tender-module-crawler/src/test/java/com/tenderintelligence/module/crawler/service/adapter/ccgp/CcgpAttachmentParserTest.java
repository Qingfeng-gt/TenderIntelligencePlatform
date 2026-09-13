package com.tenderintelligence.module.crawler.service.adapter.ccgp;

import com.tenderintelligence.module.notice.dal.dataobject.NoticeAttachmentDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 附件解析({@link CcgpAttachmentParser})测试
 *
 * 固件按 2026-09-13 实测的两套模板结构手工构造: 附件锚点带 {@code ignore=1}, 文件名在锚文本里,
 * 旧模板同条目内还有文件大小; 新模板的 URL 不含扩展名(形如 {@code ?accessCode=…})。
 */
class CcgpAttachmentParserTest {

    @Test
    @DisplayName("旧模板: 取到文件名/类型/大小")
    void parsesOldTemplateEntry() {
        String content = """
                <p class='fjxx'>附件信息：</p><ul class="fjxx" style="color: #0065ef;list-style-type: none;"><li><p style="display:inline-block"><a href="https://sx2gov2open2doc.uos.sxzfcg.zcygov.cn/1024FPA/149900/20269/5b1c0751.pdf" ignore=1>10.8采购文件2-某局器材采购项目(2).pdf</a></p><p style="display:inline-block;margin-left:20px">661.5K</p></li></ul>
                """;

        List<NoticeAttachmentDO> attachments = CcgpAttachmentParser.parse(content);

        assertThat(attachments).hasSize(1);
        NoticeAttachmentDO attachment = attachments.get(0);
        assertThat(attachment.getFileName()).isEqualTo("10.8采购文件2-某局器材采购项目(2).pdf");
        assertThat(attachment.getFileUrl()).isEqualTo("https://sx2gov2open2doc.uos.sxzfcg.zcygov.cn/1024FPA/149900/20269/5b1c0751.pdf");
        assertThat(attachment.getFileType()).isEqualTo("pdf");
        assertThat(attachment.getFileSize()).isEqualTo("661.5K");
        assertThat(attachment.getSort()).isZero();
    }

    @Test
    @DisplayName("新模板: URL 不含扩展名, 类型要能从锚文本取到")
    void resolvesTypeFromLinkTextWhenUrlHasNoExtension() {
        String content = """
                <span style="margin-left: 4px;">附件：</span> <br /> <div style="margin-left: 22px;"> <a href="https://hljcg.hlj.gov.cn/gpx-public-file?accessCode=eyJhbGciOiJNRDUiLCJ0eXBlIjoiMDQifQ==" ignore=1>某项目招标文件.zip</a> </div>
                """;

        List<NoticeAttachmentDO> attachments = CcgpAttachmentParser.parse(content);

        assertThat(attachments).hasSize(1);
        assertThat(attachments.get(0).getFileType()).isEqualTo("zip");
        assertThat(attachments.get(0).getFileName()).isEqualTo("某项目招标文件.zip");
    }

    @Test
    @DisplayName("类型也可取自 URL 路径; 大小不串到下一条目")
    void parsesMultipleEntriesWithTheirOwnSizes() {
        String content = """
                <ul class="fjxx"><li><p><a href="https://x.gov.cn/a.pdf" ignore=1>甲.pdf</a></p><p>546.3K</p></li></ul><ul class="fjxx"><li><p><a href="https://x.gov.cn/b.docx" ignore=1>乙文件</a></p><p>3.7M</p></li></ul>
                """;

        List<NoticeAttachmentDO> attachments = CcgpAttachmentParser.parse(content);

        assertThat(attachments).hasSize(2);
        assertThat(attachments.get(0).getFileType()).isEqualTo("pdf");
        assertThat(attachments.get(0).getFileSize()).isEqualTo("546.3K");
        // 第二条锚文本无扩展名, 类型回落到 URL 路径
        assertThat(attachments.get(1).getFileType()).isEqualTo("docx");
        assertThat(attachments.get(1).getFileSize()).isEqualTo("3.7M");
        assertThat(attachments.get(1).getSort()).isEqualTo(1);
    }

    @Test
    @DisplayName("""
            无扩展名证据的 ignore=1 链接不是附件 —— 如实测到的"获取采购文件"源站落地页""")
    void skipsIgnoreLinksThatAreNotFiles() {
        String content = """
                <a href="https://home.zfcg.sh.gov.cn/bidding-entrust/#/acquirepurfile/detail?prId=1" ignore=1>获取采购文件</a>
                """;

        assertThat(CcgpAttachmentParser.parse(content)).isEmpty();
    }

    @Test
    @DisplayName("不带 ignore=1 的正文链接一律不算附件")
    void ignoresPlainLinks() {
        String content = """
                <p>详见 <a href="https://www.gov.cn/zhengce/a.pdf">相关政策文件</a>, 以及 <a href="https://www.gov.cn/">首页</a>。</p>
                """;

        assertThat(CcgpAttachmentParser.parse(content)).isEmpty();
    }

    @Test
    @DisplayName("扩展名不在白名单的不收(如 .exe)")
    void skipsUnsupportedExtensions() {
        String content = """
                <a href="https://x.gov.cn/setup.exe" ignore=1>安装包.exe</a>
                """;

        assertThat(CcgpAttachmentParser.parse(content)).isEmpty();
    }

    @Test
    @DisplayName("同一 URL 重复列出时只留一条")
    void deduplicatesByUrl() {
        String content = """
                <a href="https://x.gov.cn/a.pdf" ignore=1>甲.pdf</a><a href="https://x.gov.cn/a.pdf" ignore=1>甲副本.pdf</a><a href="https://x.gov.cn/b.pdf" ignore=1>乙.pdf</a>
                """;

        List<NoticeAttachmentDO> attachments = CcgpAttachmentParser.parse(content);

        assertThat(attachments).hasSize(2);
        assertThat(attachments).extracting(NoticeAttachmentDO::getFileUrl)
                .containsExactly("https://x.gov.cn/a.pdf", "https://x.gov.cn/b.pdf");
    }

    @Test
    @DisplayName("URL 里的 &amp; 要还原成 &, 否则直链打不开")
    void unescapesUrlEntities() {
        String content = """
                <a href="https://x.gov.cn/dl?num=8b80&amp;flag=isWebsite" ignore=1>结果公告.pdf</a>
                """;

        List<NoticeAttachmentDO> attachments = CcgpAttachmentParser.parse(content);

        assertThat(attachments).hasSize(1);
        assertThat(attachments.get(0).getFileUrl()).isEqualTo("https://x.gov.cn/dl?num=8b80&flag=isWebsite");
    }

    @Test
    @DisplayName("锚文本只有扩展名从 URL 取类型时, 文件名回落到 URL 末段")
    void fallsBackToUrlForFileName() {
        String content = """
                <a href="https://x.gov.cn/1024FPA/20269/采购文件.pdf" ignore=1></a>
                """;

        List<NoticeAttachmentDO> attachments = CcgpAttachmentParser.parse(content);

        assertThat(attachments).hasSize(1);
        assertThat(attachments.get(0).getFileName()).isEqualTo("采购文件.pdf");
    }

    @Test
    @DisplayName("正文为空或 null 时返回空列表")
    void handlesEmptyInput() {
        assertThat(CcgpAttachmentParser.parse(null)).isEmpty();
        assertThat(CcgpAttachmentParser.parse("")).isEmpty();
        assertThat(CcgpAttachmentParser.parse("<p>本节无附件</p>")).isEmpty();
    }

}
