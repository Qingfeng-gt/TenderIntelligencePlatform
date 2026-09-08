#!/bin/bash
# 找 firstLastUrl / showDetail 数据 URL 构造
set -u
echo "=== detail shell 中变量定义 ==="
grep -oE '(var |let |const )[a-zA-Z]+ *= *"[^"]{5,120}"' /tmp/ggzy_detail.html | head -10
echo "=== firstLastUrl 相关 ==="
grep -oE 'firstLastUrl[^;]{0,200}' /tmp/ggzy_detail.html | head -6
echo "=== showDetail 定义(js) ==="
grep -oE 'function showDetail[^}]{0,400}' /tmp/ggzy_js.js | head -2
curl -s -m 12 -A "Mozilla/5.0" "https://www.ggzy.gov.cn/assets/js/dealDetail.js" -o /tmp/ggzy_js2.js && grep -oE 'function showDetail[^}]{0,500}' /tmp/ggzy_js2.js | head -2
