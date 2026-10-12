import { createSSRApp } from 'vue'
import App from './App.vue'

// UniApp 的入口约定：必须导出 createApp，内部用 createSSRApp（为了各端渲染一致性）
export function createApp() {
  const app = createSSRApp(App)
  return { app }
}
