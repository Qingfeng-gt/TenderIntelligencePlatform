#!/bin/bash
# 全国公共资源交易平台 ggzy.gov.cn 公告列表/详情结构
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

echo "=== 全国公共资源交易平台首页公告区 ==="
grep -oE 'href="[^"]*"' /tmp/s_$(echo -n "https://www.ggzy.gov.cn/" | md5sum | cut -c1-6).html | grep -iE '(ggzhangshi|zsxx|ggzy|announcement|info)' | sort -u | head -15

echo "=== 尝试已知公告列表页 ==="
for u in "https://www.ggzy.gov.cn/ggzufang/" "https://www.ggzy.gov.cn/zhaobiao/" "https://www.ggzy.gov.cn/page/zhaobiao" "https://www.ggzy.gov.cn/transPage/" ; do
  code=$(curl -sL -m 15 -A "$UA" "$u" -o /tmp/ggzy_t.html -w "%{http_code}:%{size_download}")
  echo "$u -> $code"
done
