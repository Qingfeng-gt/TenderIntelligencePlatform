#!/bin/bash
# 更正公告详情页正文容器分析
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
JAR=/tmp/ccgp_cookies.txt
curl -s -m 15 -A "$UA" -b "$JAR" "http://www.ccgp.gov.cn/cggg/dfgg/gzgg/202609/t20260907_27283129.htm" -o /tmp/ccgp_chg.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
echo "--- 容器类 ---"
grep -oE 'class="[^"]*"' /tmp/ccgp_chg.html | sort | uniq -c | sort -rn | head -12
echo "--- noticeArea / innercontent 出现次数 ---"
grep -c 'noticeArea' /tmp/ccgp_chg.html; grep -c 'innercontent' /tmp/ccgp_chg.html
echo "--- 标题/正文样例 ---"
grep -oE '<h2[^>]*>[^<]{0,60}' /tmp/ccgp_chg.html | head -2
grep -oE '正文[^<]{0,30}|更正内容[^<]{0,40}' /tmp/ccgp_chg.html | head -4
