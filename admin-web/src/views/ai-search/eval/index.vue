<script setup lang="ts">
// ─────────────────────────────────────────────────────────────────────────
// 占位页，由毕设插槽 3 预置（见 doc/毕设 §7）
//
// 归属：丁（M4 大模型语义模糊查找）
// 菜单：id 12830，路由 /ai-search/eval，component ai-search/eval/index
//
// 替换本文件内容即可，不需要改路由。
//
// 本页要做成什么，见《招投标情报平台-页面设计.md》§5「管理员评测面板」：
//   · 顶部：评测集规模（200 条标注查询）
//   · 对比表：方法 | Recall@10 | MRR | 平均耗时
//       MySQL LIKE / 纯向量检索 / BM25 / 混合（本文方法）/ 混合 + Rerank
//   · 散点图：每条查询在不同方法下的命中位置
//
// 依赖接口：GET /admin-api/ai-search/eval/report、POST /admin-api/ai-search/eval/run
// 图表用 echarts（admin-web 已有，Home 页在用）
// ─────────────────────────────────────────────────────────────────────────
</script>

<template>
  <div class="placeholder-page">
    <el-card shadow="never">
      <el-empty description="M4 检索效果评测面板 —— 占位页，待丁实现" />
    </el-card>
  </div>
</template>

<style scoped>
.placeholder-page {
  padding: 8px 0;
}
</style>
