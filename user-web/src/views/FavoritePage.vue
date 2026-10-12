<script setup lang="ts">
// ─────────────────────────────────────────────────────────────────────────
// 占位页，由毕设插槽 4 预置（见 doc/毕设 §7）
//
// 归属：丙（M3 前台展示与数据库存储）
// 路由已在 router/index.ts 注册（/favorite），导航项已在 PortalLayout.vue 加好。
// **替换本文件内容即可，不需要改路由，也不需要改 PortalLayout。**
//
// 本页要做成什么，见《招投标情报平台-页面设计.md》§4「我的收藏」：
//   · 用户收藏的公告列表
//   · 订阅的关键词 / 地区
//
// ⚠️ 前置：收藏功能**目前不存在**（doc/毕设 §0 第 4 条）。要做就得补：
//   · 表 notice_favorite（丙的 notice.sql）
//   · 接口 /notice/favorite/toggle
//   若工期不够，这一项是最先该砍的（doc/毕设 §12 风险 ⑥）——
//   届时把本页和路由、导航一并摘掉即可。
// ─────────────────────────────────────────────────────────────────────────
</script>

<template>
  <div class="container page-placeholder">
    <el-card shadow="never">
      <el-empty description="M3 我的收藏 —— 占位页，待丙实现" />
    </el-card>
  </div>
</template>

<style scoped>
.page-placeholder {
  padding: 24px 0;
}
</style>
