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
  it('emits refresh and locale change from the top header controls', async () => {
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

    const select = wrapper.find('#locale-switcher')
    await select.setValue('zh-CN')
    expect(wrapper.emitted('update:locale')?.[0]).toEqual(['zh-CN'])

    const refreshButton = wrapper.find('.header-actions .btn-outline')
    await refreshButton.trigger('click')

    expect(wrapper.emitted('refresh')).toHaveLength(1)
    expect(wrapper.emitted('update:viewMode')).toBeUndefined()
    expect(wrapper.emitted('update:displayMode')).toBeUndefined()
    expect(wrapper.emitted('update:showOptionsPanel')).toBeUndefined()
  })

  it('stabilizes the refresh button width to prevent jitter', async () => {
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
    
    const refreshButton = wrapper.find('.header-actions .btn-outline')
    expect(refreshButton.attributes('style')).toContain('min-width: 100px')
    expect(refreshButton.attributes('style')).toContain('text-align: center')
  })

  it('renders the header summary with total and pending counts', () => {
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

    expect(wrapper.find('.title-group h1').text()).toBe('Tasks')
    expect(wrapper.find('.subtitle').text()).toContain('12 total')
    expect(wrapper.find('.subtitle').text()).toContain('3 pending')
  })
})
