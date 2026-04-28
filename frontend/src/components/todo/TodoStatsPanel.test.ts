import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'

import TodoStatsPanel from './TodoStatsPanel.vue'
import en from '../../locales/en'
import zhCN from '../../locales/zh-CN'

function mountWithLocale(locale: 'en' | 'zh-CN' = 'en', pageMode = false) {
  return mount(TodoStatsPanel, {
    props: {
      overview: {
        todayCompleted: 2,
        weekCompleted: 7,
        overdueCount: 3,
        activeCount: 11,
        upcomingReminderCount: 5,
        unreadReminderCount: 4,
      },
      categories: [
        { category: 'Work', activeCount: 4, completedCount: 2 },
        { category: '__UNCLASSIFIED__', activeCount: 3, completedCount: 1 },
      ],
      trend: [
        { date: '2026-04-01', createdCount: 2, completedCount: 1 },
        { date: '2026-04-02', createdCount: 1, completedCount: 2 },
      ],
      trendSummary: {
        totalCreated: 3,
        totalCompleted: 3,
        completionRate: 1.0,
        netChange: 0,
      },
      dueBuckets: {
        overdue: 1,
        dueToday: 2,
        dueIn3Days: 3,
        dueIn7Days: 4,
        noDueDate: 1,
        totalActive: 11,
      },
      priorityDistribution: {
        items: [
          { priority: 5, count: 2 },
          { priority: 4, count: 3 },
          { priority: 3, count: 1 },
          { priority: 1, count: 5 },
        ],
        totalActive: 11,
      },
      aging: {
        buckets: [
          { label: '0-3 days', count: 5 },
          { label: '4-7 days', count: 3 },
          { label: '8-14 days', count: 2 }
        ],
        totalPending: 10
      },
      reminderSummary: {
        unreadCount: 4,
        readTodayCount: 2,
        scheduledCount: 10,
        overdueReminderCount: 1
      },
      recurrenceDistribution: {
        items: [
          { recurrenceType: 'DAILY', count: 3 },
          { recurrenceType: 'WEEKLY', count: 2 }
        ],
        totalActive: 5
      },
      pageMode,
    },
    global: {
      plugins: [createI18n({
        legacy: false,
        locale,
        messages: {
          en,
          'zh-CN': zhCN,
        },
      })],
    },
  })
}

describe('TodoStatsPanel', () => {
  it('renders overview cards and localized uncategorized label', () => {
    const wrapper = mountWithLocale('en')

    expect(wrapper.text()).toContain('Today Completed')
    expect(wrapper.text()).toContain('Week Completed')
    expect(wrapper.text()).toContain('Overdue Tasks')
    expect(wrapper.text()).toContain('Active Tasks')
    expect(wrapper.text()).toContain('Unread Reminders')
    expect(wrapper.text()).toContain('Uncategorized')
    expect(wrapper.text()).toContain('Active: 3')
    expect(wrapper.text()).toContain('Completed: 1')
  })

  it('renders chinese locale labels', () => {
    const wrapper = mountWithLocale('zh-CN')

    expect(wrapper.text()).toContain('今日完成')
    expect(wrapper.text()).toContain('本周完成')
    expect(wrapper.text()).toContain('未读提醒')
    expect(wrapper.text()).toContain('未分类')
    expect(wrapper.text()).toContain('活动：3')
    expect(wrapper.text()).toContain('完成：1')
  })

  it('renders page-mode dashboard sections with stable hooks', () => {
    const wrapper = mountWithLocale('en', true)

    expect(wrapper.find('[data-testid="page-stats-dashboard"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="stats-kpi-grid"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-testid="stats-kpi-card"]')).toHaveLength(6)
    expect(wrapper.find('[data-testid="stats-trend-section"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-testid="stats-trend-bar"]')).toHaveLength(2)
    expect(wrapper.find('[data-testid="stats-trend-snapshot"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="stats-categories-section"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="stats-due-section"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="stats-priority-section"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="stats-aging-section"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="stats-reminder-section"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="stats-recurrence-section"]').exists()).toBe(true)
    
    // Check trend snapshot specific text for English
    expect(wrapper.text()).toContain('Created in shown trend')
    expect(wrapper.text()).toContain('Net change')
    expect(wrapper.text()).toContain('Priority Distribution')
    expect(wrapper.text()).toContain('Aging Distribution')
    expect(wrapper.text()).toContain('Due Timeline')
    expect(wrapper.text()).toContain('Reminder Summary')
    expect(wrapper.text()).toContain('Recurrence Distribution')
  })

  it('sorts category rows deterministically in page mode', () => {
    const wrapper = mountWithLocale('en', true)
    const rows = wrapper.findAll('[data-testid="stats-category-row"]')

    expect(rows[0]?.attributes('data-category-key')).toBe('Work')
    expect(rows[1]?.attributes('data-category-key')).toBe('__UNCLASSIFIED__')
  })
})
