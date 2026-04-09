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
  it('emits refresh, locale change, view mode toggles, and options toggle', async () => {
    const wrapper = mountWithI18n(TodoToolbar, {
      props: {
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

    const refreshButton = wrapper.find('header button')
    const toggleButtons = wrapper.findAll('.view-toggle-bar button')
    await refreshButton.trigger('click')
    await toggleButtons[0].trigger('click')
    await toggleButtons[1].trigger('click')
    await toggleButtons[2].trigger('click')

    expect(wrapper.emitted('refresh')).toHaveLength(1)
    expect(wrapper.emitted('update:viewMode')).toEqual([['ACTIVE'], ['RECYCLE_BIN']])
    expect(wrapper.emitted('update:showOptionsPanel')).toEqual([[true]])
  })

  it('stabilizes the refresh button width to prevent jitter', async () => {
    const wrapper = mountWithI18n(TodoToolbar, {
      props: {
        pageData: createPageData(),
        pendingCount: 3,
        loading: false,
        viewMode: 'ACTIVE',
        showOptionsPanel: false,
        locale: 'en',
      },
    })
    
    const refreshButton = wrapper.find('header button')
    expect(refreshButton.attributes('style')).toContain('min-width: 100px')
    expect(refreshButton.attributes('style')).toContain('text-align: center')
  })
})
