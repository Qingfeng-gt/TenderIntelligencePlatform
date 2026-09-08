<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Verify } from '@/components/Verifition'
import { memberRegister, getMemberProfile } from '@/api/auth'
import { setToken, setProfile } from '@/utils/auth'

const router = useRouter()

const form = reactive({ username: '', password: '', confirmPassword: '', nickname: '' })
const captchaRef = ref()
const captchaVerification = ref('')
const loading = ref(false)

function resetCaptcha() {
  captchaVerification.value = ''
  captchaRef.value?.refresh?.()
}

async function onCaptchaSuccess(params: any) {
  captchaVerification.value = params.captchaVerification
  await doRegister()
}

async function doRegister() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    resetCaptcha()
    return
  }
  if (!/^[a-zA-Z0-9]{4,30}$/.test(form.username)) {
    ElMessage.warning('账号须为 4-30 位数字或字母')
    resetCaptcha()
    return
  }
  if (form.password.length < 4) {
    ElMessage.warning('密码至少 4 位')
    resetCaptcha()
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    resetCaptcha()
    return
  }
  loading.value = true
  try {
    const resp = await memberRegister({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      captchaVerification: captchaVerification.value
    })
    setToken(resp.accessToken)
    // 拉取并缓存会员档案,失败则用注册信息兜底
    try {
      setProfile(await getMemberProfile())
    } catch {
      setProfile({ id: resp.userId, username: form.username, nickname: form.nickname || form.username })
    }
    ElMessage.success('注册成功,已自动登录')
    router.replace('/')
  } catch (e: any) {
    ElMessage.error(e?.message || '注册失败')
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
        <h2 class="auth-title">标讯通 · 注册</h2>
        <p class="auth-sub">注册即可发起投标,跟踪项目进展</p>
      </div>

      <div class="auth-form">
        <el-input v-model="form.username" size="large" placeholder="账号(4-30 位字母数字)" @keyup.enter="captchaRef?.show?.()" />
        <el-input v-model="form.nickname" size="large" placeholder="昵称(选填)" @keyup.enter="captchaRef?.show?.()" />
        <el-input
          v-model="form.password"
          size="large"
          type="password"
          show-password
          placeholder="密码(至少 4 位)"
          @keyup.enter="captchaRef?.show?.()"
        />
        <el-input
          v-model="form.confirmPassword"
          size="large"
          type="password"
          show-password
          placeholder="确认密码"
          @keyup.enter="captchaRef?.show?.()"
        />

        <Verify
          ref="captchaRef"
          captcha-type="blockPuzzle"
          mode="pop"
          :img-size="{ width: '400px', height: '200px' }"
          @success="onCaptchaSuccess"
          @error="resetCaptcha"
        />

        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="captchaRef?.show?.()">
          注 册
        </el-button>

        <div class="auth-more">
          已有账号?
          <router-link to="/login" class="link">直接登录</router-link>
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
.auth-form { display: flex; flex-direction: column; gap: 14px; }
.submit-btn { width: 100%; }
.auth-more { text-align: center; font-size: 13px; color: var(--text-sub); }
.link { color: var(--brand); }
</style>
