package com.tenderintelligence.module.crawler.service.adapter.ccgp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 正文截断({@link CcgpHtmlSupport#extractContent})回归测试
 *
 * 固件按 2026-09-13 实测的源站结构手工构造(真实报文不入库)。关键特征是正文尾部两个闭合
 * {@code </div>} 之间**隔着换行**, 以及正文内部存在相邻的 {@code </div></div>} ——
 * 旧实现用 {@code rfind("</div></div>")} 收尾, 会命中正文内部那一处, 把其后的正文(含附件块)
 * 整段丢掉。实测 60 页中 5 页因此丢附件、2 页各丢约 1.4 万字符。
 */
class CcgpHtmlSupportTest {

    @Test
    @DisplayName("旧模板: 正文内部的 </div></div> 不能当作收尾, 尾部段落与附件块都要留下")
    void oldTemplateKeepsTailAfterInnerCloseTags() {
        String html = """
                <div class="vF_deail_maincontent">
                    <div class="vF_detail_main">
                        <div class="vF_detail_content_container">
                            <div class="vF_detail_content">
                                <div><div><p>前面的段落</p></div></div>
                                <p>采购需求：详见采购需求附件</p>
                                <p class='fjxx'>附件信息：</p><ul class="fjxx" style="color: #0065ef;"><li><p style="display:inline-block"><a href="https://x.gov.cn/a.pdf" ignore=1>采购文件.pdf</a></p><p style="display:inline-block;margin-left:20px">661.5K</p></li></ul>
                            </div>
                        </div><!--vF_detail_content_container-->
                    </div><!--vF_detail_main-->
                </div><!--vF_deail_maincontent-->
                <div class="vF_detail_relcontent mt13"><h2><p>相关公告</p></h2></div>
                """;

        String content = CcgpHtmlSupport.extractContent(html);

        assertThat(content).contains("前面的段落");
        assertThat(content).contains("采购需求：详见采购需求附件");
        assertThat(content).contains("采购文件.pdf");
        assertThat(content).contains("661.5K");
        // 推荐位在正文容器之外, 不能进正文片段
        assertThat(content).doesNotContain("相关公告");
    }

    @Test
    @DisplayName("旧模板: 完整公告正文区段都要在, 结尾停在正文容器闭合处")
    void oldTemplateEndsAtContentContainer() {
        String html = """
                <div class="vF_deail_maincontent">
                    <div class="vF_detail_main">
                        <div class="vF_detail_content_container">
                            <div class="vF_detail_content">
                                <div><div><p>一、项目基本情况</p></div></div>
                                <table><tr><td>预算金额</td><td>296,000.00元</td></tr></table>
                                <p>二、申请人的资格要求</p>
                                <p>三、联系方式</p>
                            </div>
                        </div><!--vF_detail_content_container-->
                    </div><!--vF_detail_main-->
                </div><!--vF_deail_maincontent-->
                """;

        String content = CcgpHtmlSupport.extractContent(html);

        assertThat(content).contains("二、申请人的资格要求");
        assertThat(content).contains("三、联系方式");
        assertThat(content).endsWith("</div><!--vF_deail_maincontent-->");
    }

    @Test
    @DisplayName("新模板: 附件 URL 不带扩展名也要保留, 正文里的 style 块不入库")
    void newTemplateKeepsAttachmentAndDropsStyle() {
        String html = """
                <div class="vF_detail_content_container">
                    <div class="vF_detail_content">
                        <div><div class="protect" id="noticeArea"> <style type="text/css"> #noticeArea { line-height: 2; } </style>
                            <p>项目概况</p>
                            <span style="margin-left: 4px;">附件：</span> <br />
                            <div style="margin-left: 22px;">
                                <a href="https://hljcg.hlj.gov.cn/gpx-public-file?accessCode=eyJhbGci" ignore=1>招标文件.zip</a>
                            </div>
                        </div></div>
                    </div>
                </div><!--vF_detail_content_container-->
                </div><!--vF_detail_main-->
                </div><!--vF_deail_maincontent-->
                <div class="vF_detail_relcontent mt13"><h2><p>相关公告</p></h2></div>
                """;

        String content = CcgpHtmlSupport.extractContent(html);

        assertThat(content).contains("项目概况");
        assertThat(content).contains("招标文件.zip");
        assertThat(content).doesNotContain("line-height");
        assertThat(content).doesNotContain("相关公告");
    }

    @Test
    @DisplayName("源站若去掉收尾注释, 退回旧的按闭合标签截取而不是返回空")
    void fallsBackWhenEndCommentAbsent() {
        String html = """
                <div class="vF_deail_maincontent">
                    <div class="vF_detail_content">
                        <p>正文一段</p>
                        <p>正文二段</p>
                    </div></div>
                </div>
                <div class="vF_detail_relcontent mt13">相关公告</div>
                """;

        String content = CcgpHtmlSupport.extractContent(html);

        assertThat(content).contains("正文一段");
        assertThat(content).contains("正文二段");
    }

    @Test
    @DisplayName("取不到正文容器时返回空串, 不抛异常")
    void returnsEmptyWhenNoContentContainer() {
        assertThat(CcgpHtmlSupport.extractContent(null)).isEmpty();
        assertThat(CcgpHtmlSupport.extractContent("")).isEmpty();
        assertThat(CcgpHtmlSupport.extractContent("<html><body>无容器</body></html>")).isEmpty();
    }

}
