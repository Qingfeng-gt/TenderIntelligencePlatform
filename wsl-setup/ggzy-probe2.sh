#!/bin/bash
# GGZY 详情页 + 列表接口
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
TMP="/tmp/s_$(echo -n 'https://www.ggzy.gov.cn/' | md5sum | cut -c1-6).html"
cp "$TMP" /tmp/ggzy_home.html 2>/dev/null

echo "=== 首页 script src (找 API 前缀) ==="
grep -oE 'src="[^"]*"' /tmp/ggzy_home.html | grep -iE '(api|query|list|json)' | head -8
echo "=== 首页 block 标题 ==="
grep -oE '(交易公告|公示公告|招标公告|<h[0-9][^>]*>[^<]{2,20})' /tmp/ggzy_home.html | head -10

echo "=== 详情页可达性 (取一条) ==="
DETAIL=$(grep -oE '/information/deal/html/[^"]*\.html' /tmp/ggzy_home.html | head -1)
echo "detail=$DETAIL"
curl -sL -m 20 -A "$UA" "https://www.ggzy.gov.cn$DETAIL" -o /tmp/ggzy_detail.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
echo "--- 详情页关键字段 ---"
grep -oE '(项目名称|标段名称|发布时间|招标人|公告标题|交易编号|交易方式)[^<>]{0,45}' /tmp/ggzy_detail.html | head -12
echo "--- 详情页正文 class ---"
grep -oE 'class="[^"]*"' /tmp/ggzy_detail.html | sort | uniq -c | sort -rn | head -12
