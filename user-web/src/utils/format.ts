/**
 * 时间格式化工具
 *
 * 后端全局 Jackson 配置 write-dates-as-timestamps: true,
 * LocalDateTime 字段返回 epoch 毫秒,同时兼容 ISO 字符串("2026-09-08T14:00:00")。
 */
export type TimeLike = number | string | null | undefined

const pad = (n: number) => String(n).padStart(2, '0')

export function parseTime(t: TimeLike): Date | null {
  if (t === null || t === undefined || t === '') return null
  if (typeof t === 'number') return new Date(t)
  const d = new Date(t)
  return isNaN(d.getTime()) ? null : d
}

/** 完整时间: 2026-09-08 14:00 */
export function formatDateTime(t: TimeLike): string {
  const d = parseTime(t)
  if (!d) return '—'
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 简短时间: 09-08 14:00 */
export function formatDateTimeShort(t: TimeLike): string {
  const d = parseTime(t)
  if (!d) return '—'
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
