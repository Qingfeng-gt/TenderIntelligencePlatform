<template>
  <view class="page">
    <view class="hero">
      <text class="title">标讯通</text>
      <text class="sub">招投标情报平台 · 移动端</text>
    </view>

    <view class="hint">
      <text class="hint-title">工程骨架（毕设插槽 8-①）</text>
      <text class="hint-line">本页能跑起来，说明 UniApp 工程可用。</text>
      <text class="hint-line">甲直接改写本页与下列页面即可。</text>
    </view>

    <view class="entries">
      <view class="entry" @click="go('/pages/notice/index')">
        <text>公告列表</text>
        <text class="arrow">›</text>
      </view>
      <view class="entry" @click="go('/pages/my-bids/index')">
        <text>我的投标</text>
        <text class="arrow">›</text>
      </view>
      <view class="entry" @click="go('/pages/login/index')">
        <text>登录</text>
        <text class="arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  methods: {
    go(url) {
      uni.navigateTo({ url })
    }
  }
}
</script>

<style scoped>
.page {
  padding: 40rpx 30rpx;
}
.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 0 50rpx;
}
.title {
  font-size: 52rpx;
  font-weight: 700;
  color: #2b6de5;
}
.sub {
  margin-top: 14rpx;
  font-size: 26rpx;
  color: #8a9099;
}
.hint {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx;
  display: flex;
  flex-direction: column;
}
.hint-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1f2329;
}
.hint-line {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #8a9099;
  line-height: 1.6;
}
.entries {
  margin-top: 30rpx;
  background: #ffffff;
  border-radius: 16rpx;
  overflow: hidden;
}
.entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 30rpx;
  font-size: 30rpx;
  color: #1f2329;
  border-bottom: 1rpx solid #e5e7eb;
}
.entry:last-child {
  border-bottom: none;
}
.arrow {
  color: #c0c4cc;
  font-size: 34rpx;
}
</style>
