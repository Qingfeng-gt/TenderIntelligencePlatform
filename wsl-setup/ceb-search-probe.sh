#!/bin/bash
# 探测 CEB 公告搜索接口(getSearch.do / bulletin.cebpubservice.com)
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
BASE="https://www.cebpubservice.com"

echo "=== 1) getSearch.do (GET with common params) ==="
curl -s -m 20 -A "$UA" "$BASE/ctpsp_iiss/searchbusinesstypebeforedooraction/getSearch.do" -o /tmp/ceb_search.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
grep -oE 'class="[^"]*"[^>]*>' /tmp/ceb_search.html | grep -iE '(search|result|bullet)' | sort -u | head -6
grep -oE '(list|result|records)[^"]*' /tmp/ceb_search.html | sort -u | head -6

echo "=== 2) POST getSearch.do (类似前端表单) ==="
curl -s -m 20 -A "$UA" -X POST "$BASE/ctpsp_iiss/searchbusinesstypebeforedooraction/getSearch.do" \
  -d "recordKeywords=&recordSite=&businessType=招投标&startTime=&endTime=" \
  -o /tmp/ceb_search_post.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
head -c 400 /tmp/ceb_search_post.html
echo

echo "=== 3) bulletin.cebpubservice.com 公告中心 ==="
curl -s -m 20 -A "$UA" "https://bulletin.cebpubservice.com/" -o /tmp/ceb_bulletin.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
grep -oE '(candidatestr|search[^" ]*|query[^" ]*|\.do[^"]*|list[^" ]*)' /tmp/ceb_bulletin.html | sort -u | head -15
