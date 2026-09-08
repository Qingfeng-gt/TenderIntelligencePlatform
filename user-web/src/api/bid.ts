import request from './app-request'
import type { BidProject, BidStatus } from '@/types'

/** 针对公告发起投标(需登录,身份取 token) */
export function createBidProject(data: {
  noticeId: number
  bidAmount?: number
  bidFileName?: string
  remark?: string
}): Promise<number> {
  return request({
    url: '/bid-project/create',
    method: 'post',
    data
  })
}

/** 当前用户投标项目列表 */
export function listBidProjects(status?: BidStatus | ''): Promise<BidProject[]> {
  return request({
    url: '/bid-project/list',
    method: 'get',
    params: { status: status || undefined }
  })
}

/** 投标项目状态流转 */
export function updateBidStatus(id: number, status: BidStatus): Promise<boolean> {
  return request({
    url: '/bid-project/status',
    method: 'post',
    data: { id, status }
  })
}
