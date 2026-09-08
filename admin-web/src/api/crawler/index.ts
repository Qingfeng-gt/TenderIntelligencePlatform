import request from '@/config/axios'

export interface CrawlerSiteVO {
  id: number
  name: string
  code: string
  enabled: boolean
  channels: string
  intervalMs: number
}

export interface CrawlerTaskVO {
  id: number
  siteCode: string
  startTime: Date
  endTime: Date
  status: string
  listFetched: number
  listParsed: number
  detailFetched: number
  detailFailed: number
  newInsert: number
  updated: number
  errorMsg: string
}

// 站点配置列表
export const getCrawlerSiteList = () => {
  return request.get({ url: '/crawler/site/list' })
}

// 采集任务日志分页
export const getCrawlerTaskPage = (params: PageParam & { siteCode?: string }) => {
  return request.get({ url: '/crawler/task/page', params })
}

// 查询最近一次采集任务
export const getCrawlerLatestTask = (siteCode: string) => {
  return request.get({ url: '/crawler/task/latest', params: { siteCode } })
}

// 手动触发采集(异步执行, 返回任务编号)
export const runCrawler = (siteCode?: string) => {
  return request.post({ url: '/crawler/run', params: { siteCode } })
}
