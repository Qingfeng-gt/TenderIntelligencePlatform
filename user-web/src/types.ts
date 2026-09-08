/**
 * 公告数据结构 —— 与后端爬虫入库表(tender-module-notice)字段对齐
 */
export interface Notice {
  id: number
  /** 公告类型:tender 招标 / win 中标 / change 变更 / explore 采购 */
  type: 'tender' | 'win' | 'change' | 'explore'
  /** 标题 */
  title: string
  /** 省份 */
  province: string
  /** 城市 */
  city: string
  /** 行业 */
  industry: string
  /** 发布时间(后端返回 epoch 毫秒,兼容 ISO 字符串) */
  publishTime: number | string
  /** 预算金额(万元,0 表示未知) */
  budget?: number
  /** 招标人 / 采购人 */
  tenderPerson?: string
  /** 代理机构 */
  agency?: string
  /** 联系人 */
  contact?: string
  /** 联系电话 */
  contactPhone?: string
  /** 公告原文(HTML) */
  content: string
  /** 发布日期文本 */
  publishDate: number | string
  /** 来源网站 */
  source?: string
  /** 项目编号(爬虫字段) */
  projectNo?: string
  /** 投标截止时间(epoch 毫秒或 ISO 字符串) */
  deadline?: number | string
  /** 开标时间(epoch 毫秒或 ISO 字符串) */
  openTime?: number | string
}

/** 投标项目状态: SUBMITTED已递交 / OTB待开标 / WON中标 / LOST未中标 / ABANDONED放弃 */
export type BidStatus = 'SUBMITTED' | 'OTB' | 'WON' | 'LOST' | 'ABANDONED'

/** 投标项目 —— 与后端 tender-module-bid 对齐 */
export interface BidProject {
  id: number
  userId: number
  noticeId: number
  projectName: string
  deadline?: number | string
  /** 拟投标金额(万元) */
  bidAmount?: number
  bidFileName?: string
  status: BidStatus
  remark?: string
  createTime: number | string
}

export interface NoticeQuery {
  type?: Notice['type'] | ''
  keyword?: string
  province?: string
  city?: string
  industry?: string
  timeRange?: 'today' | 'week' | 'month' | ''
  page?: number
  pageSize?: number
}

export interface PageResult<T> {
  list: T[]
  total: number
}

/** 首页统计 —— 与后端 /notice/stats 对齐 */
export interface HomeStat {
  todayCount: number
  totalCount: number
  provinces: number
  industries: number
}

export interface HotTag {
  name: string
  count: number
}

/** 会员登录返回 */
export interface MemberTokenResp {
  userId: number
  accessToken: string
  refreshToken: string
  /** epoch 毫秒或 ISO 字符串 */
  expiresTime: number | string
}

/** 会员个人信息 */
export interface MemberProfile {
  id: number
  username: string
  nickname: string
  mobile?: string
  avatar?: string
}
