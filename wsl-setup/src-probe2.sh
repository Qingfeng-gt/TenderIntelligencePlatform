#!/bin/bash
# 二次探测:找"列表+详情均可访问"的源站
set -u
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
check() {
  local name="$1" url="$2" out
  out=$(curl -sL -m 15 -A "$UA" "$url" -o "/tmp/s_$(echo -n "$url" | md5sum | cut -c1-6).html" -w "%{http_code}:%{size_download}")
  echo "$name -> $out"
}
check "中国采购与招标网-招标公告" "https://www.chinabidding.cn/zbgg/"
check "全国公共资源交易(https跟踪)" "https://www.ggzy.gov.cn/"
check "河北省政府采购网" "http://www.ccgp-hebei.gov.cn/"
check "山东省政府采购网" "http://www.ccgp-shandong.gov.cn/"
check "中国政府采购网-中央" "http://www.ccgp.gov.cn/"
check "剑鱼标讯-标讯列表" "https://www.jianyu360.cn/jylab/supsearch/index.html"
