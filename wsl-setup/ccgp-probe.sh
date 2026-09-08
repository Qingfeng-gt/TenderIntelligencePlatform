#!/bin/bash
# 中国政府采购网 bxsearch 探测:查看返回内容与结果链接结构
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

echo "=== keyword=计算机 (2026-09-01~09-08) ==="
curl -s -m 20 -A "$UA" "http://search.ccgp.gov.cn/bxsearch?searchtype=1&page_index=1&bidSort=0&pinMu=0&bidType=0&dbselect=bidx&kw=%E8%AE%A1%E7%AE%97%E6%9C%BA&start_time=2026%3A09%3A01&end_time=2026%3A09%3A08&timeType=6" -o /tmp/ccgp_kw.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
echo "--- detail htm links ---"
grep -oE '[a-z]+/[a-z0-9]+/t[0-9]+_[0-9]+\.htm' /tmp/ccgp_kw.html | head -5
echo "--- list item title hints ---"
grep -oE 'class="[^"]*t[^"]*"' /tmp/ccgp_kw.html | sort -u | head -8
echo "--- first 600 chars ---"
head -c 600 /tmp/ccgp_kw.html
echo
echo "=== no-kw result (what did we get before?) ==="
head -c 600 /tmp/ccgp_search.html
