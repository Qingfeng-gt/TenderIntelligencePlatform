<script setup lang="ts">
// ─────────────────────────────────────────────────────────────────────────
// 占位页，由毕设插槽 3 预置（见 doc/毕设 §7）
//
// 归属：甲（M1 站点发现）
// 菜单：id 12810，路由 /discovery/agent，component discovery/agent/index
//
// admin 端是**菜单驱动**的，替换本文件内容即可 —— 不需要改路由，
// 也不要动 backend/sql/mysql/notice.sql 里的 system_menu 记录。
//
// 本页要做成什么，见《招投标情报平台-页面设计.md》§2「M1 智能站点发现台」：
//   · 顶部：[种子关键词输入框] [地区过滤] [数量上限] [开始发现]
//   · Agent 运行进度：Steps 时间线（搜索引擎检索 → 抓首页 → LLM 判站 → 候选列表）
//   · 候选站点表格：站点名 / 域名 / 地区 / 健康分 / 推荐适配器
//   · 底部：[批量入库] [拒绝选中] [查看首页快照]
//
// 依赖接口：/admin-api/discovery/{run,progress,candidates,approve,reject}
// 注意：M1 已按 doc/毕设 §1 降级 —— 健康分简化为「可访问性 + 是否招投标站」两档，
//       不要做四维评分模型。
// ─────────────────────────────────────────────────────────────────────────
</script>

<template>
  <div class="placeholder-page">
    <el-card shadow="never">
      <el-empty description="M1 智能站点发现台 —— 占位页，待甲实现" />
    </el-card>
  </div>
</template>

<style scoped>
.placeholder-page {
  padding: 8px 0;
}
</style>
