import request from './app-request'
import type { MemberProfile, MemberTokenResp } from '@/types'

/** 会员账号密码登录 */
export function memberLogin(data: { username: string; password: string; captchaVerification?: string }): Promise<MemberTokenResp> {
  return request({ url: '/member/auth/login', method: 'post', data })
}

/** 会员注册(成功后自动登录) */
export function memberRegister(data: {
  username: string
  password: string
  nickname?: string
  captchaVerification?: string
}): Promise<MemberTokenResp> {
  return request({ url: '/member/auth/register', method: 'post', data })
}

/** 会员登出 */
export function memberLogout(): Promise<boolean> {
  return request({ url: '/member/auth/logout', method: 'post' })
}

/** 会员个人信息 */
export function getMemberProfile(): Promise<MemberProfile> {
  return request({ url: '/member/auth/get-profile', method: 'get' })
}
