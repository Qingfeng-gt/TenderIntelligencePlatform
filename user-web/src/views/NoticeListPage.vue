<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getIndustries, getProvinces, listNotices } from '@/api/notice'
import { formatDateTimeShort } from '@/utils/format'
import type { Notice, NoticeQuery, PageResult, RegionOption } from '@/types'

const route = useRoute()

// 省份/行业均来自后端基础数据与公告规则抽取结果，不再在前端硬编码（避免口径漂移）
const provinces = ref<RegionOption[]>([])
const industries = ref<string[]>([])

const query = reactive<NoticeQuery>({
  keyword: '',
  noticeTypeCode: '',
  provinceCode: '',
  industry: '',
  timeRange: '',
  page: 1,
  pageSize: 10
})

function syncFromQuery() {
  query.noticeTypeCode = (route.query.type as string) || ''
  query.keyword = (route.query.keyword as string) || ''
}

const list = ref<Notice[]>([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  const res: PageResult<Notice> = await listNotices(query)
  list.value = res.list
  total.value = res.total
  loading.value = false
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.keyword = ''
  query.noticeTypeCode = ''
  query.provinceCode = ''
  query.industry = ''
  query.timeRange = ''
  query.page = 1
  load()
}

function changeType(key: string) {
  query.noticeTypeCode = key
  load()
}

onMounted(async () => {
  syncFromQuery()
  try {
    const [provinceList, industryList] = await Promise.all([getProvinces(), getIndustries()])
    provinces.value = provinceList
    industries.value = industryList
  } catch (e) {
    // 筛选项加载失败不阻塞公告列表，保留空下拉
    console.error('加载筛选项失败', e)
  }
  load()
})

watch(() => route.query, () => {
  syncFromQuery()
  load()
})

const typeTabs = [
  { key: '', label: '全部' },
  { key: 'tender', label: '招标公告' },
  { key: 'win', label: '中标公告' },
  { key: 'change', label: '变更公告' },
  { key: 'explore', label: '采购公告' }
]
const typeLabel: Record<string, string> = { tender: '招标', win: '中标', change: '变更', explore: '采购', qualify: '资格预审' }
const timeOptions = [
  { key: '', label: '不限时间' },
  { key: 'today', label: '今天' },
  { key: 'week', label: '近一周' },
  { key: 'month', label: '近一月' }
]

</script>

<template>
  <div class="container page-wrap">
    <!-- 工具条 -->
    <div class="toolbar">
      <el-radio-group v-model="query.noticeTypeCode" size="small" @change="changeType">
        <el-radio-button v-for="t in typeTabs" :key="t.key" :value="t.key">{{ t.label }}</el-radio-button>
      </el-radio-group>
      <div class="search-row">
        <el-input v-model="query.keyword" placeholder="关键词搜索:如 市政 / 风电 / 医院…" clearable class="kw-input" @keyup.enter="search">
          <template #prefix><el-icon><component :is="Search" /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </div>
      <!-- ══ 挂载点 A：AI 搜索入口（丁 · M4）══ 由毕设插槽 4 预留 ══
           第 5 周由丙在此接入丁的 AI 搜索入口（跳 /ai-search，或内联一个 AI 输入框）。
           丁**不能改本文件**（user-web 既有文件全归丙）—— 由丙留点、丙接线。
           契约见 doc/毕设 §5「NoticeListPage.vue 挂载点」。
      ══════════════════════════════════════════════════════════════ -->
    </div>

    <!-- 筛选行 -->
    <div class="filters">
      <div class="filter-row">
        <span class="f-label">地区</span>
        <el-select v-model="query.provinceCode" placeholder="全部省份" clearable size="default" style="width: 200px" @change="search">
          <el-option v-for="p in provinces" :key="p.code" :label="p.fullName || p.name" :value="p.code" />
        </el-select>
        <span class="f-label">行业</span>
        <el-select v-model="query.industry" placeholder="全部行业" clearable size="default" style="width: 200px" @change="search">
          <el-option v-for="i in industries" :key="i" :label="i" :value="i" />
        </el-select>
        <span class="f-label">时间</span>
        <el-select v-model="query.timeRange" size="default" style="width: 160px" @change="search">
          <el-option v-for="t in timeOptions" :key="t.key" :label="t.label" :value="t.key" />
        </el-select>
        <!-- ══ 挂载点 B：标签 chip（戊 · M5）══ 由毕设插槽 4 预留 ══
             第 5 周由丙在此接入戊的标签 chip（数据来自 /app-api/tag/cloud）。
             选中后把 tagIds 传给 /notice/list（参数已列进 doc/毕设 §5 契约表）。
             戊**不能改本文件** —— 由丙留点、丙接线。
        ═══════════════════════════════════════════════════════════ -->
      </div>
    </div>

    <!-- 列表 -->
    <div class="list-card">
      <div class="list-head">
        <span>共 <b class="total">{{ total }}</b> 条公告</span>
      </div>
      <div v-loading="loading" class="list-box">
        <router-link v-for="n in list" :key="n.id" :to="`/notice/${n.id}`" class="notice-row">
          <span class="badge" :class="`badge-${n.noticeTypeCode}`">{{ typeLabel[n.noticeTypeCode] }}</span>
          <div class="n-body">
            <div class="n-title">{{ n.title }}</div>
            <div class="n-meta">
              <span>{{ n.provinceName }}·{{ n.cityName }}</span>
              <span>{{ n.industry }}</span>
              <span v-if="n.budget">预算 {{ n.budget }} 万元</span>
              <span>{{ n.agencyName }}</span>
            </div>
          </div>
          <div class="n-time">{{ formatDateTimeShort(n.publishTime) }}</div>
        </router-link>
        <el-empty v-if="!list.length && !loading" description="没有找到匹配的公告,换个关键词试试" :image-size="80" />
      </div>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.pageSize"
          :total="total"
          layout="prev, pager, next, total"
          background
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-wrap { padding-top: 18px; }
.toolbar {
  background: #fff;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.search-row { display: flex; gap: 8px; }
.kw-input { width: 340px; }
.filters {
  background: #fff;
  border-radius: 10px;
  margin-top: 10px;
  padding: 12px 16px;
}
.filter-row { display: flex; align-items: center; gap: 6px 14px; flex-wrap: wrap; }
.f-label { font-size: 14px; color: var(--text-sub); }
.list-card {
  background: #fff;
  border-radius: 10px;
  margin-top: 10px;
  padding: 14px 16px 20px;
}
.list-head { font-size: 14px; color: var(--text-sub); padding-bottom: 4px; }
.total { color: var(--brand); font-size: 16px; margin: 0 2px; }
.pager { margin-top: 16px; display: flex; justify-content: center; }
</style>
