import { describe, expect, it } from 'vitest'
import TodoToolbar from './TodoToolbar.vue'
import { mountWithI18n } from '../../test/test-utils'
import type { PageData, TodoItem } from './types'

function createPageData(): PageData<TodoItem> {
  return {
    content: [],
    totalElements: 12,
    totalPages: 2,
    page: 0,
    size: 10,
    first: true,
    last: false,
  }
}

describe('TodoToolbar', () => {
  it('renders through TodoWorkbenchHeader and emits refresh / locale change', async () => {
    const wrapper = mountWithI18n(TodoToolbar, {
      props: {
        displayMode: 'LIST',
        pageData: createPageData(),
        pendingCount: 3,
        loading: false,
        viewMode: 'ACTIVE',
        showOptionsPanel: false,
        locale: 'en',
      },
    })

    expect(wrapper.find('.workbench-header').exists()).toBe(true)
    expect(wrapper.find('.workbench-header-title').text()).toBe('Tasks')

    const select = wrapper.find('#locale-switcher')
    await select.setValue('zh-CN')
    expect(wrapper.emitted('update:locale')?.[0]).toEqual(['zh-CN'])

    const refreshButton = wrapper.find('.workbench-refresh-button')
    await refreshButton.trigger('click')

    expect(wrapper.emitted('refresh')).toHaveLength(1)
    expect(wrapper.emitted('update:viewMode')).toBeUndefined()
    expect(wrapper.emitted('update:displayMode')).toBeUndefined()
    expect(wrapper.emitted('update:showOptionsPanel')).toBeUndefined()
  })

  it('uses class-based refresh button styling hook', async () => {
    const wrapper = mountWithI18n(TodoToolbar, {
      props: {
        displayMode: 'LIST',
        pageData: createPageData(),
        pendingCount: 3,
        loading: false,
        viewMode: 'ACTIVE',
        showOptionsPanel: false,
        locale: 'en',
      },
    })
    
    const refreshButton = wrapper.find('.workbench-refresh-button')
    expect(refreshButton.exists()).toBe(true)
    expect(refreshButton.attributes('style')).toBeUndefined()
  })

  it('renders summary with total and pending counts', () => {
    const wrapper = mountWithI18n(TodoToolbar, {
      props: {
        displayMode: 'LIST',
        pageData: createPageData(),
        pendingCount: 3,
        loading: false,
        viewMode: 'ACTIVE',
        showOptionsPanel: false,
        locale: 'en',
      },
    })

    const summary = wrapper.find('.workbench-header-summary')
    expect(summary.exists()).toBe(true)
    expect(summary.text()).toContain('12 total')
    expect(summary.text()).toContain('3 pending')
  })
})
