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
const provinces = ['北京市', '天津市', '河北省', '内蒙古自治区', '辽宁省', '吉林省', '黑龙江省', '上海市', '江苏省', '浙江省', '安徽省', '福建省', '江西省', '山东省', '河南省', '湖北省', '湖南省', '广东省', '广西壮族自治区', '海南省', '重庆市', '四川省', '贵州省', '云南省', '陕西省', '甘肃省', '青海省', '宁夏回族自治区', '新疆维吾尔自治区', '西藏自治区']

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
          <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
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
