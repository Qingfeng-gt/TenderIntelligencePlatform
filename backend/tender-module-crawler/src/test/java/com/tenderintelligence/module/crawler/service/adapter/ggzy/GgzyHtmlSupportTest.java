package com.tenderintelligence.module.crawler.service.adapter.ggzy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 详情页取值({@link GgzyHtmlSupport})测试
 *
 * 固件按 2026-09-14 实测的 /information/deal/html/b/… 正文页结构手工构造(真实报文不入库):
 * 标题在 {@code h4.h4_o}、发布时间与信息来源在 {@code p.p_o}、正文在 {@code div.detail_content}。
 */
class GgzyHtmlSupportTest {

    /** 实测正文页骨架(缩进与标签形态照抄) */
    private static String page(String contentInner) {
        return """
                <!doctype html>
                <html><head><title>全国公共资源交易平台</title></head>
                <body style="height:auto;">
                    <div class="detail">
                        <h4 class="h4_o">三坐标测量机采购项目 - 国际招标公告</h4>
                        <p class="p_o">
                            <span>发布时间：2026-09-11 18:39</span>
                            <span>
                                信息来源：<label id="platformName">中国国际招标网</label>
                            </span>
                            <span class='detail_url'>
                                <a target="_blank" href="" style='display:none;'>原文链接地址</a>
                            </span>
                        </p>
                        <div id="mycontent">
                            <div class="detail_content">
                """
                + contentInner
                + """
                            </div>
                        </div>
                    </div>
                </body></html>
                """;
    }

    @Test
    @DisplayName("正文含嵌套 div 时不能截断 —— 取错的闭合标签会只截出几十字符")
    void keepsNestedDivsInsideContent() {
        String html = page("""
                                <p>项目概况:采购三坐标测量机</p>
                                <div style="margin-left:20px">
                                    <table><tr><td>招标项目编号:5501-264CDBHW0172</td></tr></table>
                                    <div><span>嵌套更深一层</span></div>
                                </div>
                                <p>3、投标人资格要求:投标人应具备的资格或业绩</p>
                """);

        String content = GgzyHtmlSupport.extractContent(html);

        assertThat(content).contains("项目概况:采购三坐标测量机");
        assertThat(content).contains("招标项目编号:5501-264CDBHW0172");
        assertThat(content).contains("嵌套更深一层");
        assertThat(content).contains("3、投标人资格要求:投标人应具备的资格或业绩");
        // 容器自身的标签不能残留进正文
        assertThat(content).doesNotContain("detail_content");
        assertThat(content).doesNotContain("mycontent");
    }

    @Test
    @DisplayName("正文为空的详情页返回空串 —— 适配器据此跳过不入库")
    void returnsEmptyForBlankContent() {
        // 实测: 政府采购/国有产权大量记录正文区是空的(平台只汇聚元数据)
        String html = page("\n\n                ");

        assertThat(GgzyHtmlSupport.extractContent(html)).isEmpty();
    }

    @Test
    @DisplayName("没有 detail_content 容器时返回空串, 不抛异常")
    void returnsEmptyWhenContainerMissing() {
        assertThat(GgzyHtmlSupport.extractContent("<html><body><p>改版了</p></body></html>")).isEmpty();
        assertThat(GgzyHtmlSupport.extractContent(null)).isEmpty();
        assertThat(GgzyHtmlSupport.extractContent("")).isEmpty();
    }

    @Test
    @DisplayName("正文内的 style 块属页面骨架, 不入库")
    void stripsStyleBlocks() {
        String html = page("""
                                <style>.detail_content p{margin:0}</style>
                                <p>真正的正文</p>
                """);

        String content = GgzyHtmlSupport.extractContent(html);

        assertThat(content).contains("真正的正文");
        assertThat(content).doesNotContain("margin");
    }

    @Test
    @DisplayName("标题取 h4_o")
    void extractsTitle() {
        assertThat(GgzyHtmlSupport.extractTitle(page("<p>x</p>")))
                .isEqualTo("三坐标测量机采购项目 - 国际招标公告");
        assertThat(GgzyHtmlSupport.extractTitle("<html></html>")).isNull();
    }

    @Test
    @DisplayName("发布时间取到分 —— 比列表接口的日期粒度更精确")
    void extractsPublishTime() {
        assertThat(GgzyHtmlSupport.extractPublishTime(page("<p>x</p>")))
                .isEqualTo(LocalDateTime.of(2026, 9, 11, 18, 39));
    }

    @Test
    @DisplayName("信息来源(原交易平台名)取 label#platformName")
    void extractsPlatformName() {
        assertThat(GgzyHtmlSupport.extractPlatformName(page("<p>x</p>"))).isEqualTo("中国国际招标网");
        assertThat(GgzyHtmlSupport.extractPlatformName("<html></html>")).isEmpty();
    }

    @Test
    @DisplayName("列表 URL 的 a 路径改写为 b 路径(正文页)")
    void rewritesShellUrlToContentUrl() {
        assertThat(GgzyHtmlSupport.toDetailUrl(
                "/information/deal/html/a/130000/0201/20260914/2c8382a3.html"))
                .isEqualTo("/information/deal/html/b/130000/0201/20260914/2c8382a3.html");
        assertThat(GgzyHtmlSupport.toDetailUrl(null)).isNull();
    }

    @Test
    @DisplayName("""
            日期两种写法都吃: 本站的 yyyy-MM-dd 与各省模板的 yyyy年MM月dd日""")
    void parsesBothDateStyles() {
        assertThat(GgzyHtmlSupport.parseDateTime("2026-10-10 10:00"))
                .isEqualTo(LocalDateTime.of(2026, 10, 10, 10, 0));
        assertThat(GgzyHtmlSupport.parseDateTime("2026年10月10日 09时30分"))
                .isEqualTo(LocalDateTime.of(2026, 10, 10, 9, 30));
        // 无时间部分取当日 0 点
        assertThat(GgzyHtmlSupport.parseDateTime("2026-10-10"))
                .isEqualTo(LocalDateTime.of(2026, 10, 10, 0, 0));
    }

    @Test
    @DisplayName("脏日期不让解析崩掉")
    void toleratesMalformedDate() {
        assertThat(GgzyHtmlSupport.parseDateTime("2026-13-45")).isNull();
        assertThat(GgzyHtmlSupport.parseDateTime("无日期")).isNull();
        assertThat(GgzyHtmlSupport.parseDateTime(null)).isNull();
    }

    @Test
    @DisplayName("删标签锚点不补空格 —— 否则被 <span> 打断的「投标截止时间」匹配不上")
    void stripTagsDoesNotInsertSpaces() {
        assertThat(GgzyHtmlSupport.stripTags("投标<span>截止</span>时间：2026-10-10"))
                .isEqualTo("投标截止时间：2026-10-10");
    }

}
