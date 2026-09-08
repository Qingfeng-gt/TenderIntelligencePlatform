#!/bin/bash
# 验证 b/ 内容页 + 找交易公告列表页
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
BURL="https://www.ggzy.gov.cn/information/deal/html/b/640000/0501/20260908/006459063831bfa949df94dfe3f5465b3503.html"
curl -sL -m 20 -A "$UA" "$BURL" -o /tmp/ggzy_b.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
echo "--- b 页面关键内容 ---"
grep -oE '(项目名称|标段名称|发布时间|招标人|采购人|交易方式|交易编号|公告标题)[^<>]{0,60}' /tmp/ggzy_b.html | head -12
echo "--- 页面 title ---"
grep -oE '<title>[^<]*</title>' /tmp/ggzy_b.html | head -2

echo "=== 首页"交易公告"区块链接 ==="
CP="/tmp/ggzy_home.html"
grep -oE 'href="[^"]*"' "$CP" | grep -oE '/information/[a-z]+/html/[^"]*' | head -8
grep -oE 'href="[^"]*(class|[0-9]{6}|a/[0-9]{6})[^"]*"' "$CP" | grep -oE '/information/[a-z]+/html/[^"]*' | sort -u | head -8
