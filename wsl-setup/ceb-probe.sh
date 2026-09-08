#!/bin/bash
# 中国招标投标公共服务平台(cebpubservice)探测:找公开的搜索/公告数据接口
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

echo "=== homepage ==="
curl -s -m 15 -A "$UA" "https://www.cebpubservice.com/" -o /tmp/ceb_home.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
echo "--- search-related urls in homepage ---"
grep -oE 'https?://[a-zA-Z0-9./_-]+\.(do|action|jsp|html)' /tmp/ceb_home.html | sort -u | head -10
grep -oE '(getSearchInfo|searchbulletin|search[^"]*)' /tmp/ceb_home.html | sort -u | head -8
echo "--- iframe/script urls ---"
grep -oE '(src|href)="[^"]*"' /tmp/ceb_home.html | grep -iE '(search|bulletin|gonggao|notice)' | head -10
