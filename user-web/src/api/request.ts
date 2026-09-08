import axios from 'axios'
import type { AxiosInstance } from 'axios'

/**
 * 后端接口封装:默认走 vite 代理 /admin-api → http://127.0.0.1:48080
 */
const instance: AxiosInstance = axios.create({
  baseURL: '/admin-api',
  timeout: 15000
})

instance.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body?.code !== 0) {
      return Promise.reject(new Error(body?.msg || '请求失败'))
    }
    return body.data
  },
  (err) => Promise.reject(err)
)

export default instance
