#!/bin/bash
# 千里马列表项细节 + 详情页字段结构
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

echo "=== list-single item 的 href ==="
grep -oE '<a[^>]*href="[^"]*"[^>]*>' /tmp/qlm_zbgg.html | grep -iE '(content|zbgg)' | head -8
echo "--- title link sample ---"
grep -B2 -A2 'class="a-link"' /tmp/qlm_zbgg.html | head -30
