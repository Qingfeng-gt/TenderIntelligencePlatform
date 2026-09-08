#!/bin/bash
# 中国政府采购网 静态列表页+详情页 可达性验证(经首页 → 列表 → 详情)
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
JAR=/tmp/ccgp_cookies.txt

echo "=== 首站首页(拿 cookie) ==="
curl -s -m 15 -A "$UA" -c "$JAR" "http://www.ccgp.gov.cn/" -o /tmp/ccgp_home.html -w "HTTP:%{http_code} bytes:%{size_download}\n"

echo "=== 地方-招标公告列表页 ==="
curl -s -m 20 -A "$UA" -b "$JAR" -H "Referer: http://www.ccgp.gov.cn/" "http://www.ccgp.gov.cn/cggg/dfgg/gkzb/" -o /tmp/ccgp_list.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
echo "--- 列表项链接/标题结构 ---"
grep -oE 'href="[^"]*t[0-9]{13}[^"]*"' /tmp/ccgp_list.html | head -4
grep -oE '<a[^>]*target="_blank"[^>]*>[^<]{6,50}</a>' /tmp/ccgp_list.html | head -5
grep -oE '<li>[^<]*</li>' /tmp/ccgp_list.html | head -5
echo "--- class 概览 ---"
grep -oE 'class="[^"]*"' /tmp/ccgp_list.html | sort | uniq -c | sort -rn | head -10
