<template>
  <el-row class="mb-20px">
    <el-col :span="13">
      <el-button type="primary" @click="handleRun()" :loading="runLoading">
        <Icon icon="ep:video-play" class="mr-5px" />
        采集全部
      </el-button>
      <el-button @click="initialLoad">刷新</el-button>
    </el-col>
    <el-col :span="11" class="text-right">
      <el-tag type="info">定时采集默认每 2 小时一轮</el-tag>
    </el-col>
  </el-row>

  <!-- 站点配置列表 -->
  <ContentWrap v-loading="siteLoading">
    <el-row :gutter="20">
      <el-col v-for="item in siteList" :key="item.id" :xs="24" :sm="12" :lg="8" class="mb-20px">
        <el-card shadow="hover">
          <template #header>
            <div class="flex-align-center">
              <span class="font-bold">{{ item.name }}</span>
              <el-tag :type="item.enabled ? 'success' : 'info'" size="small" class="ml-10px">
                {{ item.enabled ? '启用' : '停用' }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="1" size="small">
            <el-descriptions-item label="站点标识">{{ item.code }}</el-descriptions-item>
            <el-descriptions-item label="频道数">{{ channelCount(item.channels) }}</el-descriptions-item>
            <el-descriptions-item label="最近任务">
              <el-tag v-if="latestMap[item.code]" :type="statusType(latestMap[item.code].status)" size="small">
                {{ statusLabel(latestMap[item.code].status) }}
              </el-tag>
              <span v-else>暂无</span>
            </el-descriptions-item>
            <el-descriptions-item label="最近新增/更新">
              <template v-if="latestMap[item.code]">
                {{ latestMap[item.code].newInsert ?? 0 }} / {{ latestMap[item.code].updated ?? 0 }}
              </template>
              <template v-else>-</template>
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              <span v-if="latestMap[item.code]">{{ formatDate(latestMap[item.code].endTime) }}</span>
              <span v-else>-</span>
            </el-descriptions-item>
          </el-descriptions>
          <div class="text-right mt-10px">
            <el-button type="primary" link :loading="runLoading" @click="handleRun(item.code)">
              <Icon icon="ep:video-play" class="mr-5px" />
              立即采集
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col v-if="!siteLoading && siteList.length === 0" :span="24">
        <el-empty description="暂无采集站点配置" />
      </el-col>
    </el-row>
  </ContentWrap>

  <!-- 采集任务日志 -->
  <ContentWrap class="mt-15px">
    <el-form class="-mb-15px" :model="queryParams" :inline="true" label-width="68px">
      <el-form-item label="站点" prop="siteCode">
        <el-select v-model="queryParams.siteCode" placeholder="全部站点" clearable class="!w-240px">
          <el-option v-for="item in siteList" :key="item.code" :label="item.name" :value="item.code" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="taskLoading" :data="taskList" stripe>
      <el-table-column label="任务编号" align="center" prop="id" width="100" />
      <el-table-column label="站点" align="center" width="130">
        <template #default="scope">{{ siteName(scope.row.siteCode) }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="列表/解析" align="center" width="90">
        <template #default="scope">{{ scope.row.listFetched ?? 0 }} / {{ scope.row.listParsed ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="详情/失败" align="center" width="90">
        <template #default="scope">{{ scope.row.detailFetched ?? 0 }} / {{ scope.row.detailFailed ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="新增/更新" align="center" width="90">
        <template #default="scope">{{ scope.row.newInsert ?? 0 }} / {{ scope.row.updated ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" width="160">
        <template #default="scope">{{ formatDate(scope.row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" width="160">
        <template #default="scope">{{ formatDate(scope.row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="错误信息" align="center" show-overflow-tooltip prop="errorMsg" />
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ContentWrap } from '@/components/ContentWrap'
import Icon from '@/components/Icon/src/Icon.vue'
import Pagination from '@/components/Pagination/index.vue'
import { formatDate } from '@/utils/formatTime'
import {
  CrawlerSiteVO,
  CrawlerTaskVO,
  getCrawlerSiteList,
  getCrawlerTaskPage,
  getCrawlerLatestTask,
  runCrawler
} from '@/api/crawler'

defineOptions({ name: 'CrawlerCenter' })

/** 站点列表 */
const siteList = ref<CrawlerSiteVO[]>([])
const siteLoading = ref(false)
/** 每站点最近任务 */
const latestMap = ref<Record<string, CrawlerTaskVO>>({})

/** 任务日志 */
const taskLoading = ref(false)
const taskList = ref<CrawlerTaskVO[]>([])
const total = ref(0)
const queryParams = ref({ pageNo: 1, pageSize: 10, siteCode: undefined })
const runLoading = ref(false)

/** 轮询状态 */
let timer: number | undefined

const statusType = (status: string): 'primary' | 'success' | 'danger' | 'info' => {
  const map: Record<string, 'primary' | 'success' | 'danger' | 'info'> = {
    RUNNING: 'primary',
    SUCCESS: 'success',
    FAILED: 'danger'
  }
  return map[status] ?? 'info'
}

const statusLabel = (status: string) => {
  const map: Record<string, string> = { RUNNING: '执行中', SUCCESS: '成功', FAILED: '失败' }
  return map[status] ?? (status || '-')
}

const channelCount = (channels: string) => {
  try {
    const arr = JSON.parse(channels || '[]')
    return Array.isArray(arr) ? arr.length : 0
  } catch {
    return 0
  }
}

const siteName = (code?: string) => {
  return siteList.value.find((item) => item.code === code)?.name ?? code ?? '-'
}

const loadSites = async () => {
  siteLoading.value = true
  try {
    const data = await getCrawlerSiteList()
    siteList.value = data
    await refreshLatest()
  } finally {
    siteLoading.value = false
  }
}

/** 刷新各站点最近任务状态 */
const refreshLatest = async () => {
  const tasks = await Promise.all(
    siteList.value.map((item) => getCrawlerLatestTask(item.code).catch(() => null))
  )
  const map: Record<string, CrawlerTaskVO> = {}
  siteList.value.forEach((item, index) => {
    if (tasks[index]) {
      map[item.code] = tasks[index]
    }
  })
  latestMap.value = map
}

const getList = async () => {
  taskLoading.value = true
  try {
    const data = await getCrawlerTaskPage(queryParams.value)
    taskList.value = data.list
    total.value = data.total
  } finally {
    taskLoading.value = false
  }
}

const handleQuery = () => {
  queryParams.value.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.value = { pageNo: 1, pageSize: 10, siteCode: undefined }
  getList()
}

/** 手动触发采集(不传站点 = 采集全部启用站点) */
const handleRun = async (siteCode?: string) => {
  runLoading.value = true
  try {
    const taskId = await runCrawler(siteCode)
    ElMessage.success(siteCode ? `采集任务 #${taskId} 已创建,稍后刷新查看` : `采集任务 #${taskId} 已创建,将所有站点入队`)
    await refreshLatest()
    await getList()
  } finally {
    runLoading.value = false
  }
}

const initialLoad = () => {
  loadSites()
  getList()
}

onMounted(() => {
  initialLoad()
  // 定时轮询最近任务状态(站点卡片 + 日志), 5s 一次
  timer = window.setInterval(() => {
    refreshLatest()
  }, 5000)
})

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>
