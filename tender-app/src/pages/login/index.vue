<template>
  <view class="page">
    <view class="placeholder">
      <text class="p-title">登录 —— 待甲实现</text>
      <text class="p-line">接口：POST /app-api/member/auth/login（@PermitAll）</text>
      <text class="p-line">
        ⚠️ 滑块验证码在 uni-app 里用不了，队长已在插槽阶段关掉开关
        （tender.captcha.enable=false，读自 MemberAuthServiceImpl）。甲不要改 yaml。
      </text>
      <text class="p-line">手机号验证码登录留到二期（后端短信通道不存在）。</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {}
  }
}
</script>

<style scoped>
.page {
  padding: 30rpx;
}
.placeholder {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 40rpx 30rpx;
  display: flex;
  flex-direction: column;
}
.p-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2329;
}
.p-line {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #8a9099;
  line-height: 1.7;
}
</style>
