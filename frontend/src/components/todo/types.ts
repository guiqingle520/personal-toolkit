export type TodoStatus = 'PENDING' | 'DONE'

export interface TodoItem {
  id: number
  title: string
  status: TodoStatus | string
  priority?: number
  dueDate?: string
  remindAt?: string
  category?: string
  tags?: string
  notes?: string
  attachmentLinks?: string
  ownerLabel?: string
  collaborators?: string
  watchers?: string
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
  recurrenceType: string
  timePreset: string
  dueDateFrom: string
  dueDateTo: string
  remindDateFrom: string
  remindDateTo: string
  sortBy: string
  sortDir: string
}

export interface TodoDraft {
  title: string
  priority: number
  category: string
  dueDate: string
  remindAt: string
  tags: string
  notes: string
  attachmentLinks: string
  ownerLabel: string
  collaborators: string
  watchers: string
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
  upcomingReminderCount: number
  unreadReminderCount: number
}

export interface TodoReminderItem {
  id: number
  todoId: number
  todoTitle: string
  todoStatus?: string
  category?: string
  dueDate?: string
  scheduledAt: string
  status: string
  sentAt?: string
  readAt?: string
}

export interface TodoSavedView {
  id: number
  name: string
  isDefault: boolean
  filters: Partial<Record<keyof TodoFiltersModel, string>>
  createTime: string
  updateTime: string
}

export interface TodoStatsCategoryItem {
  category: string
  activeCount: number
  completedCount: number
}

export interface TodoStatsTrendItem {
  date: string
  createdCount: number
  completedCount: number
}

export interface TodoStatsTrendSummary {
  totalCreated: number
  totalCompleted: number
  completionRate: number
  netChange: number
}

export interface TodoStatsTrend {
  range: string
  items: TodoStatsTrendItem[]
  summary: TodoStatsTrendSummary
}

export interface TodoStatsDueBuckets {
  overdue: number
  dueToday: number
  dueIn3Days: number
  dueIn7Days: number
  noDueDate: number
  totalActive: number
}

export interface TodoStatsPriorityItem {
  priority: number
  count: number
}

export interface TodoStatsPriorityDistribution {
  items: TodoStatsPriorityItem[]
  totalActive: number
}

export interface TodoStatsAgingBucket {
  label: string
  count: number
}

export interface TodoStatsAging {
  buckets: TodoStatsAgingBucket[]
  totalPending: number
}

export interface TodoReminderSummary {
  unreadCount: number
  readTodayCount: number
  scheduledCount: number
  overdueReminderCount: number
}

export interface TodoStatsRecurrenceItem {
  recurrenceType: string
  count: number
}

export interface TodoStatsRecurrenceDistribution {
  items: TodoStatsRecurrenceItem[]
  totalActive: number
}
