import { describe, expect, it } from 'vitest'

import {
  buildDashboardCategories,
  buildDashboardKpis,
  buildDashboardSnapshot,
  buildDashboardTrend,
  buildDashboardDueBuckets,
  buildDashboardPriorities,
  buildDashboardAging,
  buildDashboardReminderSummary,
  buildDashboardRecurrence,
} from './todoStatsDashboard'

describe('todoStatsDashboard', () => {
  it('builds KPI items from overview data', () => {
    const kpis = buildDashboardKpis({
      todayCompleted: 2,
      weekCompleted: 7,
      overdueCount: 3,
      activeCount: 11,
      upcomingReminderCount: 5,
      unreadReminderCount: 4,
    })

    expect(kpis).toHaveLength(6)
    expect(kpis[0]).toMatchObject({ key: 'todayCompleted', value: 2 })
    expect(kpis[5]).toMatchObject({ key: 'unreadReminderCount', value: 4 })
  })

  it('sorts trend data chronologically and marks peak day', () => {
    const trend = buildDashboardTrend([
      { date: '2026-04-03', createdCount: 2, completedCount: 1 },
      { date: '2026-04-01', createdCount: 3, completedCount: 2 },
      { date: '2026-04-02', createdCount: 1, completedCount: 0 },
    ], (date) => date.slice(5))

    expect(trend.map((item) => item.date)).toEqual(['2026-04-01', '2026-04-02', '2026-04-03'])
    expect(trend[0]?.isPeak).toBe(true)
    expect(trend[2]?.isPeak).toBe(false)
  })

  it('builds snapshot metrics from shown trend data', () => {
    const snapshot = buildDashboardSnapshot([
      { date: '2026-04-01', label: '04-01', createdCount: 3, completedCount: 2, isPeak: false },
      { date: '2026-04-02', label: '04-02', createdCount: 1, completedCount: 0, isPeak: false },
      { date: '2026-04-03', label: '04-03', createdCount: 2, completedCount: 4, isPeak: true },
    ])

    expect(snapshot).toMatchObject({
      totalCreated: 6,
      totalCompleted: 6,
      netChange: 0,
      completionRate: 100,
      averagePerShownDay: 2,
      activeDays: 2,
      peakCompletedCount: 4,
      peakDate: '04-03',
    })
  })

  it('builds snapshot metrics using trend summary when provided', () => {
    const snapshot = buildDashboardSnapshot([
      { date: '2026-04-01', label: '04-01', createdCount: 1, completedCount: 1, isPeak: false },
    ], {
      totalCreated: 10,
      totalCompleted: 5,
      netChange: 5,
      completionRate: 0.5,
    })

    expect(snapshot).toMatchObject({
      totalCreated: 10,
      totalCompleted: 5,
      netChange: 5,
      completionRate: 50,
    })
  })

  it('builds sorted category rankings with share and completion rates', () => {
    const categories = buildDashboardCategories([
      { category: '__UNCLASSIFIED__', activeCount: 1, completedCount: 1 },
      { category: 'Work', activeCount: 4, completedCount: 2 },
      { category: 'Ops', activeCount: 4, completedCount: 2 },
    ], (category) => category === '__UNCLASSIFIED__' ? 'Uncategorized' : category)

    expect(categories[0]).toMatchObject({ categoryKey: 'Ops', totalCount: 6, completionRate: 33 })
    expect(categories[1]).toMatchObject({ categoryKey: 'Work', totalCount: 6, completionRate: 33 })
    expect(categories[2]).toMatchObject({ categoryKey: '__UNCLASSIFIED__', shareOfTotal: 14 })
  })

  it('handles empty trend and category collections safely', () => {
    expect(buildDashboardSnapshot([])).toMatchObject({
      totalCreated: 0,
      totalCompleted: 0,
      netChange: 0,
      completionRate: 0,
      averagePerShownDay: 0,
      activeDays: 0,
      peakDate: '',
      peakCompletedCount: 0,
    })

    expect(buildDashboardCategories([], (category) => category)).toEqual([])
  })

  it('builds dashboard due buckets correctly', () => {
    const buckets = buildDashboardDueBuckets({
      overdue: 2,
      dueToday: 1,
      dueIn3Days: 3,
      dueIn7Days: 4,
      noDueDate: 0,
      totalActive: 10,
    })

    expect(buckets).toHaveLength(5)
    expect(buckets[0]).toMatchObject({ key: 'bucketOverdue', count: 2, percentage: 20 })
  })

  it('builds dashboard priority distribution correctly', () => {
    const distribution = buildDashboardPriorities({
      items: [
        { priority: 5, count: 2 },
        { priority: 0, count: 8 },
        { priority: 10, count: 1 },
      ],
      totalActive: 11,
    })

    expect(distribution).toHaveLength(5)
    expect(distribution[0]).toMatchObject({ priority: 5, labelKey: 'priority.critical', count: 3, percentage: 27 })
    expect(distribution[4]).toMatchObject({ priority: 1, labelKey: 'priority.backlog', count: 8, percentage: 73 })
  })

  it('builds dashboard aging buckets correctly', () => {
    const aging = buildDashboardAging({
      buckets: [
        { label: '0-3 days', count: 5 },
        { label: '4-7 days', count: 3 },
        { label: '8-14 days', count: 1 },
        { label: '15+ days', count: 1 }
      ],
      totalPending: 10
    })

    expect(aging).toHaveLength(4)
    expect(aging[0]).toMatchObject({ label: '0-3 days', count: 5, percentage: 50, toneClass: 'text-primary' })
    expect(aging[1]).toMatchObject({ label: '4-7 days', count: 3, percentage: 30, toneClass: 'text-info' })
    expect(aging[3]).toMatchObject({ label: '15+ days', count: 1, percentage: 10, toneClass: 'text-warning' })
  })

  it('builds reminder summary items correctly', () => {
    const summary = buildDashboardReminderSummary({
      unreadCount: 4,
      readTodayCount: 2,
      scheduledCount: 10,
      overdueReminderCount: 1
    })

    expect(summary).toHaveLength(4)
    expect(summary[0]).toMatchObject({ key: 'reminderUnread', count: 4, toneClass: 'text-warning' })
    expect(summary[1]).toMatchObject({ key: 'reminderReadToday', count: 2, toneClass: 'text-success' })
  })

  it('builds recurrence distribution correctly', () => {
    const recurrence = buildDashboardRecurrence({
      items: [
        { recurrenceType: 'DAILY', count: 3 },
        { recurrenceType: 'WEEKLY', count: 2 }
      ],
      totalActive: 5
    })

    expect(recurrence).toHaveLength(2)
    expect(recurrence[0]).toMatchObject({ recurrenceType: 'DAILY', labelKey: 'recurrence.daily', count: 3, percentage: 60 })
    expect(recurrence[1]).toMatchObject({ recurrenceType: 'WEEKLY', labelKey: 'recurrence.weekly', count: 2, percentage: 40 })
  })
})
