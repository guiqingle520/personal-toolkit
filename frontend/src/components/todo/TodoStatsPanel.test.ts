import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'

import TodoStatsPanel from './TodoStatsPanel.vue'
import en from '../../locales/en'
import zhCN from '../../locales/zh-CN'

function mountWithLocale(locale: 'en' | 'zh-CN' = 'en') {
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
        { date: '2026-04-01', completedCount: 1 },
        { date: '2026-04-02', completedCount: 2 },
      ],
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
})
