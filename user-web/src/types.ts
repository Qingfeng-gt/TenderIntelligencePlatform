/**
 * 公告数据结构 —— 与后端四层模型(P0)对齐
 *
 * 读路径: notice 新列 + project / tender_project / tender_lot / enterprise / base_area
 */
export type NoticeTypeCode = 'tender' | 'win' | 'change' | 'explore' | 'qualify'

export interface Notice {
  id: number
  /** 项目 ID */
  projectId?: number
  /** 招标项目 ID */
  tenderProjectId?: number
  /** 标段 ID */
  lotId?: number
  /** 公告类型代码 */
  noticeTypeCode: NoticeTypeCode | string
  /** 公告类型名称 */
  noticeTypeName?: string
  /** 标题 */
  title: string
  /** 公告标识/编号 */
  noticeNo?: string
  /** 项目编号 */
  projectNo?: string
  /** 招标项目编号 */
  tenderProjectNo?: string
  /** 标段编号 */
  lotNo?: string
  /** 统一交易标识码 */
  unifiedDealCode?: string
  /** 项目名称 */
  projectName?: string
  /** 项目预算(万元) */
  projectBudget?: number
  /** 项目公告数 */
  projectNoticeCount?: number
  /** 招标项目名称 */
  tenderProjectName?: string
  /** 采购方式 */
  procurementMethodCode?: string
  procurementMethodName?: string
  /** 省份代码/名称 */
  provinceCode?: string
  provinceName?: string
  /** 城市代码/名称 */
  cityCode?: string
  cityName?: string
  /** 行业(规则抽取结果) */
  industry?: string
  industryCode?: string
  /** 预算金额(万元,0 表示未知) */
  budget?: number
  /** 最高限价(万元) */
  priceCeiling?: number
  /** 保证金(万元) */
  depositAmount?: number
  /** 中标金额(万元) */
  winAmount?: number
  /** 招标人 */
  tendererEnterpriseId?: number
  tendererName?: string
  /** 代理机构 */
  agencyEnterpriseId?: number
  agencyName?: string
  /** 中标人 */
  winnerEnterpriseId?: number
  winnerName?: string
  /** 联系人 */
  contact?: string
  /** 联系电话 */
  contactPhone?: string
  /** 公告原文(HTML,仅详情接口返回) */
  content?: string
  /** 来源站 code */
  sourceSiteCode?: string
  /** 来源平台 */
  sourcePlatform?: string
  /** 源站详情 URL */
  sourceUrl?: string
  /** 发布时间(后端返回 epoch 毫秒,兼容 ISO 字符串) */
  publishTime: number | string
  /** 投标截止时间 */
  bidDeadline?: number | string
  /** 开标时间 */
  openTime?: number | string
  /** 文件获取开始/截止 */
  fileGetStartTime?: number | string
  fileGetEndTime?: number | string
  /** 答疑截止 */
  qaDeadline?: number | string
  /** 报名截止 */
  signDeadline?: number | string
  /** 公示开始/结束 */
  publicityStartTime?: number | string
  publicityEndTime?: number | string
  /** 开标地点 */
  openPlace?: string
  /** 资格要求 */
  qualificationRequirement?: string
  /** 特定资格 */
  specificQualification?: string
  /** 业绩要求 */
  performanceRequirement?: string
  /** 是否接受联合体 */
  consortiumAllowed?: boolean
  /** 资质等级 */
  qualificationLevelCode?: string
  qualificationLevelName?: string
  hasAttachment?: boolean
  viewCount?: number
  status?: string
  dataQualityStatus?: string
  /** 附件列表(仅详情接口返回) */
  attachments?: NoticeAttachment[]
  /** 澄清/修改记录(仅详情接口返回) */
  changes?: NoticeChange[]
  /** 关联企业及角色(仅详情接口返回) */
  enterprises?: NoticeEnterprise[]
}

/** 标讯附件 —— 与后端 NoticeAttachmentRespVO 对齐 */
export interface NoticeAttachment {
  id: number
  /** 附件文件名 */
  fileName: string
  /** 源站附件直链(绝对 URL,点开后到源站下载) */
  fileUrl: string
  /** 文件类型(pdf/doc/docx/xls/xlsx/zip 等),可能为空 */
  fileType?: string
  /** 源站标注的文件大小(源站自由文本,如 546.3K),可能为空 */
  fileSize?: string
  /** 同一公告内的展示顺序 */
  sort?: number
}

/** 公告澄清/修改记录 */
export interface NoticeChange {
  id: number
  changeTypeCode?: string
  changeTitle?: string
  changeContent?: string
  changeTime?: number | string
  sourceUrl?: string
}

/** 公告关联企业 */
export interface NoticeEnterprise {
  enterpriseId: number
  enterpriseName?: string
  roleCode: string
  roleName?: string
  sourceText?: string
  confidence?: number
  status?: string
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
  noticeTypeCode?: string
  keyword?: string
  provinceCode?: string
  cityCode?: string
  industry?: string
  timeRange?: 'today' | 'week' | 'month' | ''
  page?: number
  pageSize?: number
}

export interface PageResult<T> {
  list: T[]
  total: number
}

/** 首页统计 —— 与后端 /notice/overview 对齐 */
export interface HomeStat {
  todayCount: number
  totalCount: number
  provinces: number
  industries: number
  projects: number
}

/** 行政区划选项 */
export interface RegionOption {
  code: string
  parentCode?: string
  level: number
  name: string
  shortName?: string
  fullName?: string
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
