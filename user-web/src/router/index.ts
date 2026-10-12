import { createRouter, createWebHistory } from 'vue-router'
import PortalLayout from '@/layouts/PortalLayout.vue'
import { isLoggedIn } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: PortalLayout,
      children: [
        { path: '', name: 'home', component: () => import('@/views/HomePage.vue') },
        { path: 'notice', name: 'notice-list', component: () => import('@/views/NoticeListPage.vue') },
        { path: 'notice/:id', name: 'notice-detail', component: () => import('@/views/NoticeDetailPage.vue') },
        {
          path: 'my-bids',
          name: 'my-bids',
          component: () => import('@/views/MyBidsPage.vue'),
          meta: { requiresAuth: true }
        },

        // ── 毕设插槽 4 预置（见 doc/毕设 §7）──────────────────────────────
        // 占位页已建好，**丁/戊/丙 替换页面内容即可，不要再改本文件**。
        // 本文件是全组唯一无法避免的冲突点，预置后彻底消失。
        { path: 'ai-search', name: 'ai-search', component: () => import('@/views/AiSearchPage.vue') },
        { path: 'tag/explore', name: 'tag-explore', component: () => import('@/views/TagExplorePage.vue') },
        { path: 'favorite', name: 'favorite', component: () => import('@/views/FavoritePage.vue') }
      ]
    },
    { path: '/login', name: 'login', component: () => import('@/views/LoginPage.vue') },
    { path: '/register', name: 'register', component: () => import('@/views/RegisterPage.vue') }
  ],
  scrollBehavior: () => ({ top: 0 })
})

// 登录守卫:需要登录的路由跳转登录页;已登录不允许回登录/注册页
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if ((to.path === '/login' || to.path === '/register') && isLoggedIn()) {
    return { path: '/' }
  }
  return true
})

export default router
