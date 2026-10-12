<script setup lang="ts">
// ─────────────────────────────────────────────────────────────────────────
// 占位页，由毕设插槽 4 预置（见 doc/毕设 §7）
//
// 归属：丁（M4 大模型语义模糊查找）
// 路由已在 router/index.ts 注册（/ai-search），导航项已在 PortalLayout.vue 加好。
// **替换本文件内容即可，不需要改路由，也不需要改 PortalLayout。**
//
// 本页要做成什么，见《招投标情报平台-页面设计.md》§5「用户端 AI 搜索页」：
//   · 大搜索框，占位符「我想找贵阳 50 万以下的弱电智能化项目」
//   · LLM 查询解析结果（可折叠）：地区 / 预算上限 / 语义关键词 / 召回方式
//   · 结果卡片（按相关度排序）：相关度色条 + 命中方式标签（向量 / BM25 / 结构化过滤）
//
// 依赖接口：POST /app-api/ai-search（已由插槽 5 放行免登录）
// 组件另建：components/{AiSearchBox,IntentPanel,SearchResultCard}.vue
// ─────────────────────────────────────────────────────────────────────────
</script>

<template>
  <div class="container page-placeholder">
    <el-card shadow="never">
      <el-empty description="M4 AI 自然语言搜索 —— 占位页，待丁实现" />
    </el-card>
  </div>
</template>

<style scoped>
.page-placeholder {
  padding: 24px 0;
}
</style>
