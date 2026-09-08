<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { hotTags, getHomeStat, listNotices } from '@/api/notice'
import { formatDateTimeShort } from '@/utils/format'
import type { HomeStat, Notice, PageResult } from '@/types'

const router = useRouter()
const keyword = ref('')
const stat = ref<HomeStat>({ todayCount: 0, totalCount: 0, provinces: 0, industries: 0 })
const latest = ref<Notice[]>([])
const loading = ref(false)

const typeTabs = [
  { key: '', label: '全部' },
  { key: 'tender', label: '招标公告' },
  { key: 'win', label: '中标公告' },
  { key: 'change', label: '变更公告' },
  { key: 'explore', label: '采购公告' }
]
const activeTab = ref('')

const typeLabel: Record<string, string> = { tender: '招标', win: '中标', change: '变更', explore: '采购' }

async function loadLatest() {
  loading.value = true
  const res: PageResult<Notice> = await listNotices({ type: activeTab.value as any, page: 1, pageSize: 8 })
  latest.value = res.list
  loading.value = false
}

function doSearch() {
  router.push({ path: '/notice', query: { keyword: keyword.value } })
}

function pickTag(name: string) {
  keyword.value = name
  doSearch()
}


onMounted(async () => {
  stat.value = await getHomeStat()
  loadLatest()
})
</script>

<template>
  <div>
    <!-- Hero -->
    <section class="hero">
      <div class="container hero-inner">
        <h1 class="hero-title">找项目 · 查标讯 · 抢商机</h1>
        <p class="hero-sub">每日 12000+ 条招标、中标、采购信息,实时汇聚</p>
        <div class="search-wrap">
          <el-input
            v-model="keyword"
            size="large"
            placeholder="输入关键词,如:市政道路 / 数据中心 / 光伏 / 医院…"
            class="search-input"
            @keyup.enter="doSearch"
          >
            <template #prefix>
              <el-icon><component :is="Search" /></el-icon>
            </template>
          </el-input>
          <el-button size="large" type="primary" class="search-btn" @click="doSearch">搜索标讯</el-button>
        </div>
        <div class="quick">
          <span class="quick-label">快速查找:</span>
          <el-button v-for="t in typeTabs.slice(1)" :key="t.key" size="small" round @click="router.push(`/notice?type=${t.key}`)">
            {{ t.label }}
          </el-button>
        </div>
      </div>
    </section>

    <!-- 统计 -->
    <section class="container stats">
      <div class="stat-item">
        <div class="stat-num">{{ stat.todayCount }}</div>
        <div class="stat-label">今日新增</div>
      </div>
      <div class="stat-item">
        <div class="stat-num">{{ stat.totalCount }}</div>
        <div class="stat-label">数据总量</div>
      </div>
      <div class="stat-item">
        <div class="stat-num">{{ stat.provinces }}</div>
        <div class="stat-label">覆盖省份</div>
      </div>
      <div class="stat-item">
        <div class="stat-num">{{ stat.industries }}</div>
        <div class="stat-label">行业分类</div>
      </div>
    </section>

    <!-- 内容区 -->
    <section class="container content-grid">
      <div class="main-col">
        <el-tabs v-model="activeTab" class="list-tabs" @tab-change="loadLatest">
          <el-tab-pane v-for="t in typeTabs" :key="t.key" :label="t.label" :name="t.key" />
        </el-tabs>
        <div v-loading="loading" class="list-box">
          <router-link
            v-for="n in latest"
            :key="n.id"
            :to="`/notice/${n.id}`"
            class="notice-row"
          >
            <span class="badge" :class="`badge-${n.type}`">{{ typeLabel[n.type] }}</span>
            <div class="n-body">
              <div class="n-title">{{ n.title }}</div>
              <div class="n-meta">
                <span>{{ n.province }}·{{ n.city }}</span>
                <span>{{ n.industry }}</span>
                <span v-if="n.budget">预算 {{ n.budget }} 万元</span>
              </div>
            </div>
            <div class="n-time">{{ formatDateTimeShort(n.publishTime) }}</div>
          </router-link>
          <el-empty v-if="!latest.length && !loading" description="暂无数据" :image-size="60" />
        </div>
      </div>

      <!-- 右侧栏 -->
      <aside class="side-col">
        <div class="card">
          <h3 class="card-title">热门关键词</h3>
          <div class="tag-cloud">
            <a v-for="t in hotTags" :key="t.name" class="tag-item" @click.prevent="pickTag(t.name)">
              {{ t.name }}
              <em>{{ t.count }}</em>
            </a>
          </div>
        </div>
        <div class="card">
          <h3 class="card-title">订阅提醒</h3>
          <p class="card-tip">关注行业关键词,项目机会第一时间推送</p>
          <el-button type="primary" plain style="width: 100%" @click="pickTag('数据中心')">立即订阅</el-button>
        </div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.hero {
  background: linear-gradient(135deg, #1e6fff 0%, #3b82f6 55%, #38bdf8 100%);
  padding: 46px 0 58px;
  color: #fff;
}
.hero-inner { text-align: center; }
.hero-title { font-size: 32px; font-weight: 700; letter-spacing: 1px; }
.hero-sub { margin-top: 10px; opacity: 0.9; font-size: 15px; }
.search-wrap {
  margin: 26px auto 14px;
  max-width: 640px;
  display: flex;
  gap: 10px;
}
.search-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
}
.search-btn {
  padding: 0 34px;
  background: #0b3fa8;
  border-color: #0b3fa8;
  font-size: 16px;
}
.quick { font-size: 13px; display: flex; justify-content: center; gap: 6px; align-items: center; }
.quick-label { opacity: 0.85; }
.quick :deep(.el-button) { background: rgba(255, 255, 255, 0.18); border: none; color: #fff; }

.stats {
  display: flex;
  gap: 12px;
  margin-top: -26px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(31, 41, 55, 0.06);
  padding: 20px 0;
}
.stat-item { flex: 1; text-align: center; }
.stat-num { font-size: 26px; font-weight: 700; color: var(--brand); }
.stat-label { margin-top: 2px; font-size: 13px; color: var(--text-sub); }

.content-grid {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
  margin-top: 26px;
}
.main-col, .card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px 20px;
  box-shadow: 0 2px 10px rgba(31, 41, 55, 0.04);
}
.card { margin-bottom: 16px; }
.card-title { font-size: 16px; margin-bottom: 12px; }
.card-tip { font-size: 13px; color: var(--text-sub); margin-bottom: 12px; }
.tag-cloud { display: flex; flex-wrap: wrap; gap: 10px; }
.tag-item {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 5px 12px;
  background: var(--bg-soft);
  border-radius: 14px;
  font-size: 13px;
  cursor: pointer;
}
.tag-item:hover { background: #e8f1ff; color: var(--brand); }
.tag-item em { font-style: normal; font-size: 12px; color: #9ca3af; }
</style>
