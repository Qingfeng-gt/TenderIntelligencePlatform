import axios from 'axios'
import type { AxiosInstance } from 'axios'
import { getToken, clearAuth } from '@/utils/auth'

/**
 * 用户端会员接口封装: /app-api → http://127.0.0.1:48080
 * 自动携带会员 token;未登录(401)时清除登录态并跳转登录页
 */
const instance: AxiosInstance = axios.create({
  baseURL: '/app-api',
  timeout: 15000
})

instance.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers!.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body?.code !== 0) {
      return Promise.reject(new Error(body?.msg || '请求失败'))
    }
    return body.data
  },
  (err) => {
    // 401: token 失效,退出登录并跳转
    if (err.response?.status === 401) {
      clearAuth()
      const redirect = encodeURIComponent(location.pathname + location.search)
      location.replace(`/login?redirect=${redirect}`)
    }
    return Promise.reject(err)
  }
)

export default instance
