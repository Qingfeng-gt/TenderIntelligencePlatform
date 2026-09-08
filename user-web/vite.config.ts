import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3001,
    host: '0.0.0.0',
    proxy: {
      '/admin-api': {
        target: 'http://127.0.0.1:48080',
        changeOrigin: true
      },
      // 用户端会员接口(登录/注册/投标),走 app-api 前缀
      '/app-api': {
        target: 'http://127.0.0.1:48080',
        changeOrigin: true
      }
    }
  }
})
