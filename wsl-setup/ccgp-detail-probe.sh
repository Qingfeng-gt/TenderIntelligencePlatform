#!/bin/bash
# 中国政府采购网 详情页字段 + 各频道确认
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
JAR=/tmp/ccgp_cookies.txt

DET="http://www.ccgp.gov.cn/cggg/dfgg/gkzb/202609/t20260908_27283358.htm"
echo "=== 详情页 ==="
curl -s -m 20 -A "$UA" -b "$JAR" -H "Referer: http://www.ccgp.gov.cn/cggg/dfgg/gkzb/" "$DET" -o /tmp/ccgp_det.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
echo "--- 标题 ---"
grep -oE '<h1[^>]*>.{1,80}' /tmp/ccgp_det.html | head -2
echo "--- 基本信息区(发布时间/采购人/联系人等) ---"
grep -oE '(发布时间|采购人名称|代理机构名称|联系人|联系电话|项目编号|预算金额|投标截止|开标时间)[^<]{0,60}' /tmp/ccgp_det.html | head -15
echo "--- 正文容器/日期 ---"
grep -oE 'class="[^"]*(essay|content|vF_detail_content|summary)[^"]*"' /tmp/ccgp_det.html | sort -u | head -6

echo "=== 频道确认: 中标公告 / 更正公告 ==="
for ch in "zbgg" "jgcg" "gzgg" "jzxtp" "xjgg" "dsjz" ; do
  code=$(curl -s -m 12 -A "$UA" -b "$JAR" "http://www.ccgp.gov.cn/cggg/dfgg/$ch/" -o /tmp/ccgp_ch.html -w "%{http_code}:%{size_download}")
  ttl=$(grep -oE '<title>[^<]*' /tmp/ccgp_ch.html | head -1)
  echo "$ch -> $code  $ttl"
done
