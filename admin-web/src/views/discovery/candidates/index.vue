<script setup lang="ts">
// ─────────────────────────────────────────────────────────────────────────
// 占位页，由毕设插槽 3 预置（见 doc/毕设 §7）
//
// 归属：甲（M1 站点发现）
// 菜单：id 12811，路由 /discovery/candidates，component discovery/candidates/index
//
// 替换本文件内容即可，不需要改路由。
//
// 本页要做成什么，见《招投标情报平台-页面设计.md》§2：
//   · 候选站点审核列表（待审 / 已通过 / 已拒绝三种状态）
//   · 勾选通过 → 写入 crawler_site（甲的模块自己定义 DiscoveredSiteDO，
//     **不依赖 tender-module-crawler 的任何 Java 类**）
//   · 可查看首页快照，辅助人工判断
//
// 依赖接口：/admin-api/discovery/{candidates,approve,reject}
// ⚠️ 写 crawler_site 前先确认乙已同意加列（doc/毕设 §4.1 加列流程）
// ─────────────────────────────────────────────────────────────────────────
</script>

<template>
  <div class="placeholder-page">
    <el-card shadow="never">
      <el-empty description="M1 候选站点审核 —— 占位页，待甲实现" />
    </el-card>
  </div>
</template>

<style scoped>
.placeholder-page {
  padding: 8px 0;
}
</style>
