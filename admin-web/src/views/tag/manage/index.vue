<script setup lang="ts">
// ─────────────────────────────────────────────────────────────────────────
// 占位页，由毕设插槽 3 预置（见 doc/毕设 §7）
//
// 归属：戊（M5 标签拓扑查询系统）
// 菜单：id 12840，路由 /tag/manage，component tag/manage/index
//
// 替换本文件内容即可，不需要改路由。
//
// 本页要做成什么，见《招投标情报平台-页面设计.md》§6「管理员标签管理页」：
//   · 顶部：[新增标签] [批量导入行业标签] [触发全量自动打标]
//   · 左侧：标签树（行业 / 地区 / 金额 / 采购方式 四类）
//   · 右侧：标签详情 —— 名称 / 层级 / 父标签 / 关联公告数 /
//             打标方式（RULE 还是 LLM）/ 置信度 / 共现 Top5
//
// 依赖接口：/admin-api/tag/{tree,auto-tag/all,auto-tag/one}
// 数据表：tag、notice_tag_rel（含 source 与 confidence）、tag_cooccurrence
// ⚠️ 戊的 tag 表是**唯一权威标签源**；notice.industry 是 crawler 的规则抽取结果，
//    只作为 RuleTaggingService 的输入之一（doc/毕设 §6 第 8 项）
// ─────────────────────────────────────────────────────────────────────────
</script>

<template>
  <div class="placeholder-page">
    <el-card shadow="never">
      <el-empty description="M5 标签管理与自动打标 —— 占位页，待戊实现" />
    </el-card>
  </div>
</template>

<style scoped>
.placeholder-page {
  padding: 8px 0;
}
</style>
