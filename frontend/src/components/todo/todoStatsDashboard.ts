import type { 
  TodoStatsCategoryItem, 
  TodoStatsOverview, 
  TodoStatsTrendItem,
  TodoStatsTrendSummary,
  TodoStatsDueBuckets,
  TodoStatsPriorityDistribution,
  TodoStatsAging,
  TodoReminderSummary,
  TodoStatsRecurrenceDistribution
} from './types'

export type DashboardKpiKey =
  | 'todayCompleted'
  | 'weekCompleted'
  | 'overdueCount'
  | 'activeCount'
  | 'upcomingReminderCount'
  | 'unreadReminderCount'

export type DashboardKpiItem = {
  key: DashboardKpiKey
  value: number
  toneClass: string
  icon: string
}

export type DashboardTrendItem = {
  date: string
  createdCount: number
  completedCount: number
  label: string
  isPeak: boolean
}

export type DashboardSnapshot = {
  totalCreated: number
  totalCompleted: number
  netChange: number
  completionRate: number
  averagePerShownDay: number
  activeDays: number
  peakDate: string
  peakCompletedCount: number
}

export type DashboardCategoryItem = {
  categoryKey: string
  displayName: string
  activeCount: number
  completedCount: number
  totalCount: number
  completionRate: number
  shareOfTotal: number
}

export type DashboardDueBucketItem = {
  key: string
  count: number
  percentage: number
  toneClass: string
}

export type DashboardPriorityItem = {
  priority: number
  labelKey: string
  count: number
  percentage: number
  toneClass: string
}

export type DashboardAgingItem = {
  label: string
  count: number
  percentage: number
  toneClass: string
}

export type DashboardReminderSummaryItem = {
  key: string
  count: number
  toneClass: string
}

export type DashboardRecurrenceItem = {
  recurrenceType: string
  labelKey: string
  count: number
  percentage: number
}

export function buildDashboardKpis(overview: TodoStatsOverview): DashboardKpiItem[] {
  return [
    { key: 'todayCompleted', value: overview.todayCompleted, toneClass: 'text-success', icon: '✓' },
    { key: 'weekCompleted', value: overview.weekCompleted, toneClass: 'text-success', icon: '📅' },
    { key: 'overdueCount', value: overview.overdueCount, toneClass: 'text-warning', icon: '⚠️' },
    { key: 'activeCount', value: overview.activeCount, toneClass: 'text-primary', icon: '⚡' },
    { key: 'upcomingReminderCount', value: overview.upcomingReminderCount, toneClass: 'text-info', icon: '🔔' },
    { key: 'unreadReminderCount', value: overview.unreadReminderCount, toneClass: 'text-info', icon: '📫' },
  ]
}

export function buildDashboardTrend(
  trend: TodoStatsTrendItem[],
  formatLabel: (date: string) => string,
): DashboardTrendItem[] {
  const sortedTrend = [...trend].sort((left, right) => left.date.localeCompare(right.date))
  const peakCompletedCount = sortedTrend.reduce((max, item) => Math.max(max, item.completedCount), 0)

  return sortedTrend.map((item) => ({
    date: item.date,
    createdCount: item.createdCount ?? 0,
    completedCount: item.completedCount,
    label: formatLabel(item.date),
    isPeak: peakCompletedCount > 0 && item.completedCount === peakCompletedCount,
  }))
}

export function buildDashboardSnapshot(
  trend: DashboardTrendItem[],
  summary?: TodoStatsTrendSummary
): DashboardSnapshot {
  const totalCompleted = summary ? summary.totalCompleted : trend.reduce((sum, item) => sum + item.completedCount, 0)
  const totalCreated = summary ? summary.totalCreated : trend.reduce((sum, item) => sum + item.createdCount, 0)
  const netChange = summary ? summary.netChange : totalCreated - totalCompleted
  const completionRate = summary ? Math.round(summary.completionRate * 100) : (totalCreated > 0 ? Math.round((totalCompleted / totalCreated) * 100) : 0)

  const activeDays = trend.filter((item) => item.completedCount > 0).length
  const peakItem = trend.reduce<DashboardTrendItem | null>((currentPeak, item) => {
    if (!currentPeak || item.completedCount >= currentPeak.completedCount) {
      return item
    }

    return currentPeak
  }, null)

  return {
    totalCreated,
    totalCompleted,
    netChange,
    completionRate,
    averagePerShownDay: trend.length ? Number((totalCompleted / trend.length).toFixed(1)) : 0,
    activeDays,
    peakDate: peakItem?.label || '',
    peakCompletedCount: peakItem?.completedCount || 0,
  }
}

