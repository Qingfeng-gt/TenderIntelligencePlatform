<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { isLoggedIn, getProfile, setProfile, clearAuth } from '@/utils/auth'
import { memberLogout, getMemberProfile } from '@/api/auth'
import type { MemberProfile } from '@/types'

const router = useRouter()
const profile = ref<MemberProfile | null>(null)

const nav = [
  { label: '首页', to: '/' },
  { label: '招标公告', to: '/notice?type=tender' },
  { label: '中标公告', to: '/notice?type=win' },
  { label: '变更公告', to: '/notice?type=change' },
  { label: '采购公告', to: '/notice?type=explore' },
  { label: '我的投标', to: '/my-bids' }
]

onMounted(async () => {
  if (!isLoggedIn()) {
    return
  }
  profile.value = getProfile()
  // 有 token 但无档案缓存(如刚注册/被清理)时,主动拉取一次
  if (!profile.value) {
    try {
      profile.value = await getMemberProfile()
      setProfile(profile.value)
    } catch {
      // 拉取失败保留未登录态展示,交由 401 拦截处理
    }
  }
})

async function logout() {
  try {
    await memberLogout()
  } catch {
    // 登出接口失败也继续清本地状态
  }
  clearAuth()
  profile.value = null
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<template>
  <div class="portal">
    <header class="topbar">
      <div class="container topbar-inner">
        <router-link to="/" class="logo">
          <span class="logo-mark">标</span>
          <span class="logo-text">标讯通</span>
        </router-link>
        <nav class="nav">
          <router-link v-for="item in nav" :key="item.to" :to="item.to" class="nav-item">
            {{ item.label }}
          </router-link>
        </nav>
        <div class="right">
          <el-dropdown v-if="profile">
            <span class="user-chip">
              <span class="user-avatar">{{ (profile.nickname || profile.username).slice(0, 1) }}</span>
              {{ profile.nickname || profile.username }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/my-bids')">我的投标</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <router-link v-else to="/login">
            <el-button type="primary" link>登录</el-button>
          </router-link>
        </div>
      </div>
    </header>

    <main>
      <router-view />
    </main>

    <footer class="footer">
      <div class="container">
        <p>标讯通 —— 招投标信息服务平台 · 专注项目机会发现</p>
        <p class="sub">数据来源于公开渠道,最终以官方发布为准</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.topbar {
  background: #fff;
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
}
.topbar-inner {
  display: flex;
  align-items: center;
  gap: 36px;
  height: 60px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 700;
}
.logo-mark {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--brand);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}
.nav {
  display: flex;
  gap: 28px;
  flex: 1;
}
.nav-item {
  font-size: 15px;
  color: var(--text-main);
  padding: 4px 0;
}
.nav-item:hover,
.nav-item.router-link-active {
  color: var(--brand);
  font-weight: 600;
}
.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-main);
  outline: none;
}
.user-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--brand);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}
.footer {
  margin-top: 48px;
  background: #fff;
  border-top: 1px solid var(--border);
  padding: 28px 0 36px;
  text-align: center;
  font-size: 13px;
  color: var(--text-sub);
}
.footer .sub {
  margin-top: 6px;
  font-size: 12px;
  opacity: 0.8;
}
</style>
