import type { TodoStatsCategoryItem, TodoStatsOverview, TodoStatsTrendItem } from './types'

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
  completedCount: number
  label: string
  isPeak: boolean
}

export type DashboardSnapshot = {
  totalCompleted: number
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
    completedCount: item.completedCount,
    label: formatLabel(item.date),
    isPeak: peakCompletedCount > 0 && item.completedCount === peakCompletedCount,
  }))
}

export function buildDashboardSnapshot(trend: DashboardTrendItem[]): DashboardSnapshot {
  const totalCompleted = trend.reduce((sum, item) => sum + item.completedCount, 0)
  const activeDays = trend.filter((item) => item.completedCount > 0).length
  const peakItem = trend.reduce<DashboardTrendItem | null>((currentPeak, item) => {
    if (!currentPeak || item.completedCount >= currentPeak.completedCount) {
      return item
    }

    return currentPeak
  }, null)

  return {
    totalCompleted,
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
