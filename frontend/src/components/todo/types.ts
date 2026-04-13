export type TodoStatus = 'PENDING' | 'DONE'

export interface TodoItem {
  id: number
  title: string
  status: TodoStatus | string
  priority?: number
  dueDate?: string
  category?: string
  tags?: string
  createTime: string
  updateTime: string
  subItemSummary?: TodoSubItemSummary
  recurrenceType?: string
  recurrenceInterval?: number
  recurrenceEndTime?: string
  nextTriggerTime?: string
  completedAt?: string
}

export interface PageData<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  first: boolean
  last: boolean
}

export interface TodoFiltersModel {
  page: number
  size: number
  status: string
  priority: string
  category: string
  keyword: string
  tag: string
  dueDateFrom: string
  dueDateTo: string
  sortBy: string
  sortDir: string
}

export interface TodoDraft {
  title: string
  priority: number
  category: string
  dueDate: string
  tags: string
  recurrenceType?: string
  recurrenceInterval?: number
  recurrenceEndTime?: string
}

export interface TodoOptions {
  categories: string[]
  tags: string[]
}

export interface TodoSubItem {
  id: number
  todoId: number
  title: string
  status: TodoStatus | string
  sortOrder: number
  createTime: string
  updateTime: string
}

export interface TodoSubItemSummary {
  totalCount: number
  completedCount: number
  progressPercent: number
}

export interface TodoSubItemDraft {
  title: string
}

export interface TodoStatsOverview {
  todayCompleted: number
  weekCompleted: number
  overdueCount: number
  activeCount: number
}

export interface TodoStatsCategoryItem {
  category: string
  activeCount: number
  completedCount: number
}

export interface TodoStatsTrendItem {
  date: string
  completedCount: number
}

export interface TodoStatsTrend {
  range: string
  items: TodoStatsTrendItem[]
}
