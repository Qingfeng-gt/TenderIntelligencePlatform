#!/bin/bash
# 环境自检:数据库 / 爬虫源站可达性(在 WSL 中运行)
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

echo "=== MySQL databases ==="
mysql -uroot -p123456 -N -e "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA" 2>/dev/null

DB="ruoyi-vue-pro-jdk8"
echo "=== tables in $DB (count) ==="
mysql -uroot -p123456 -N "$DB" -e "SHOW TABLES" 2>/dev/null | wc -l
echo "=== notice table count ==="
mysql -uroot -p123456 -N "$DB" -e "SELECT COUNT(*) FROM notice" 2>/dev/null

echo "=== CCGP search page (search.ccgp.gov.cn) ==="
curl -s -m 15 -A "$UA" "http://search.ccgp.gov.cn/bxsearch?searchtype=1&page_index=1&bidSort=0&pinMu=0&bidType=0&dbselect=bidx&kw=&start_time=2026%3A09%3A07&end_time=2026%3A09%3A08&timeType=6" -o /tmp/ccgp_search.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
echo "--- result list marker & first detail links ---"
grep -oE 'www\.ccgp\.gov\.cn/cggg/[a-z]+/[a-z]+/[a-z0-9]+/t[0-9]+_[0-9]+\.htm' /tmp/ccgp_search.html | head -3
grep -oE '<span[^>]*vT-srch-result-list-bid[^>]*>' /tmp/ccgp_search.html | head -1

echo "=== CCGP detail page (www.ccgp.gov.cn) ==="
LINK=$(grep -oE 'www\.ccgp\.gov\.cn/cggg/[a-z]+/[a-z]+/[a-z0-9]+/t[0-9]+_[0-9]+\.htm' /tmp/ccgp_search.html | head -1)
if [ -n "$LINK" ]; then
  curl -s -m 15 -A "$UA" "http://$LINK" -o /tmp/ccgp_detail.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
  echo "--- detail markers: 项目名称/预算金额/公告正文 ---"
  grep -oE '<span[^>]*class="[^"]*(ptc|tc)[^"]*"[^>]*>' /tmp/ccgp_detail.html | head -3
  grep -cE 'vT_detail_content' /tmp/ccgp_detail.html
else
  echo "NO detail link found in search page"
fi

echo "=== cebpubservice ==="
curl -s -o /dev/null -m 10 -w "HTTP:%{http_code}\n" -A "$UA" "https://www.cebpubservice.com/"
