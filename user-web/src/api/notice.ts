import request from './request'
import type { Notice, NoticeQuery, PageResult, HomeStat, HotTag, RegionOption } from '@/types'

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

/** 分页查询公告(四层模型) */
export function listNotices(query: NoticeQuery): Promise<PageResult<Notice>> {
  return request({
    url: '/notice/page',
    method: 'get',
    params: {
      pageNo: query.page ?? 1,
      pageSize: query.pageSize ?? 10,
      keyword: query.keyword || undefined,
      noticeTypeCode: query.noticeTypeCode || undefined,
      provinceCode: query.provinceCode || undefined,
      cityCode: query.cityCode || undefined,
      industry: query.industry || undefined,
      publishTimeGreaterThan: toTimeParam(query.timeRange)
    }
  })
}

/** 公告详情(含附件、澄清修改、关联企业) */
export function getNoticeDetail(id: number): Promise<Notice> {
  return request({ url: '/notice/detail', method: 'get', params: { id } })
}

/** 首页统计 */
export function getHomeStat(): Promise<HomeStat> {
  return request({ url: '/notice/overview', method: 'get' })
}

/** 省级行政区划(来自 base_area) */
export function getProvinces(): Promise<RegionOption[]> {
  return request({ url: '/notice/provinces', method: 'get' })
}

/** 行业选项(来自已有公告的规则抽取结果) */
export function getIndustries(): Promise<string[]> {
  return request({ url: '/notice/industries', method: 'get' })
}

export { hotTags }
