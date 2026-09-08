#!/bin/bash
# CCGP 详情页精确结构 + 分页格式
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
JAR=/tmp/ccgp_cookies.txt
echo "=== 详情页标题标签 ==="
grep -oE '<(h1|h2|h3)[^>]*>[^<]{5,80}' /tmp/ccgp_det.html | head -4
echo "=== 正文节点行示例 ==="
grep -nE 'innercontent' /tmp/ccgp_det.html | head -4
echo "=== 基本信息行(含中文冒号) ==="
grep -oE '(项目编号|采购人|代理机构|预算金额|开标时间|联系人|联系电话)[：:][^<]{2,80}' /tmp/ccgp_det.html | head -12
echo "=== 列表分页链接 ==="
grep -oE 'href="[^"]*index[^"]*"' /tmp/ccgp_list.html | head -6
grep -oE '下一页|class="[^"]*page[^"]*"' /tmp/ccgp_list.html | head -6
echo "=== 更新时间(页面底部) ==="
grep -oE '2026-[01][0-9]-[0-9]{2} [0-9]{2}:[0-9]{2}' /tmp/ccgp_det.html | head -3
