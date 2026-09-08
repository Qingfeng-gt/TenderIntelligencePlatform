#!/bin/bash
# 网络连通性测试（用于判断是否需配置镜像/代理）
test_url() {
  local url="$1"
  local code
  code=$(curl -s -o /dev/null -m 8 -w '%{http_code}' "$url" 2>/dev/null)
  if [ -z "$code" ] || [ "$code" = "000" ]; then
    echo "FAIL  $url"
  else
    echo "OK:$code  $url"
  fi
}
test_url http://archive.ubuntu.com/ubuntu/
test_url https://get.sdkman.io
test_url https://nodejs.org
test_url https://registry.npmjs.org
test_url https://repo.maven.apache.org
test_url https://github.com
test_url https://download.docker.com
test_url https://mirrors.aliyun.com/docker-ce/
test_url https://mirrors.aliyun.com/maven/
test_url https://registry.npmmirror.com
