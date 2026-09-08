/**
 * 会员登录态管理(localStorage)
 */
import type { MemberProfile } from '@/types'

const TOKEN_KEY = 'tender_member_token'
const PROFILE_KEY = 'tender_member_profile'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getProfile(): MemberProfile | null {
  const raw = localStorage.getItem(PROFILE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function setProfile(profile: MemberProfile) {
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(PROFILE_KEY)
}

export function isLoggedIn(): boolean {
  return !!getToken()
}
