<template>
  <view class="page">
    <view class="placeholder">
      <text class="p-title">我的投标 —— 待甲实现</text>
      <text class="p-line">当前用户的投标项目列表 + 状态。</text>
      <text class="p-line">⚠️ 本页**需要登录**（与公告两页不同，没有 @PermitAll）。</text>
      <text class="p-line">接口：GET /app-api/bid-project/list（要求 MEMBER token）</text>
      <text class="p-line">APP 无 token 时会被 401，需先跳登录页。</text>
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
