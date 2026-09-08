#!/bin/bash
# 批量探测候选爬取源站(无验证码/可公开访问的为佳)
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
hit() { echo "HTTP:$2 bytes:$3  $1"; }

check() {
  local name="$1"; local url="$2"
  local out tmp
  tmp="/tmp/probe_$(echo "$name" | md5sum | cut -c1-8).html"
  out=$(curl -s -m 12 -A "$UA" "$url" -o "$tmp" -w "%{http_code}:%{size_download}")
  echo "$name -> $out"
}

check "全国公共资源交易平台" "http://www.ggzy.gov.cn/"
check "四川省公共资源交易" "http://ggzyjy.sc.gov.cn/"
check "河南省公共资源交易" "http://www.hnggzy.com/"
check "贵州公共资源交易" "http://ggzy.guizhou.gov.cn/"
check "江西公共资源交易网" "http://ggzyjx.jiangxi.gov.cn/"
check "千里马招标网-招标公告" "https://www.qianlima.com/zbgg/"
check "中国采购与招标网" "https://www.chinabidding.cn/"
check "建设招标网(建库)" "https://www.jianshe99.com/zbgg/"
check "剑鱼标讯" "https://www.jianyu360.cn/"
