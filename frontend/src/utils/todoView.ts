import type { TodoFiltersModel } from '../components/todo/types'

export type TodoViewMode = 'ACTIVE' | 'RECYCLE_BIN'
export type TodoDisplayMode = 'LIST' | 'KANBAN'

type TodoUrlState = {
  filters: TodoFiltersModel
  viewMode: TodoViewMode
  displayMode: TodoDisplayMode
}

const SYNCED_FILTER_KEYS: Array<keyof TodoFiltersModel> = [
  'page',
  'status',
  'priority',
  'category',
  'keyword',
  'tag',
  'recurrenceType',
  'timePreset',
  'dueDateFrom',
  'dueDateTo',
  'remindDateFrom',
  'remindDateTo',
  'sortBy',
  'sortDir',
]

/**
 * 返回 Todo 列表的默认筛选值。
 */
export function createDefaultTodoFilters(): TodoFiltersModel {
  return {
    page: 0,
    size: 10,
    status: '',
    priority: '',
    category: '',
    keyword: '',
    tag: '',
    recurrenceType: '',
    timePreset: '',
    dueDateFrom: '',
    dueDateTo: '',
    remindDateFrom: '',
    remindDateTo: '',
    sortBy: 'createTime',
    sortDir: 'DESC',
  }
}

/**
 * 将 Todo 页面状态序列化为 URL 查询字符串。
 */
export function serializeTodoUrlState(state: TodoUrlState): string {
  const params = new URLSearchParams()
  const defaultFilters = createDefaultTodoFilters()

  SYNCED_FILTER_KEYS.forEach((key) => {
    const value = String(state.filters[key] ?? '')
    const defaultValue = String(defaultFilters[key] ?? '')
    if (value && value !== defaultValue) {
      params.set(key, value)
    }
  })

  if (state.viewMode !== 'ACTIVE') {
    params.set('viewMode', state.viewMode)
  }

  if (state.displayMode !== 'LIST' && state.viewMode === 'ACTIVE') {
    params.set('displayMode', state.displayMode)
  }

  return params.toString()
}

/**
 * 将 URL 查询字符串反序列化为 Todo 页面状态。
 */
export function parseTodoUrlState(search: string): TodoUrlState {
  const params = new URLSearchParams(search.startsWith('?') ? search.slice(1) : search)
  const filters = createDefaultTodoFilters()

  SYNCED_FILTER_KEYS.forEach((key) => {
    const value = params.get(key)
    if (!value) {
      return
    }

    if (key === 'page') {
      const parsedPage = Number.parseInt(value, 10)
      filters.page = Number.isFinite(parsedPage) && parsedPage >= 0 ? parsedPage : 0
      return
    }

    filters[key] = value as never
  })

  const viewMode = params.get('viewMode') === 'RECYCLE_BIN' ? 'RECYCLE_BIN' : 'ACTIVE'
  const displayMode = params.get('displayMode') === 'KANBAN' && viewMode === 'ACTIVE' ? 'KANBAN' : 'LIST'

  return { filters, viewMode, displayMode }
}

/**
 * 判断当前 URL 是否包含可恢复的 Todo 状态查询参数。
 */
export function hasMeaningfulTodoQuery(search: string): boolean {
  const params = new URLSearchParams(search.startsWith('?') ? search.slice(1) : search)
  return SYNCED_FILTER_KEYS.some((key) => params.has(key)) || params.has('viewMode') || params.has('displayMode')
}

/**
 * 校验提醒日期不能晚于截止日期。
 */
export function isReminderAfterDueDate(remindAt?: string | null, dueDate?: string | null): boolean {
  if (!remindAt || !dueDate) {
    return false
  }

  return remindAt > dueDate
}

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
 * 将日期时间渲染为当前语言环境下的短日期时间文本。
 */
export function formatDateTimeLabel(value?: string, locale = 'en'): string {
  if (!value) {
    return ''
  }

  return new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
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
