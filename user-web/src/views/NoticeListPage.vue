<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getAllIndustries, listNotices } from '@/api/notice'
import { formatDateTimeShort } from '@/utils/format'
import type { Notice, NoticeQuery, PageResult } from '@/types'

const route = useRoute()
const router = useRouter()

const industries = getAllIndustries()

// 省份下拉: label 用全称便于阅读, value 用简称 —— 必须与后端入库口径一致
// notice.province 由 NoticeRegionExtractor 规则推导并按【简称】入库(北京/湖北/新疆),
// 而后端查询是精确相等匹配; 此前 value 传全称(北京市/湖北省)导致筛选恒不命中, 只命中
// 恰好用全称的种子数据。口径说明见 doc/技术/06-数据采集设计.md §5.1
const provinces = [
  { label: '北京市', value: '北京' }, { label: '天津市', value: '天津' },
  { label: '上海市', value: '上海' }, { label: '重庆市', value: '重庆' },
  { label: '河北省', value: '河北' }, { label: '山西省', value: '山西' },
  { label: '内蒙古自治区', value: '内蒙古' }, { label: '辽宁省', value: '辽宁' },
  { label: '吉林省', value: '吉林' }, { label: '黑龙江省', value: '黑龙江' },
  { label: '江苏省', value: '江苏' }, { label: '浙江省', value: '浙江' },
  { label: '安徽省', value: '安徽' }, { label: '福建省', value: '福建' },
  { label: '江西省', value: '江西' }, { label: '山东省', value: '山东' },
  { label: '河南省', value: '河南' }, { label: '湖北省', value: '湖北' },
  { label: '湖南省', value: '湖南' }, { label: '广东省', value: '广东' },
  { label: '广西壮族自治区', value: '广西' }, { label: '海南省', value: '海南' },
  { label: '四川省', value: '四川' }, { label: '贵州省', value: '贵州' },
  { label: '云南省', value: '云南' }, { label: '西藏自治区', value: '西藏' },
  { label: '陕西省', value: '陕西' }, { label: '甘肃省', value: '甘肃' },
  { label: '青海省', value: '青海' }, { label: '宁夏回族自治区', value: '宁夏' },
  { label: '新疆维吾尔自治区', value: '新疆' },
]

const query = reactive<NoticeQuery>({
  keyword: '',
  type: '',
  province: '',
  industry: '',
  timeRange: '',
  page: 1,
  pageSize: 10
})

function syncFromQuery() {
  query.type = ((route.query.type as string) || '') as NoticeQuery['type']
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
  query.type = ''
  query.province = ''
  query.industry = ''
  query.timeRange = ''
  query.page = 1
  load()
}

function changeType(key: string) {
  query.type = key as NoticeQuery['type']
  load()
}

onMounted(() => {
  syncFromQuery()
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
const typeLabel: Record<string, string> = { tender: '招标', win: '中标', change: '变更', explore: '采购' }
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
      <el-radio-group v-model="query.type" size="small" @change="changeType">
        <el-radio-button v-for="t in typeTabs" :key="t.key" :value="t.key">{{ t.label }}</el-radio-button>
      </el-radio-group>
      <div class="search-row">
        <el-input v-model="query.keyword" placeholder="关键词搜索:如 市政 / 风电 / 医院…" clearable class="kw-input" @keyup.enter="search">
          <template #prefix><el-icon><component :is="Search" /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </div>
    </div>

    <!-- 筛选行 -->
    <div class="filters">
      <div class="filter-row">
        <span class="f-label">地区</span>
        <el-select v-model="query.province" placeholder="全部省份" clearable size="default" style="width: 200px" @change="search">
          <el-option v-for="p in provinces" :key="p.value" :label="p.label" :value="p.value" />
        </el-select>
        <span class="f-label">行业</span>
        <el-select v-model="query.industry" placeholder="全部行业" clearable size="default" style="width: 200px" @change="search">
          <el-option v-for="i in industries" :key="i" :label="i" :value="i" />
        </el-select>
        <span class="f-label">时间</span>
        <el-select v-model="query.timeRange" size="default" style="width: 160px" @change="search">
          <el-option v-for="t in timeOptions" :key="t.key" :label="t.label" :value="t.key" />
        </el-select>
      </div>
    </div>

    <!-- 列表 -->
    <div class="list-card">
      <div class="list-head">
        <span>共 <b class="total">{{ total }}</b> 条公告</span>
      </div>
      <div v-loading="loading" class="list-box">
        <router-link v-for="n in list" :key="n.id" :to="`/notice/${n.id}`" class="notice-row">
          <span class="badge" :class="`badge-${n.type}`">{{ typeLabel[n.type] }}</span>
          <div class="n-body">
            <div class="n-title">{{ n.title }}</div>
            <div class="n-meta">
              <span>{{ n.province }}·{{ n.city }}</span>
              <span>{{ n.industry }}</span>
              <span v-if="n.budget">预算 {{ n.budget }} 万元</span>
              <span>{{ n.agency }}</span>
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
