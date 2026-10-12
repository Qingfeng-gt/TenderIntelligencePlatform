<template>
  <view class="page">
    <view class="placeholder">
      <text class="p-title">公告列表 —— 待甲实现</text>
      <text class="p-line">列表 + 筛选。这是 M6 性能基准的**主测页面**（长列表重）。</text>
      <text class="p-line">接口：GET /admin-api/notice/list（已放行免登录，后端零改造）</text>
      <text class="p-line">参数：pageNo / pageSize / keyword / type / province / industry / publishTimeGreaterThan</text>
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
