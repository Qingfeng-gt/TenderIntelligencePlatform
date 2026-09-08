import request from '@/config/axios'
import type { UserLoginVO } from './types'

// 登录
export const login = (data: UserLoginVO) => {
  return request.post({
    url: '/system/auth/login',
    data,
    headers: {
      isEncrypt: false
    }
  })
}

// 登出
export const loginOut = () => {
  return request.post({ url: '/system/auth/logout' })
}

// 获取用户权限信息
export const getInfo = () => {
  return request.get({ url: '/system/auth/get-permission-info' })
}

// 获取验证图片以及 token
export const getCode = (data: any) => {
  return request.postOriginal({ url: 'system/captcha/get', data })
}

// 滑动或者点选验证
export const reqCheck = (data: any) => {
  return request.postOriginal({ url: 'system/captcha/check', data })
}
