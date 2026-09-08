#!/bin/bash
# 419 原因分析 + 带 cookie/referer 重试详情页
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

echo "=== 419 page content ==="
head -c 800 /tmp/qlm_detail.html
echo
echo "=== 带 cookie+referer 重试 ==="
sleep 3
curl -s -m 20 -A "$UA" -H "Referer: https://www.qianlima.com/zbgg/" \
  -H "Accept-Language: zh-CN,zh;q=0.9" \
  -c /tmp/qlm_cookies.txt -b /tmp/qlm_cookies.txt \
  "https://www.qianlima.com/bid-628874154.html" -o /tmp/qlm_detail2.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
grep -oE '(采购单位|代理机构|公告类型|发布时间|预算金额|截止时间|项目编号)[^<>]{0,50}' /tmp/qlm_detail2.html | head -20
echo "--- info container classes ---"
grep -oE 'class="[^"]*(info|content|detail|text)[^"]*"' /tmp/qlm_detail2.html | sort | uniq -c | sort -rn | head -15
