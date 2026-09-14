package com.tenderintelligence.module.crawler.service.adapter.ggzy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeAttachmentDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 公告解析({@link GgzyNoticeParser})测试
 *
 * 列表记录固件取自 2026-09-14 对 getTradList 的实测报文(字段名与取值形态照抄); 正文页固件按
 * /information/deal/html/b/… 的实测结构手工构造。真实报文不入库。
 */
class GgzyNoticeParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DETAIL_URL =
            "https://www.ggzy.gov.cn/information/deal/html/b/510000/0101/20260911/ff8080819e42905601a0900c9a740f39.html";
    private static final String SITE_NAME = "全国公共资源交易平台";

    /** 实测正文页骨架 */
    private static String page(String contentInner) {
        return """
                <!doctype html>
                <html><body>
                    <div class="detail">
                        <h4 class="h4_o">三坐标测量机采购项目 - 国际招标公告</h4>
                        <p class="p_o"><span>发布时间：2026-09-11 18:39</span></p>
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

    /** 实测列表记录(政府采购样例形态; 各用例按需覆盖字段) */
    private static JsonNode record(String overridesJson) {
        String base = """
                {
                  "id": "ff8080819e42905601a0900c9a740f39",
                  "publishTime": "2026-09-11",
                  "informationType": "0101",
                  "informationTypeText": "招标/资审公告",
                  "province": "510000",
                  "provinceText": "四川省",
                  "city": null,
                  "cityText": null,
                  "industryTypeText": "",
                  "title": "三坐标测量机采购项目 - 国际招标公告",
                  "tenderProjectCode": null,
                  "url": "/information/deal/html/a/510000/0101/20260911/ff8080819e42905601a0900c9a740f39.html"
                }
                """;
        if (overridesJson == null) {
            return read(base);
        }
        // 用覆盖 JSON 覆盖基础固件的字段
        try {
            JsonNode override = MAPPER.readTree(overridesJson);
            com.fasterxml.jackson.databind.node.ObjectNode merged =
                    (com.fasterxml.jackson.databind.node.ObjectNode) read(base);
            override.fields().forEachRemaining(entry -> merged.set(entry.getKey(), entry.getValue()));
            return merged;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static JsonNode read(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static NoticePortalDO parse(JsonNode record, String html) {
        return GgzyNoticeParser.parse(record, html, "tender", DETAIL_URL, SITE_NAME);
    }

    // ==================== 类型映射 ====================

    @ParameterizedTest(name = "informationType={0} → {1}")
    @DisplayName("16 个实测类型码全部按码表映射(码表取自遍历 9 个类别的实测枚举)")
    @CsvSource({
            "0101, tender", "0102, tender", "0104, win", "0105, change",
            "0201, tender", "0301, tender", "0302, win", "0401, tender",
            "0402, win", "0501, tender", "2102, win", "2302, tender",
            "2501, tender", "2502, win", "9001, tender", "9002, win"
    })
    void mapsEveryMeasuredTypeCode(String code, String expected) {
        JsonNode record = record("{\"informationType\": \"" + code + "\", \"informationTypeText\": \"\"}");

        assertThat(parse(record, page("<p>正文</p>")).getType()).isEqualTo(expected);
    }

    @Test
    @DisplayName("码表未收录时降级到类型文本关键词 —— 源站新增码不应退化成兜底类型")
    void fallsBackToTypeTextForUnknownCode() {
        assertThat(parse(record("{\"informationType\":\"0199\",\"informationTypeText\":\"中标候选人公示\"}"),
                page("<p>正文</p>")).getType()).isEqualTo("win");
        assertThat(parse(record("{\"informationType\":\"0199\",\"informationTypeText\":\"答疑澄清文件\"}"),
                page("<p>正文</p>")).getType()).isEqualTo("change");
    }

    @Test
    @DisplayName("码表与关键词都识别不出时才用频道兜底类型")
    void fallsBackToChannelTypeLast() {
        NoticePortalDO notice = GgzyNoticeParser.parse(
                record("{\"informationType\":\"9999\",\"informationTypeText\":\"\"}"),
                page("<p>正文</p>"), "explore", DETAIL_URL, SITE_NAME);

        assertThat(notice.getType()).isEqualTo("explore");
    }

    // ==================== 结构化字段 ====================

    @Test
    @DisplayName("省份按简称入库 —— 存全称会让前端省份筛选恒不命中(同 ccgp 踩过的坑)")
    void normalizesProvinceToShortForm() {
        assertThat(parse(record("{\"provinceText\":\"四川省\"}"), page("<p>正文</p>")).getProvince())
                .isEqualTo("四川");
        assertThat(parse(record("{\"provinceText\":\"广西壮族自治区\"}"), page("<p>正文</p>")).getProvince())
                .isEqualTo("广西");
        assertThat(parse(record("{\"provinceText\":\"北京市\"}"), page("<p>正文</p>")).getProvince())
                .isEqualTo("北京");
    }

    @Test
    @DisplayName("地区码直取源站 province 字段(已是 6 位码)")
    void takesRegionCodeFromListField() {
        NoticePortalDO notice = parse(record(null), page("<p>正文</p>"));

        assertThat(notice.getRegionCode()).isEqualTo("510000");
        assertThat(notice.getProvince()).isEqualTo("四川");
    }

    @Test
    @DisplayName("城市去掉「市」后缀, 与 NoticeRegionExtractor 口径一致")
    void normalizesCitySuffix() {
        assertThat(parse(record("{\"cityText\":\"成都市\"}"), page("<p>正文</p>")).getCity()).isEqualTo("成都");
    }

    @Test
    @DisplayName("源站地区字段缺失时回落到公共词典的规则提取")
    void fallsBackToRegionExtractor() {
        // 城市锚点须出现在扫描起点附近: NoticeRegionExtractor 的 CITY 是非贪婪 {2,12}?市,
        // 从句中被长机构名挡住就取不到(该行为是 ccgp 也在用的既有实现, 此处只验证回落链路接通)
        NoticePortalDO notice = parse(
                record("{\"province\":null,\"provinceText\":null,\"cityText\":null,"
                        + "\"title\":\"武汉市江岸区某医院医疗设备采购项目招标公告\"}"),
                page("<p>本项目位于湖北省, 招标人为某单位。</p>"));

        assertThat(notice.getProvince()).isEqualTo("湖北");
        assertThat(notice.getCity()).isEqualTo("武汉");
    }

    @Test
    @DisplayName("行业优先取源站字段, 为空白时回落关键词词典")
    void prefersIndustryFromListField() {
        assertThat(parse(record("{\"industryTypeText\":\"房屋建筑业\"}"), page("<p>正文</p>")).getIndustry())
                .isEqualTo("房屋建筑业");
        // industryTypeText 为空白串(实测常见) → 走词典
        assertThat(parse(record(null), page("<p>本次采购为医疗设备采购项目</p>")).getIndustry())
                .isNotBlank();
    }

    // ==================== 发布时间 ====================

    @Test
    @DisplayName("发布时间优先取正文页(精确到分), 而非列表的日粒度")
    void prefersDetailPagePublishTime() {
        assertThat(parse(record(null), page("<p>正文</p>")).getPublishTime())
                .isEqualTo(LocalDateTime.of(2026, 9, 11, 18, 39));
    }

    @Test
    @DisplayName("正文页无发布时间时回落到列表日期(当日 0 点)")
    void fallsBackToListPublishDate() {
        String html = """
                <html><body><div id="mycontent"><div class="detail_content"><p>正文</p></div></div></body></html>
                """;

        assertThat(parse(record(null), html).getPublishTime())
                .isEqualTo(LocalDateTime.of(2026, 9, 11, 0, 0));
    }

    @Test
    @DisplayName("两处都取不到时也不能为 null —— publish_time 是 NOT NULL 且无默认值")
    void neverLeavesPublishTimeNull() {
        String html = """
                <html><body><div id="mycontent"><div class="detail_content"><p>正文</p></div></div></body></html>
                """;

        assertThat(parse(record("{\"publishTime\":null}"), html).getPublishTime()).isNotNull();
    }

    // ==================== 结构化字段(正文锚点) ====================

    @Test
    @DisplayName("项目编号优先取源站字段, 缺失时回落到正文锚点")
    void prefersProjectNoFieldThenAnchor() {
        assertThat(parse(record("{\"tenderProjectCode\":\"SRC-CODE-001\"}"),
                page("<p>招标项目编号:5501-264CDBHW0172</p>")).getProjectNo())
                .isEqualTo("SRC-CODE-001");
        assertThat(parse(record(null), page("<p>招标项目编号:5501-264CDBHW0172</p>")).getProjectNo())
                .isEqualTo("5501-264CDBHW0172");
    }

    @Test
    @DisplayName("预算: 「元」折算成万元, 「万元」只归一标度")
    void parsesBudgetWithUnitConversion() {
        assertThat(parse(record(null), page("<p>预算金额：296,000.00元</p>")).getBudget())
                .isEqualByComparingTo("29.60");
        assertThat(parse(record(null), page("<p>预算金额：120万元</p>")).getBudget())
                .isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("""
            源站把投标截止与开标合并写在一个锚点里时, deadline 与 openTime 同值(与 ccgp 同源同理)""")
    void parsesCombinedDeadlineAndOpenTime() {
        NoticePortalDO notice = parse(record(null), page("""
                <p>5、投标文件的递交</p>
                <p>投标截止时间（开标时间）:2026-10-10 10:00</p>
                """));

        assertThat(notice.getDeadline()).isEqualTo(LocalDateTime.of(2026, 10, 10, 10, 0));
        assertThat(notice.getOpenTime()).isEqualTo(LocalDateTime.of(2026, 10, 10, 10, 0));
    }

    @Test
    @DisplayName("招标人/代理机构/联系人/电话 从正文锚点提取")
    void parsesContactFields() {
        NoticePortalDO notice = parse(record(null), page("""
                <p>招标人:成光镜界（四川）科技有限公司</p>
                <p>招标代理机构:中咨高技术咨询中心有限公司</p>
                <p>联系人:鲜老师</p>
                <p>联系方式:028-81208678</p>
                """));

        assertThat(notice.getTenderPerson()).isEqualTo("成光镜界（四川）科技有限公司");
        assertThat(notice.getAgency()).isEqualTo("中咨高技术咨询中心有限公司");
        assertThat(notice.getContact()).isEqualTo("鲜老师");
        assertThat(notice.getContactPhone()).isEqualTo("028-81208678");
    }

    @Test
    @DisplayName("「联系人」锚点不能误命中「联系方式」")
    void contactAnchorDoesNotMatchPhoneLabel() {
        NoticePortalDO notice = parse(record(null), page("<p>联系方式:028-81208678</p>"));

        assertThat(notice.getContact()).isNull();
    }

    @Test
    @DisplayName("正文无任何结构化锚点时字段为 null, 但不抛异常")
    void toleratesContentWithoutAnchors() {
        NoticePortalDO notice = parse(record(null), page("<p>本公告无结构化字段。</p>"));

        assertThat(notice.getProjectNo()).isNull();
        assertThat(notice.getBudget()).isNull();
        assertThat(notice.getDeadline()).isNull();
        assertThat(notice.getTenderPerson()).isNull();
        assertThat(notice.getContent()).contains("本公告无结构化字段");
    }

    @Test
    @DisplayName("标题优先取列表字段, 缺失时回落到正文页 h4_o")
    void prefersTitleFromListThenPage() {
        assertThat(parse(record(null), page("<p>正文</p>")).getTitle())
                .isEqualTo("三坐标测量机采购项目 - 国际招标公告");
        assertThat(parse(record("{\"title\":null}"), page("<p>正文</p>")).getTitle())
                .isEqualTo("三坐标测量机采购项目 - 国际招标公告");
    }

    // ==================== 附件 ====================

    @Test
    @DisplayName("正文内的附件锚点相对路径要补成绝对地址(notice_attachment.file_url 要求绝对)")
    void resolvesRelativeAttachmentUrl() {
        NoticePortalDO notice = parse(record(null), page(
                "<p><a href=\"/information/upload/a.pdf\">采购文件.pdf</a></p>"));

        List<NoticeAttachmentDO> attachments = notice.getAttachments();
        assertThat(attachments).hasSize(1);
        assertThat(attachments.get(0).getFileUrl())
                .isEqualTo("https://www.ggzy.gov.cn/information/upload/a.pdf");
        assertThat(attachments.get(0).getFileType()).isEqualTo("pdf");
        assertThat(attachments.get(0).getFileName()).isEqualTo("采购文件.pdf");
    }

    @Test
    @DisplayName("非附件链接不留存 —— 该站正文来自各省平台, 无统一附件区块, 只能靠扩展名保守判定")
    void ignoresLinksWithoutFileExtension() {
        NoticePortalDO notice = parse(record(null), page("""
                <p>详见 <a href="https://www.gov.cn/zhengce/index.html">相关政策</a>,
                   以及 <a href="https://zzgjs.aipuret.cn/">获取招标文件</a>。</p>
                """));

        assertThat(notice.getAttachments()).isEmpty();
    }

    @Test
    @DisplayName("同一 URL 重复列出时只留一条")
    void deduplicatesAttachmentsByUrl() {
        NoticePortalDO notice = parse(record(null), page("""
                <a href="https://x.gov.cn/a.pdf">甲.pdf</a><a href="https://x.gov.cn/a.pdf">甲副本.pdf</a>
                """));

        assertThat(notice.getAttachments()).hasSize(1);
    }

}