export function buildDashboardCategories(
  categories: TodoStatsCategoryItem[],
  getDisplayName: (category: string) => string,
): DashboardCategoryItem[] {
  const normalizedCategories = categories.map((item) => {
    const totalCount = item.activeCount + item.completedCount

    return {
      categoryKey: item.category,
      displayName: getDisplayName(item.category),
      activeCount: item.activeCount,
      completedCount: item.completedCount,
      totalCount,
      completionRate: totalCount ? Math.round((item.completedCount / totalCount) * 100) : 0,
    }
  })

  const totalTrackedCount = normalizedCategories.reduce((sum, item) => sum + item.totalCount, 0)

  return normalizedCategories
    .map((item) => ({
      ...item,
      shareOfTotal: totalTrackedCount ? Math.round((item.totalCount / totalTrackedCount) * 100) : 0,
    }))
    .sort((left, right) => {
      if (right.totalCount !== left.totalCount) {
        return right.totalCount - left.totalCount
      }
      if (right.activeCount !== left.activeCount) {
        return right.activeCount - left.activeCount
      }

      return left.displayName.localeCompare(right.displayName)
    })
}

export function buildDashboardDueBuckets(buckets: TodoStatsDueBuckets): DashboardDueBucketItem[] {
  const total = buckets.totalActive || 1
  return [
    { key: 'bucketOverdue', count: buckets.overdue, percentage: Math.round((buckets.overdue / total) * 100), toneClass: 'text-warning' },
    { key: 'bucketToday', count: buckets.dueToday, percentage: Math.round((buckets.dueToday / total) * 100), toneClass: 'text-primary' },
    { key: 'bucket3Days', count: buckets.dueIn3Days, percentage: Math.round((buckets.dueIn3Days / total) * 100), toneClass: 'text-info' },
    { key: 'bucket7Days', count: buckets.dueIn7Days, percentage: Math.round((buckets.dueIn7Days / total) * 100), toneClass: 'text-info' },
    { key: 'bucketNoDate', count: buckets.noDueDate, percentage: Math.round((buckets.noDueDate / total) * 100), toneClass: 'text-muted' },
  ]
}

export function buildDashboardPriorities(distribution: TodoStatsPriorityDistribution): DashboardPriorityItem[] {
  const total = distribution.totalActive || 1
  const countMap = new Map(distribution.items.map((item) => [item.priority, item.count]))

  let criticalCount = 0
  let backlogCount = 0
  
  for (const [p, c] of countMap.entries()) {
    if (p === null || p === undefined || p < 2) {
      backlogCount += c
    } else if (p >= 5) {
      criticalCount += c
    }
  }

  const getConfig = (priority: number) => {
    switch (priority) {
      case 5: return { labelKey: 'priority.critical', toneClass: 'text-warning' } // Reusing existing classes
      case 4: return { labelKey: 'priority.high', toneClass: 'text-warning' }
      case 3: return { labelKey: 'priority.medium', toneClass: 'text-primary' }
      case 2: return { labelKey: 'priority.low', toneClass: 'text-info' }
      default: return { labelKey: 'priority.backlog', toneClass: 'text-muted' }
    }
  }

  return [5, 4, 3, 2, 1].map((priority) => {
    const count = priority === 1 ? backlogCount : (priority === 5 ? criticalCount : (countMap.get(priority) || 0))
    const { labelKey, toneClass } = getConfig(priority)
    return {
      priority,
      labelKey,
      count,
      percentage: Math.round((count / total) * 100),
      toneClass,
    }
  })
}

export function buildDashboardAging(aging: TodoStatsAging): DashboardAgingItem[] {
  const total = aging.totalPending || 1
  const buckets = aging.buckets || []
  
  return buckets.map((bucket, index) => {
    let toneClass = 'text-primary'
    if (index >= 3) toneClass = 'text-warning'
    else if (index >= 1) toneClass = 'text-info'
    return {
      label: bucket.label,
      count: bucket.count,
      percentage: Math.round((bucket.count / total) * 100),
      toneClass
    }
  })
}

export function buildDashboardReminderSummary(summary: TodoReminderSummary): DashboardReminderSummaryItem[] {
  return [
    { key: 'reminderUnread', count: summary.unreadCount || 0, toneClass: 'text-warning' },
    { key: 'reminderReadToday', count: summary.readTodayCount || 0, toneClass: 'text-success' },
    { key: 'reminderScheduled', count: summary.scheduledCount || 0, toneClass: 'text-info' },
    { key: 'reminderOverdue', count: summary.overdueReminderCount || 0, toneClass: 'text-warning' },
  ]
}

export function buildDashboardRecurrence(distribution: TodoStatsRecurrenceDistribution): DashboardRecurrenceItem[] {
  const total = distribution.totalActive || 1
  const items = distribution.items || []
  return items.map(item => ({
    recurrenceType: item.recurrenceType,
    labelKey: `recurrence.${(item.recurrenceType || 'none').toLowerCase()}`,
    count: item.count,
    percentage: Math.round((item.count / total) * 100),
  }))
}
