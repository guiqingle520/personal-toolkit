import { describe, expect, it } from 'vitest'
import { mountWithI18n } from '../../test/test-utils'
import TodoWorkbenchHeader from './TodoWorkbenchHeader.vue'

describe('TodoWorkbenchHeader', () => {
  it('renders title in the header title node', () => {
    const wrapper = mountWithI18n(TodoWorkbenchHeader, {
      props: {
        title: 'Tasks',
      },
    })

    expect(wrapper.find('.workbench-header').exists()).toBe(true)
    expect(wrapper.find('.workbench-header-title').text()).toBe('Tasks')
  })

  it('renders subtitle when no summary slot is provided', () => {
    const wrapper = mountWithI18n(TodoWorkbenchHeader, {
      props: {
        title: 'Tasks',
        subtitle: '12 total • 3 pending',
      },
    })

    expect(wrapper.find('.workbench-header-summary').exists()).toBe(true)
    expect(wrapper.find('.workbench-header-summary').text()).toContain('12 total')
  })

  it('renders summary slot instead of subtitle when provided', () => {
    const wrapper = mountWithI18n(TodoWorkbenchHeader, {
      props: {
        title: 'Tasks',
        subtitle: 'fallback subtitle',
      },
      slots: {
        summary: '<div class="custom-summary">slot summary</div>',
      },
    })

    expect(wrapper.find('.workbench-header-summary').exists()).toBe(true)
    expect(wrapper.find('.custom-summary').exists()).toBe(true)
    expect(wrapper.find('.workbench-header-summary').text()).toContain('slot summary')
    expect(wrapper.text()).not.toContain('fallback subtitle')
  })

  it('renders actions slot in the actions region', () => {
    const wrapper = mountWithI18n(TodoWorkbenchHeader, {
      props: {
        title: 'Tasks',
      },
      slots: {
        actions: '<button class="custom-action">Refresh</button>',
      },
    })

    expect(wrapper.find('.workbench-header-actions').exists()).toBe(true)
    expect(wrapper.find('.custom-action').exists()).toBe(true)
  })

  it('omits empty summary and actions containers when unused', () => {
    const wrapper = mountWithI18n(TodoWorkbenchHeader, {
      props: {
        title: 'Tasks',
      },
    })

    expect(wrapper.find('.workbench-header-summary').exists()).toBe(false)
    expect(wrapper.find('.workbench-header-actions').exists()).toBe(false)
  })
})
