#!/bin/bash
# GGZY 详情页数据加载方式分析
set -u
echo "=== detail shell content (grep script/ajax/url) ==="
grep -oE '(ajax|get[^(]*\(|url:[^,]*|\.php|\.json|load[^(]*\()[^)]{0,80}' /tmp/ggzy_detail.html | head -12
echo "--- static html to json guesses ---"
DETAIL="/information/deal/html/a/640000/0501/20260908/006459063831bfa949df94dfe3f5465b3503.html"
for suffix in ".json" "" ".data"; do
  code=$(curl -sL -m 12 -A "Mozilla/5.0" "https://www.ggzy.gov.cn/information/deal/html/a/640000/0501/20260908/006459063831bfa949df94dfe3f5465b3503${suffix}" -o /dev/null -w "%{http_code}")
  echo "suffix[$suffix] -> $code"
done
echo "--- 详情页 head 脚本片段 ---"
sed -n '1,40p' /tmp/ggzy_detail.html | grep -iE '(script|json|api|href)' | head -10
