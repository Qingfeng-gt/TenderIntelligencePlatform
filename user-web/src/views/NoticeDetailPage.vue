<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar } from '@element-plus/icons-vue'
import { getNoticeDetail } from '@/api/notice'
import { createBidProject } from '@/api/bid'
import { formatDateTime } from '@/utils/format'
import { isLoggedIn } from '@/utils/auth'
import type { Notice } from '@/types'

const route = useRoute()
const router = useRouter()
const notice = ref<Notice | null>(null)
const loading = ref(false)

/** 发起投标弹窗状态 */
const bidDialogVisible = ref(false)
const submitting = ref(false)
const bidAmount = ref<number | null>(null)
const bidFileName = ref('')

const typeLabel: Record<string, string> = { tender: '招标公告', win: '中标公告', change: '变更公告', explore: '采购公告' }
const typeShort: Record<string, string> = { tender: '招标', win: '中标', change: '变更', explore: '采购' }

function formatDate(t: number | string) {
  return formatDateTime(t)
}

async function submitBid() {
  if (!notice.value) return
  submitting.value = true
  try {
    await createBidProject({
      noticeId: notice.value.id,
      bidAmount: bidAmount.value ?? undefined,
      bidFileName: bidFileName.value || undefined
    })
    ElMessage.success('已递交投标请求,请在「我的投标」中跟进')
    bidDialogVisible.value = false
    router.push('/my-bids')
  } finally {
    submitting.value = false
  }
}

/** 打开投标弹窗:未登录先跳转登录页 */
function openBidDialog() {
  if (!isLoggedIn()) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  bidDialogVisible.value = true
}

onMounted(async () => {
  loading.value = true
  notice.value = await getNoticeDetail(Number(route.params.id))
  loading.value = false
})
</script>

<template>
  <div class="container page-wrap" v-loading="loading">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/notice' }">标讯大厅</el-breadcrumb-item>
      <el-breadcrumb-item>{{ notice ? typeLabel[notice.type] : '详情' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div v-if="notice" class="detail-card">
      <div class="detail-head">
        <div class="title-row">
          <span class="badge" :class="`badge-${notice.type}`">{{ typeShort[notice.type] }}</span>
          <h1 class="title">{{ notice.title }}</h1>
        </div>
        <div class="sub-row">
          <span><el-icon><component :is="Calendar" /></el-icon> 发布时间:{{ formatDate(notice.publishTime) }}</span>
          <span>来源:{{ notice.source || '公开渠道' }}</span>
          <el-button size="small" text type="primary" @click="router.push({ path: '/notice', query: { type: notice.type } })">
            更多{{ typeLabel[notice.type] }} →
          </el-button>
          <el-button
            v-if="notice.type === 'tender'"
            type="primary"
            size="small"
            @click="openBidDialog"
            class="bid-btn"
          >
            发起投标
          </el-button>
        </div>
      </div>

      <div class="info-grid">
        <div class="cell"><span class="k">所在地区</span><span class="v">{{ notice.province }} · {{ notice.city }}</span></div>
        <div class="cell"><span class="k">所属行业</span><span class="v">{{ notice.industry }}</span></div>
        <div class="cell"><span class="k">项目编号</span><span class="v">{{ notice.projectNo || '—' }}</span></div>
        <div class="cell"><span class="k">预算金额</span><span class="v">{{ notice.budget ? notice.budget + ' 万元' : '—' }}</span></div>
        <div class="cell"><span class="k">投标截止</span><span class="v">{{ notice.deadline ? formatDate(notice.deadline) : '—' }}</span></div>
        <div class="cell"><span class="k">招标人</span><span class="v">{{ notice.tenderPerson || '—' }}</span></div>
        <div class="cell"><span class="k">代理机构</span><span class="v">{{ notice.agency || '—' }}</span></div>
        <div class="cell"><span class="k">联系人</span><span class="v">{{ notice.contact || '—' }}</span></div>
      </div>

      <h3 class="content-title">公告详情</h3>
      <div class="content" v-html="notice.content"></div>
    </div>

    <el-dialog v-model="bidDialogVisible" title="发起投标" width="440px">
      <div class="bid-form">
        <p class="bid-form-title">{{ notice?.title }}</p>
        <el-form label-width="100px">
          <el-form-item label="拟投标金额">
            <el-input-number v-model="bidAmount" :min="0" :step="1" :precision="2" class="amount-input" />
            <span class="unit">万元</span>
          </el-form-item>
          <el-form-item label="投标文件">
            <el-input v-model="bidFileName" placeholder="如: 标书-XXX工程.docx(演示用,不作上传)" />
          </el-form-item>
          <el-form-item label="截止时间">
            <span class="deadline">{{ notice?.deadline ? formatDate(notice.deadline) : '以公告为准' }}</span>
          </el-form-item>
        </el-form>
        <p class="tip">演示流程: 采用并纳入投标项目管理,正式版将关联会员鉴权与文件上传</p>
      </div>
      <template #footer>
        <el-button @click="bidDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBid">确认递交</el-button>
      </template>
    </el-dialog>

    <el-empty v-if="!notice && !loading" description="公告不存在或已下线">
      <el-button type="primary" @click="router.push('/notice')">返回列表</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.page-wrap { padding: 18px 0 0; min-height: 60vh; }
.detail-card {
  background: #fff;
  border-radius: 10px;
  margin-top: 14px;
  padding: 24px 28px 36px;
}
.title-row { display: flex; gap: 10px; align-items: flex-start; }
.title { font-size: 21px; line-height: 1.6; font-weight: 700; }
.sub-row {
  margin-top: 12px;
  display: flex;
  gap: 22px;
  align-items: center;
  font-size: 13px;
  color: var(--text-sub);
}
.info-grid {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  background: var(--bg-soft);
  border-radius: 8px;
  padding: 16px 18px;
}
.cell { font-size: 13px; overflow: hidden; }
.cell .k { color: var(--text-sub); margin-right: 8px; }
.cell .v { color: var(--text-main); font-weight: 500; }
.content-title { margin: 26px 0 12px; font-size: 16px; }
.content { font-size: 14.5px; line-height: 2; color: #374151; }
.content :deep(p) { margin: 8px 0; }
.content :deep(h3) { font-size: 15px; margin: 14px 0 6px; }
.bid-btn { margin-left: auto; }
.bid-form-title { font-size: 13px; font-weight: 600; margin: 2px 0 14px; }
.amount-input { width: 160px; }
.unit { margin-left: 8px; color: var(--text-sub); }
.deadline { color: var(--text-main); }
.tip { margin-top: 6px; font-size: 12px; color: var(--text-sub); }
</style>
