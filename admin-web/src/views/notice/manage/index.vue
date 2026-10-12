<script setup lang="ts">
// ─────────────────────────────────────────────────────────────────────────
// 占位页，由毕设插槽 3 预置（见 doc/毕设 §7）
//
// 归属：丙（M3 前台展示与数据库存储）
// 菜单：id 12850，路由 /notice/manage，component notice/manage/index
//
// 替换本文件内容即可，不需要改路由。
//
// 本页要做成什么（《招投标情报平台-页面设计.md》未给布局，由丙自行设计）：
//   · 公告列表 + 筛选（地区 / 行业 / 来源 / 发布时间）
//   · 公告详情查看、附件查看
//   · 运营视角：采集来源与入库时间、是否有附件、字段完整度
//
// 依赖接口：/notice/{list,get,stats}（已存在）；新增的管理端接口由丙自定
// ⚠️ 门户接口 /notice/* 目前挂在 /admin-api/notice/** 下靠放行免登录（历史怪癖），
//    本页是**管理端**用途，需要登录 + 权限，别复用那个放行路径
// ─────────────────────────────────────────────────────────────────────────
</script>

<template>
  <div class="placeholder-page">
    <el-card shadow="never">
      <el-empty description="M3 公告管理 —— 占位页，待丙实现" />
    </el-card>
  </div>
</template>

<style scoped>
.placeholder-page {
  padding: 8px 0;
}
</style>
