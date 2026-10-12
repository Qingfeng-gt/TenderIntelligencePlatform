<template>
  <view class="page">
    <view class="placeholder">
      <text class="p-title">公告详情 —— 待甲实现</text>
      <text class="p-line">标题 / 正文（HTML）/ 附件 / 联系方式。</text>
      <text class="p-line">这是 M6 性能基准的**次测页面**（长文本渲染）。</text>
      <text class="p-line">接口：GET /admin-api/notice/get?id=（已放行免登录）</text>
      <text class="p-line">参数从上一页传入：onLoad(options) 里的 options.id</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return { id: null }
  },
  onLoad(options) {
    this.id = options && options.id ? options.id : null
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
