<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Verify } from '@/components/Verifition'
import { memberLogin, getMemberProfile } from '@/api/auth'
import { setToken, setProfile } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const form = reactive({ username: '', password: '' })
const captchaRef = ref()
const captchaVerification = ref('')
const loading = ref(false)

function resetCaptcha() {
  captchaVerification.value = ''
  captchaRef.value?.refresh?.()
}

async function onCaptchaSuccess(params: any) {
  captchaVerification.value = params.captchaVerification
  await doLogin()
}

async function doLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    resetCaptcha()
    return
  }
  loading.value = true
  try {
    const resp = await memberLogin({
      username: form.username,
      password: form.password,
      captchaVerification: captchaVerification.value
    })
    setToken(resp.accessToken)
    // 拉取会员信息(昵称等),失败则先用账号名兜底
    try {
      const profile = await getMemberProfile()
      setProfile(profile)
    } catch {
      setProfile({ id: resp.userId, username: form.username, nickname: form.username })
    }
    ElMessage.success('登录成功')
    router.replace((route.query.redirect as string) || '/')
  } catch (e: any) {
    ElMessage.error(e?.message || '登录失败')
    resetCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-head">
        <span class="logo-mark">标</span>
        <h2 class="auth-title">标讯通 · 登录</h2>
        <p class="auth-sub">登录后可点击「发起投标」并管理「我的投标」</p>
      </div>

      <div class="auth-form">
        <el-input v-model="form.username" size="large" placeholder="账号(4-30 位字母数字)" @keyup.enter="captchaRef?.show?.()" />
        <el-input
          v-model="form.password"
          size="large"
          type="password"
          show-password
          placeholder="密码(4-16 位)"
          @keyup.enter="captchaRef?.show?.()"
        />

        <!-- 滑块验证码(弹窗式,点击登录触发) -->
        <Verify
          ref="captchaRef"
          captcha-type="blockPuzzle"
          mode="pop"
          :img-size="{ width: '400px', height: '200px' }"
          @success="onCaptchaSuccess"
          @error="resetCaptcha"
        />

        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="captchaRef?.show?.()">
          登 录
        </el-button>

        <div class="auth-more">
          还没有账号?
          <router-link to="/register" class="link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e6fff 0%, #3b82f6 55%, #38bdf8 100%);
}
.auth-card {
  width: 420px;
  background: #fff;
  border-radius: 14px;
  padding: 36px 40px 30px;
  box-shadow: 0 16px 40px rgba(15, 40, 80, 0.18);
}
.auth-head { text-align: center; margin-bottom: 24px; }
.logo-mark {
  display: inline-flex;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: var(--brand);
  color: #fff;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}
.auth-title { margin-top: 12px; font-size: 22px; font-weight: 700; }
.auth-sub { margin-top: 6px; font-size: 13px; color: var(--text-sub); }
.auth-form { display: flex; flex-direction: column; gap: 16px; }
.submit-btn { width: 100%; }
.auth-more { text-align: center; font-size: 13px; color: var(--text-sub); }
.link { color: var(--brand); }
</style>
