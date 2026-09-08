#!/bin/bash
# CCGP 列表分页 href 形式(取第一页的 pager 区域)
set -u
grep -oE '<a[^>]*class="pager"[^>]*>[^<]*</a>' /tmp/ccgp_list.html | head -8
echo "--- context around 下一页 ---"
grep -B6 "下一页" /tmp/ccgp_list.html | grep -oE 'href="[^"]*"' | head -10
