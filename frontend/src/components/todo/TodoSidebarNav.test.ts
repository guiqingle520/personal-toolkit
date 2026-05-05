import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import { createI18n } from 'vue-i18n'
import TodoSidebarNav from './TodoSidebarNav.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      app: {
        tasks: 'Tasks',
        activeTasks: 'Active',
        recycleBin: 'Recycle Bin',
        statistics: 'Statistics',
        views: 'Views',
        listView: 'List',
        kanbanView: 'Kanban',
        calendarView: 'Calendar',
        settings: 'Settings',
        manageCategories: 'Manage Categories'
      }
    }
  }
})

describe('TodoSidebarNav.vue', () => {
  it('renders correctly and respects auto-expand props', () => {
    const wrapper = mount(TodoSidebarNav, {
      global: {
        plugins: [i18n]
      },
      props: {
        routeName: 'tasks',
        viewMode: 'ACTIVE',
        displayMode: 'LIST',
        showOptionsPanel: false
      }
    })

    // Label should be a button with aria-expanded true for tasks
    const tasksButton = wrapper.findAll('.workbench-menu-label').filter(w => w.text().includes('Tasks'))
    expect(tasksButton.length).toBe(1)
    expect(tasksButton[0].attributes('aria-expanded')).toBe('true')
  })

  it('toggles sections when clicked', async () => {
    const wrapper = mount(TodoSidebarNav, {
      global: {
        plugins: [i18n]
      },
      props: {
        routeName: 'tasks',
        viewMode: 'ACTIVE'
      }
    })

    const buttons = wrapper.findAll('.workbench-menu-label')
    const statisticsButton = buttons.find(w => w.text().includes('Statistics'))
    
    // Auto-expanded initially since routeName is 'tasks', statistics should be false
    expect(statisticsButton?.attributes('aria-expanded')).toBe('false')

    // Click to expand
    await statisticsButton?.trigger('click')
    expect(statisticsButton?.attributes('aria-expanded')).toBe('true')
    
    // The group container should now be visible
    const group = wrapper.find('#menu-group-statistics')
    expect(group.isVisible()).toBe(true)
  })

  it('emits events when children are clicked', async () => {
    const wrapper = mount(TodoSidebarNav, {
      global: {
        plugins: [i18n]
      },
      props: {
        routeName: 'tasks',
        viewMode: 'ACTIVE'
      }
    })

    const activeTasksBtn = wrapper.findAll('.workbench-menu-button').find(w => w.text().includes('Active'))
    await activeTasksBtn?.trigger('click')
    
    // Check emits
    expect(wrapper.emitted('update:viewMode')).toBeTruthy()
    expect(wrapper.emitted('update:viewMode')?.[0]).toEqual(['ACTIVE'])
  })
})
