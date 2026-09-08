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
        }
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
