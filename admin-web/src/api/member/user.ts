import request from '@/config/axios'

export interface MemberUserVO {
  id: number
  username: string
  nickname: string
  mobile: string
  avatar: string
  status: number
  loginIp: string
  loginDate: string | Date
  createTime: string | Date
}

export interface MemberUserPageReqVO {
  pageNo: number
  pageSize: number
  username?: string
  nickname?: string
  mobile?: string
  status?: number
}

/** 分页查询会员用户 */
export const getMemberUserPage = (params: MemberUserPageReqVO) => {
  return request.get({ url: '/member/user/page', params })
}

/** 查询会员用户详情 */
export const getMemberUser = (id: number) => {
  return request.get<MemberUserVO>({ url: '/member/user/get', params: { id } })
}

/** 修改会员状态 */
export const updateMemberUserStatus = (id: number, status: number) => {
  return request.put({ url: '/member/user/update-status', data: { id, status } })
}

/** 重置会员密码 */
export const resetMemberUserPassword = (id: number, password: string) => {
  return request.put({ url: '/member/user/reset-password', data: { id, password } })
}
