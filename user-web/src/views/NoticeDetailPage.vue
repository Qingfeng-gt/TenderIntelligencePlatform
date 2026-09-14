<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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

/** 附件列表(列表接口不返回附件, 这里只可能来自详情接口) */
const attachments = computed(() => notice.value?.attachments ?? [])

/**
 * 正文里的外链(如附件直链)一律新窗口打开
 *
 * 正文是源站 HTML 原样渲染的, 里面的 <a> 没有 target。不拦的话点击会在 SPA 内直接跳走,
 * 用户丢失当前公告且返回不便。
 */
function openLinkInNewTab(event: MouseEvent) {
  const anchor = (event.target as HTMLElement | null)?.closest?.('a')
  const href = anchor?.getAttribute('href')
  if (!href || href.startsWith('#')) return
  event.preventDefault()
  window.open(href, '_blank', 'noopener,noreferrer')
}

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

      <div v-if="attachments.length" class="attach-box">
        <h3 class="content-title">
          附件下载
          <span class="attach-count">{{ attachments.length }}</span>
        </h3>
        <ul class="attach-list">
          <li v-for="item in attachments" :key="item.fileUrl" class="attach-item">
            <span class="attach-type">{{ (item.fileType || 'file').toUpperCase() }}</span>
            <a class="attach-name" :href="item.fileUrl" target="_blank" rel="noopener noreferrer" :title="item.fileName">
              {{ item.fileName }}
            </a>
            <span v-if="item.fileSize" class="attach-size">{{ item.fileSize }}</span>
          </li>
        </ul>
        <p class="attach-tip">附件由源站提供,点击在新窗口打开源站链接下载</p>
      </div>

      <h3 class="content-title">公告详情</h3>
      <div class="content" v-html="notice.content" @click="openLinkInNewTab"></div>
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
.attach-box { margin-top: 22px; }
.attach-box .content-title { margin-top: 0; display: flex; align-items: center; gap: 8px; }
.attach-count {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-sub);
  background: var(--bg-soft);
  border-radius: 9px;
  padding: 1px 8px;
}
.attach-list { list-style: none; padding: 0; margin: 0; }
.attach-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--border-soft, #ebeef5);
  font-size: 14px;
}
.attach-item:last-child { border-bottom: none; }
.attach-type {
  flex: none;
  min-width: 38px;
  text-align: center;
  font-size: 11px;
  font-weight: 600;
  color: #0065ef;
  background: rgba(0, 101, 239, 0.08);
  border-radius: 4px;
  padding: 2px 6px;
}
.attach-name { color: #0065ef; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.attach-name:hover { text-decoration: underline; }
.attach-size { flex: none; margin-left: auto; font-size: 12px; color: var(--text-sub); }
.attach-tip { margin-top: 10px; font-size: 12px; color: var(--text-sub); }
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
