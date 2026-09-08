#!/bin/bash
# 千里马招标网列表页结构分析
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

echo "=== 千里马 /zbgg/ 页面结构 ==="
curl -s -m 20 -A "$UA" "https://www.qianlima.com/zbgg/" -o /tmp/qlm_zbgg.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
echo "--- 列表项链接(含详情 url 形态) ---"
grep -oE 'href="[^"]*"' /tmp/qlm_zbgg.html | grep -iE '(zbgg|content|detail|zbcontent|company)' | head -12
echo "--- 标题/时间/金额类 class ---"
grep -oE 'class="[^"]*"' /tmp/qlm_zbgg.html | sort | uniq -c | sort -rn | head -30
