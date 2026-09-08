package com.tenderintelligence.module.crawler.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公告地区(省/市)与行业规则提取(公共词典, 供各源站解析器复用)
 *
 * 省份按词表取文本中最早出现者; 城市取自校市词表(避免把普通机构名误判为城市);
 * 行业为标题+正文关键词词典, 均兜底"其他"
 */
public final class NoticeRegionExtractor {

    private NoticeRegionExtractor() {
    }

    /** 地区提取: 城市(非直辖市, 词表外的非直辖市城市跳过) */
    private static final Pattern CITY = Pattern.compile("([\\u4e00-\\u9fa5]{2,12}?市)");

    /** 省级名称(含带后缀形式, 用于查找文本中出现的省份, 取最早出现者) */
    private static final String[] PROVINCES = {
            "北京市", "天津市", "上海市", "重庆市",
            "河北省", "山西省", "内蒙古自治区", "辽宁省", "吉林省", "黑龙江省",
            "江苏省", "浙江省", "安徽省", "福建省", "江西省", "山东省",
            "河南省", "湖北省", "湖南省", "广东省", "广西壮族自治区", "海南省",
            "四川省", "贵州省", "云南省", "西藏自治区",
            "陕西省", "甘肃省", "青海省", "宁夏回族自治区", "新疆维吾尔自治区"
    };

    /** 城市 → 省份词典 */
    private static final String[][] CITY_PROVINCE = {
            {"武汉", "湖北"}, {"宜昌", "湖北"}, {"襄阳", "湖北"}, {"黄冈", "湖北"}, {"荆州", "湖北"},
            {"长沙", "湖南"}, {"株洲", "湖南"}, {"湘潭", "湖南"}, {"衡阳", "湖南"}, {"岳阳", "湖南"},
            {"郑州", "河南"}, {"洛阳", "河南"}, {"南阳", "河南"}, {"开封", "河南"}, {"信阳", "河南"},
            {"石家庄", "河北"}, {"唐山", "河北"}, {"邯郸", "河北"}, {"保定", "河北"}, {"沧州", "河北"},
            {"太原", "山西"}, {"大同", "山西"},
            {"济南", "山东"}, {"青岛", "山东"}, {"烟台", "山东"}, {"潍坊", "山东"}, {"临沂", "山东"},
            {"德州", "山东"}, {"滨州", "山东"}, {"济宁", "山东"}, {"威海", "山东"}, {"日照", "山东"},
            {"东营", "山东"}, {"淄博", "山东"}, {"泰安", "山东"}, {"聊城", "山东"}, {"菏泽", "山东"},
            {"南京", "江苏"}, {"苏州", "江苏"}, {"无锡", "江苏"}, {"南通", "江苏"}, {"徐州", "江苏"}, {"常州", "江苏"},
            {"杭州", "浙江"}, {"宁波", "浙江"}, {"温州", "浙江"}, {"绍兴", "浙江"}, {"嘉兴", "浙江"},
            {"合肥", "安徽"}, {"芜湖", "安徽"}, {"安庆", "安徽"}, {"蚌埠", "安徽"},
            {"南昌", "江西"}, {"九江", "江西"}, {"赣州", "江西"},
            {"福州", "福建"}, {"厦门", "福建"}, {"泉州", "福建"},
            {"广州", "广东"}, {"深圳", "广东"}, {"珠海", "广东"}, {"佛山", "广东"}, {"东莞", "广东"}, {"中山", "广东"},
            {"南宁", "广西"}, {"柳州", "广西"}, {"桂林", "广西"},
            {"海口", "海南"}, {"三亚", "海南"},
            {"成都", "四川"}, {"绵阳", "四川"}, {"德阳", "四川"}, {"宜宾", "四川"},
            {"贵阳", "贵州"}, {"遵义", "贵州"},
            {"昆明", "云南"}, {"曲靖", "云南"}, {"大理", "云南"},
            {"重庆", "重庆"},
            {"拉萨", "西藏"},
            {"西安", "陕西"}, {"宝鸡", "陕西"}, {"咸阳", "陕西"}, {"榆林", "陕西"},
            {"兰州", "甘肃"}, {"天水", "甘肃"},
            {"西宁", "青海"},
            {"银川", "宁夏"},
            {"乌鲁木齐", "新疆"},
            {"呼和浩特", "内蒙古"}, {"包头", "内蒙古"},
            {"沈阳", "辽宁"}, {"大连", "辽宁"}, {"鞍山", "辽宁"},
            {"长春", "吉林"}, {"吉林", "吉林"},
            {"哈尔滨", "黑龙江"}, {"大庆", "黑龙江"}, {"佳木斯", "黑龙江"},
            {"北京", "北京"}, {"天津", "天津"}, {"上海", "上海"}
    };

