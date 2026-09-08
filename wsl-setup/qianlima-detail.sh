#!/bin/bash
# 千里马详情页字段结构分析
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
curl -s -m 20 -A "$UA" "https://www.qianlima.com/bid-628874154.html" -o /tmp/qlm_detail.html -w "HTTP:%{http_code} bytes:%{size_download}\n"
echo "--- 常见字段 class/标签 ---"
grep -oE '(采购单位|代理机构|地区|行业|公告类型|发布时间|预算金额|投标截止|开标时间|资金来源|项目编号)[^<>]{0,40}' /tmp/qlm_detail.html | head -20
echo "--- 正文容器 ---"
grep -oE 'class="[^"]*"' /tmp/qlm_detail.html | sort | uniq -c | sort -rn | head -20
