#!/bin/bash
# 抓 dealDetailList.js 看详情数据接口
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
curl -s -m 15 -A "$UA" "https://www.ggzy.gov.cn/assets/js/dealDetailList.js" -o /tmp/ggzy_js.js -w "HTTP:%{http_code} bytes:%{size_download}\n"
head -c 3000 /tmp/ggzy_js.js
