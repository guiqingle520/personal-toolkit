import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TodoCalendarView from './TodoCalendarView.vue'
import { createI18n } from 'vue-i18n'
import en from '../../locales/en'
import zhCN from '../../locales/zh-CN'

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en,
    zhCN
  }
})

describe('TodoCalendarView', () => {
  const mockTodos = [
    {
      id: 1,
      title: 'Task 1',
      status: 'PENDING',
      dueDate: new Date().toISOString(),
      priority: 2
    },
    {
      id: 2,
      title: 'Task 2',
      status: 'DONE',
      dueDate: new Date().toISOString(),
      priority: 3
    }
  ]

  it('renders calendar board', () => {
    const wrapper = mount(TodoCalendarView, {
      global: {
        plugins: [i18n]
      },
      props: {
        todos: []
      }
    })
    
    expect(wrapper.find('.calendar-board').exists()).toBe(true)
    expect(wrapper.find('.calendar-header').exists()).toBe(true)
    expect(wrapper.find('.calendar-grid').exists()).toBe(true)
  })

  it('displays empty badge when no events exist in current view', () => {
    const wrapper = mount(TodoCalendarView, {
      global: {
        plugins: [i18n]
      },
      props: {
        todos: []
      }
    })
    
    expect(wrapper.find('.empty-month-badge').exists()).toBe(true)
    expect(wrapper.find('.empty-month-badge').text()).toBe('No due dates this month')
    expect(wrapper.find('.calendar-empty-state').exists()).toBe(true)
  })

  it('does not display empty badge when events exist', () => {
    const wrapper = mount(TodoCalendarView, {
      global: {
        plugins: [i18n]
      },
      props: {
        todos: mockTodos
      }
    })
    
    expect(wrapper.find('.empty-month-badge').exists()).toBe(false)
  })

  it('caps visible events to 3 and shows +N more indicator', () => {
    const manyTodos = Array.from({ length: 5 }, (_, i) => ({
      id: i,
      title: `Task ${i}`,
      status: 'PENDING',
      dueDate: new Date().toISOString(),
      priority: 3
    }))

    const wrapper = mount(TodoCalendarView, {
      global: {
        plugins: [i18n]
      },
      props: {
        todos: manyTodos
      }
    })
    
    // The events are in the "today" cell
    const todayCell = wrapper.find('.calendar-day.is-today')
    const events = todayCell.findAll('.calendar-event')
    
    expect(events.length).toBe(3)
    
    const moreIndicator = todayCell.find('.calendar-more-indicator')
    expect(moreIndicator.exists()).toBe(true)
    expect(moreIndicator.text()).toBe('+2 more')
    expect(todayCell.find('.calendar-day-count').text()).toBe('5')
  })

  it('emits startEdit when event is clicked', async () => {
    const wrapper = mount(TodoCalendarView, {
      global: {
        plugins: [i18n]
      },
      props: {
        todos: mockTodos
      }
    })
    
    const todayCell = wrapper.find('.calendar-day.is-today')
    const firstEvent = todayCell.find('.calendar-event')
    
    await firstEvent.trigger('click')
    
    expect(wrapper.emitted('startEdit')).toBeTruthy()
    expect(wrapper.emitted('startEdit')![0][0]).toEqual(mockTodos[0])
  })

  it('emits startEdit when pressing enter on a calendar event', async () => {
    const wrapper = mount(TodoCalendarView, {
      global: {
        plugins: [i18n]
      },
      props: {
        todos: mockTodos
      }
    })

    const todayCell = wrapper.find('.calendar-day.is-today')
    const firstEvent = todayCell.find('.calendar-event')

    await firstEvent.trigger('keydown', { key: 'Enter' })

    expect(wrapper.emitted('startEdit')).toBeTruthy()
    expect(wrapper.emitted('startEdit')![0][0]).toEqual(mockTodos[0])
  })

  it('emits toggleStatus when status toggle is clicked', async () => {
    const wrapper = mount(TodoCalendarView, {
      global: {
        plugins: [i18n]
      },
      props: {
        todos: mockTodos
      }
    })
    
    const todayCell = wrapper.find('.calendar-day.is-today')
    const toggleBtn = todayCell.find('.status-toggle')
    
    await toggleBtn.trigger('click')
    
    expect(wrapper.emitted('toggleStatus')).toBeTruthy()
    expect(wrapper.emitted('toggleStatus')![0][0]).toEqual(mockTodos[0])
    expect(wrapper.emitted('startEdit')).toBeFalsy()
  })

  it('exposes localized navigation aria labels', () => {
    const wrapper = mount(TodoCalendarView, {
      global: {
        plugins: [i18n]
      },
      props: {
        todos: mockTodos
      }
    })

    const navButtons = wrapper.findAll('.calendar-nav-button')
    expect(navButtons).toHaveLength(3)
    expect(navButtons[1].attributes('aria-label')).toBe('Previous month')
    expect(navButtons[2].attributes('aria-label')).toBe('Next month')
  })
})
