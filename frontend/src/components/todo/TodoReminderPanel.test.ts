import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'

import TodoReminderPanel from './TodoReminderPanel.vue'
import en from '../../locales/en'
import zhCN from '../../locales/zh-CN'

function mountPanel() {
  return mount(TodoReminderPanel, {
    props: {
      reminders: [
        {
          id: 1,
          todoId: 9,
          todoTitle: 'Review logs',
          scheduledAt: '2026-04-22T20:30:00',
          status: 'SENT',
          category: 'Work',
        },
      ],
      loading: false,
    },
    global: {
      plugins: [createI18n({
        legacy: false,
        locale: 'en',
        messages: { en, 'zh-CN': zhCN },
      })],
    },
  })
}

describe('TodoReminderPanel', () => {
  it('renders reminder items', () => {
    const wrapper = mountPanel()

    expect(wrapper.text()).toContain('Review logs')
    expect(wrapper.text()).toContain('Reminder time')
  })

  it('emits read events', async () => {
    const wrapper = mountPanel()

    await wrapper.findAll('button')[1].trigger('click')
    expect(wrapper.emitted('open-todo')?.[0]).toEqual([9])

    await wrapper.findAll('button')[2].trigger('click')
    expect(wrapper.emitted('mark-read')?.[0]).toEqual([1])
  })
})
