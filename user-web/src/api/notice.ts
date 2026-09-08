import request from './request'
import type { Notice, NoticeQuery, PageResult, HomeStat, HotTag } from '@/types'

/** 本地热门关键词(后端暂无接口,后续由搜索统计生成) */
const hotTags: HotTag[] = [
  { name: '市政道路', count: 128 },
  { name: '数据中心', count: 96 },
  { name: '光伏', count: 87 },
  { name: '信息化', count: 75 },
  { name: 'EPC', count: 63 },
  { name: '乡村振兴', count: 58 },
  { name: '污水处理', count: 52 },
  { name: '学校', count: 46 }
]

function toTimeParam(r?: '' | 'today' | 'week' | 'month'): string | undefined {
  const now = new Date()
  function fmt(d: Date) {
    const p = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:00`
  }
  if (!r) return undefined
  if (r === 'today') return fmt(new Date(now.getFullYear(), now.getMonth(), now.getDate()))
  if (r === 'week') return fmt(new Date(now.getTime() - 7 * 24 * 3600 * 1000))
  return fmt(new Date(now.getTime() - 30 * 24 * 3600 * 1000))
}

export function listNotices(query: NoticeQuery): Promise<PageResult<Notice>> {
  return request({
    url: '/notice/list',
    method: 'get',
    params: {
      pageNo: query.page ?? 1,
      pageSize: query.pageSize ?? 10,
      keyword: query.keyword || undefined,
      type: query.type || undefined,
      province: query.province || undefined,
      industry: query.industry || undefined,
      publishTimeGreaterThan: toTimeParam(query.timeRange)
    }
  })
}

export function getNoticeDetail(id: number): Promise<Notice> {
  return request({ url: '/notice/get', method: 'get', params: { id } })
}

export function getHomeStat(): Promise<HomeStat> {
  return request({ url: '/notice/stats', method: 'get' })
}

/** 行业列表(静态,后端提供行业字典后可替换) */
const industries = ['市政工程', '轨道交通', '建筑工程', '医疗卫生', '通信工程', '农业水利', '生态环保', '新能源', '智慧城市', '软件服务', '能源化工', '咨询服务', '公共安全', '科研设备', '机关团体']

export function getAllIndustries(): string[] {
  return [...industries]
}

export { hotTags }
