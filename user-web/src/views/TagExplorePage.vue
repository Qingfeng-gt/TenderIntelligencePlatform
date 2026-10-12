<script setup lang="ts">
// ─────────────────────────────────────────────────────────────────────────
// 占位页，由毕设插槽 4 预置（见 doc/毕设 §7）
//
// 归属：戊（M5 标签拓扑查询系统）
// 路由已在 router/index.ts 注册（/tag/explore），导航项已在 PortalLayout.vue 加好。
// **替换本文件内容即可，不需要改路由，也不需要改 PortalLayout。**
//
// 本页要做成什么，见《招投标情报平台-页面设计.md》§6「用户端标签探索页」：
//   · 左侧：标签云（按权重大小映射字号）
//   · 右侧：标签拓扑图（echarts-graph 力导向图，节点大小=公告数、连线粗细=共现次数）
//   · 底部：点标签节点后联动列出相关公告（走丙的 /notice/list?tagIds=）
//
// 依赖接口：/app-api/tag/{cloud,topology,recommend}（已由插槽 5 放行免登录）
// 组件另建：components/{TagCloud,TagTopologyGraph}.vue
// 注意：本页要嵌进 NoticeListPage 的标签 chip 挂载点，需与丙对接（挂载点由丙预留）
// ─────────────────────────────────────────────────────────────────────────
</script>

<template>
  <div class="container page-placeholder">
    <el-card shadow="never">
      <el-empty description="M5 标签拓扑浏览 —— 占位页，待戊实现" />
    </el-card>
  </div>
</template>

<style scoped>
.page-placeholder {
  padding: 24px 0;
}
</style>
