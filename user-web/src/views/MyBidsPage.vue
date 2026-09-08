<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { updateBidStatus, listBidProjects } from '@/api/bid'
import { formatDateTime } from '@/utils/format'
import type { BidProject, BidStatus } from '@/types'

const list = ref<BidProject[]>([])
const loading = ref(false)
const filter = ref<BidStatus | ''>('')

const statusLabel: Record<BidStatus, string> = {
  SUBMITTED: '已递交',
  OTB: '待开标',
  WON: '中标',
  LOST: '未中标',
  ABANDONED: '放弃'
}
const statusType: Record<BidStatus, 'primary' | 'warning' | 'success' | 'info' | 'danger'> = {
  SUBMITTED: 'primary',
  OTB: 'warning',
  WON: 'success',
  LOST: 'danger',
  ABANDONED: 'info'
}

/** 各状态可用操作 */
const actions: Record<BidStatus, { label: string; to: BidStatus; type: 'primary' | 'success' | 'danger' | 'info' }[]> = {
  SUBMITTED: [
    { label: '标记开标', to: 'OTB', type: 'primary' },
    { label: '放弃项目', to: 'ABANDONED', type: 'danger' }
  ],
  OTB: [
    { label: '确认中标', to: 'WON', type: 'success' },
    { label: '确认未中标', to: 'LOST', type: 'danger' },
    { label: '放弃项目', to: 'ABANDONED', type: 'info' }
  ],
  WON: [],
  LOST: [],
  ABANDONED: []
}

function fmt(t?: number | string) {
  return formatDateTime(t)
}

function fmtAmount(amount?: number) {
  return amount != null ? amount + ' 万元' : '待填写'
}

async function load() {
  loading.value = true
  list.value = await listBidProjects(filter.value)
  loading.value = false
}

async function doAction(item: BidProject, to: BidStatus) {
  await updateBidStatus(item.id, to)
  await load()
}

onMounted(load)
</script>

<template>
  <div class="container page-wrap">
    <div class="head">
      <h2 class="page-title">我的投标</h2>
      <span class="sub">投标项目管理 · 状态流转: 已递交 → 待开标 → 中标 / 未中标</span>
    </div>

    <div class="toolbar">
      <el-radio-group v-model="filter" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button v-for="(label, key) in statusLabel" :key="key" :value="key">{{ label }}</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="list" v-loading="loading" class="bid-table" :empty-text="'暂无投标项目,去标讯大厅发起投标'">
      <el-table-column label="项目名称" min-width="320" show-overflow-tooltip>
        <template #default="{ row }">
          <router-link :to="`/notice/${row.noticeId}`" class="proj-name">{{ row.projectName }}</router-link>
        </template>
      </el-table-column>
      <el-table-column prop="bidAmount" label="拟投标金额" width="130">
        <template #default="{ row }">{{ fmtAmount(row.bidAmount) }}</template>
      </el-table-column>
      <el-table-column label="投标截止" width="160">
        <template #default="{ row }">{{ fmt(row.deadline) }}</template>
      </el-table-column>
      <el-table-column label="投标文件" width="200">
        <template #default="{ row }">
          <span :class="{ 'no-file': !row.bidFileName }">{{ row.bidFileName || '待上传' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType[row.status as BidStatus]" effect="light">{{ statusLabel[row.status as BidStatus] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="150">
        <template #default="{ row }">{{ fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <el-button
            v-for="action in actions[row.status as BidStatus]"
            :key="action.to"
            size="small"
            :type="action.type"
            plain
            @click="doAction(row, action.to)"
          >
            {{ action.label }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.page-wrap { padding: 18px 0 0; min-height: 60vh; }
.head { display: flex; align-items: baseline; gap: 14px; }
.page-title { font-size: 20px; font-weight: 700; }
.sub { font-size: 13px; color: var(--text-sub); }
.toolbar { margin: 14px 0; }
.bid-table { background: #fff; border-radius: 10px; padding: 8px; }
.proj-name { color: var(--text-main); font-weight: 500; }
.proj-name:hover { color: var(--brand); }
.no-file { color: var(--text-sub); }
</style>
