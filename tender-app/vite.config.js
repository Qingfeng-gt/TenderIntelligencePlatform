import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// UniApp 工程（毕设插槽 8-①）。归属：甲（M6 跨端框架选型与性能对比）。
//
// 三个硬约束，改之前先读：
//   1. vite 必须精确锁 5.2.8 —— @dcloudio/vite-plugin-uni 的 peerDependency 是精确版本，
//      不是范围。换 vite 版本会直接装不上。
//   2. 那一串 3.0.0-alpha-5030120260930001 是 @dcloudio 的 vue3 dist-tag 版本，
//      5 个包必须**同版本**，混用会编译失败。升级时一起升。
//   3. 本工程**不是 Maven 模块**，不进 backend/pom.xml —— 它是独立工程，与
//      admin-web/、user-web/、backend/ 平级。
export default defineConfig({
  plugins: [uni()]
})
