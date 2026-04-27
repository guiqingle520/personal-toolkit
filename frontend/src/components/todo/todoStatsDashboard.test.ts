import { describe, expect, it } from 'vitest'

import {
  buildDashboardCategories,
  buildDashboardKpis,
  buildDashboardSnapshot,
  buildDashboardTrend,
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
      { date: '2026-04-03', completedCount: 1 },
      { date: '2026-04-01', completedCount: 2 },
      { date: '2026-04-02', completedCount: 0 },
    ], (date) => date.slice(5))

    expect(trend.map((item) => item.date)).toEqual(['2026-04-01', '2026-04-02', '2026-04-03'])
    expect(trend[0]?.isPeak).toBe(true)
    expect(trend[2]?.isPeak).toBe(false)
  })

  it('builds snapshot metrics from shown trend data', () => {
    const snapshot = buildDashboardSnapshot([
      { date: '2026-04-01', label: '04-01', completedCount: 2, isPeak: false },
      { date: '2026-04-02', label: '04-02', completedCount: 0, isPeak: false },
      { date: '2026-04-03', label: '04-03', completedCount: 4, isPeak: true },
    ])

    expect(snapshot).toMatchObject({
      totalCompleted: 6,
      averagePerShownDay: 2,
      activeDays: 2,
      peakCompletedCount: 4,
      peakDate: '04-03',
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
      totalCompleted: 0,
      averagePerShownDay: 0,
      activeDays: 0,
      peakDate: '',
      peakCompletedCount: 0,
    })

    expect(buildDashboardCategories([], (category) => category)).toEqual([])
  })
})
