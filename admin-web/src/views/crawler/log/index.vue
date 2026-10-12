<script setup lang="ts">
// ─────────────────────────────────────────────────────────────────────────
// 占位页，由毕设插槽 3 预置（见 doc/毕设 §7）
//
// 归属：乙（M2 招投标网站爬虫）
// 菜单：id 12820，路由 /crawler/log，component crawler/log/index
//
// 替换本文件内容即可，不需要改路由。
// 注意：已存在的 /crawler（id 12800，component crawler/index）是**另一个页面**，别动它。
//
// 本页要做成什么，见《招投标情报平台-页面设计.md》§3「抓取日志与字段核对」：
//   · 左侧：本次任务抓到的公告列表
//   · 右侧：选中一条的字段抽取核对表
//       字段 | 抽取值 | 准确率判定（✓ / ✗）
//       标题 / 采购人 / 预算金额 / 报名截止 / 联系方式
//   · 底部：[在浏览器打开原文链接] [标注为抽取错误]
//   · 要能展示「LLM 兜底抽取成功」的条目（extract_mode = 规则/LLM/融合）
//
// 依赖接口：/admin-api/crawler/log/{page,sample,mark}
// 数据来自 crawler_page_snapshot（乙新建的表）
// ─────────────────────────────────────────────────────────────────────────
</script>

<template>
  <div class="placeholder-page">
    <el-card shadow="never">
      <el-empty description="M2 抓取日志与字段核对 —— 占位页，待乙实现" />
    </el-card>
  </div>
</template>

<style scoped>
.placeholder-page {
  padding: 8px 0;
}
</style>
