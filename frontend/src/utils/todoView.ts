/**
 * 将日期输入框值转换为后端可接受的日期时间字符串。
 */
export function toDateTimeValue(value: string): string | null {
  if (!value) {
    return null
  }
  return `${value}T00:00:00`
}

/**
 * 将后端返回的日期时间裁剪为日期输入框值。
 */
export function formatDateForInput(value?: string): string {
  if (!value) {
    return ''
  }
  return value.slice(0, 10)
}

/**
 * 将逗号分隔的标签字符串解析为标签数组。
 */
export function parseTags(tags?: string): string[] {
  if (!tags) {
    return []
  }

  return tags
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
}

export type PriorityLabelKey =
  | 'priority.na'
  | 'priority.critical'
  | 'priority.high'
  | 'priority.medium'
  | 'priority.low'
  | 'priority.backlog'

export type RecurrenceLabelKey =
  | 'recurrence.none'
  | 'recurrence.daily'
  | 'recurrence.weekly'
  | 'recurrence.monthly'

/**
 * 根据数值优先级生成用户可读标签。
 */
export function formatPriorityLabel(priority?: number): PriorityLabelKey {
  if (priority === undefined || priority === null) {
    return 'priority.na'
  }

  if (priority >= 5) return 'priority.critical'
  if (priority === 4) return 'priority.high'
  if (priority === 3) return 'priority.medium'
  if (priority === 2) return 'priority.low'
  return 'priority.backlog'
}

/**
 * 根据数值优先级返回对应的徽标样式类名。
 */
export function priorityBadgeClass(priority?: number): string {
  if (priority === undefined || priority === null) {
    return 'badge-neutral'
  }

  if (priority >= 4) return 'badge-high'
  if (priority === 3) return 'badge-medium'
  return 'badge-low'
}

/**
 * 将前后端混用的重复类型值映射为稳定的国际化 key。
 */
export function formatRecurrenceLabelKey(recurrenceType?: string): RecurrenceLabelKey {
  const normalizedRecurrenceType = recurrenceType?.trim().toUpperCase()

  if (normalizedRecurrenceType === 'DAILY') return 'recurrence.daily'
  if (normalizedRecurrenceType === 'WEEKLY') return 'recurrence.weekly'
  if (normalizedRecurrenceType === 'MONTHLY') return 'recurrence.monthly'
  return 'recurrence.none'
}