    /** 行业词典: 每行 = 行业名(第1个元素) + 关键词(其余); 文本命中任一关键词即判定为该行业 */
    private static final String[][] INDUSTRY_KEYWORDS = {
            {"医疗卫生", "医疗", "医院", "卫生院", "中医药", "药械", "手术", "试剂"},
            {"市政工程", "道路", "管网", "市政", "人行道", "排水", "桥梁", "路灯"},
            {"软件服务", "数据", "系统", "软件", "信息化", "平台", "机房", "运维"},
            {"轨道交通", "地铁", "轨道交通"},
            {"交通公路", "铁路", "隧道", "高速", "公路", "机场"},
            {"教育", "学校", "校园", "食堂", "学院"},
            {"农业水利", "农田", "灌溉", "水利", "河道", "水库"},
            {"新能源", "光伏", "风电", "电力", "输变电", "充电桩"},
            {"生态环保", "污水处理", "环保", "水环境", "垃圾"},
            {"建筑工程", "建筑", "施工", "总承包", "EPC", "房屋", "装修"},
            {"通信工程", "通信", "5G", "传输"},
            {"能源化工", "石油", "天然气", "化工", "炼化"},
            {"公共安全", "安防", "监控"},
            {"工业设备", "设备", "仪器", "机械", "装备"},
            {"咨询服务", "咨询", "监理", "勘察", "设计"}
    };

    /** 标题+正文纯文本 → [省份, 城市] */
    public static String[] extractRegion(String text) {
        // 1) 省份: 词表查找, 取文本中最早出现的省级名称
        String province = "";
        int bestIdx = Integer.MAX_VALUE;
        for (String name : PROVINCES) {
            int idx = text.indexOf(name);
            if (idx >= 0 && idx < bestIdx) {
                bestIdx = idx;
                province = name.replace("自治区", "").replace("省", "").replace("市", "");
            }
        }
        // 2) 城市: 词表内城市(整体提取, 避免把普通机构名误判为城市)
        String city = "";
        Matcher cityMatcher = CITY.matcher(text);
        while (cityMatcher.find()) {
            String candidate = cityMatcher.group(1).replace("市", "");
            String mapped = cityToProvince(candidate);
            if ("北京".equals(candidate) || "天津".equals(candidate) || "上海".equals(candidate)
                    || "重庆".equals(candidate) || mapped != null) {
                city = candidate;
                if (province.isEmpty()) {
                    province = mapped != null ? mapped : candidate; // 直辖市: 省=市
                }
                break;
            }
        }
        if (province.isEmpty() && city.isEmpty()) {
            province = "其他";
        }
        return new String[]{province.isEmpty() ? "其他" : province, city};
    }

    /** 关键词词典 → 行业, 兜底"其他" */
    public static String detectIndustry(String text) {
        for (String[] entry : INDUSTRY_KEYWORDS) {
            for (int i = 1; i < entry.length; i++) {
                if (text.contains(entry[i])) {
                    return entry[0];
                }
            }
        }
        return "其他";
    }

    /** HTML → 纯文本(用于关键词匹配) */
    public static String toPlainText(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
    }

    private static String cityToProvince(String city) {
        for (String[] entry : CITY_PROVINCE) {
            if (entry[0].equals(city)) {
                return entry[1];
            }
        }
        return null;
    }
}
