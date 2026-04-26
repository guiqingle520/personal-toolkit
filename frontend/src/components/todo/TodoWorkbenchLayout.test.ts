import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import TodoWorkbenchLayout from './TodoWorkbenchLayout.vue'

describe('TodoWorkbenchLayout', () => {
  it('renders header, menu, main, and sidebar slots in the correct regions', () => {
    const wrapper = mount(TodoWorkbenchLayout, {
      slots: {
        header: '<div class="slot-header">Header</div>',
        menu: '<div class="slot-menu">Menu</div>',
        default: '<div class="slot-main">Main</div>',
        sidebar: '<div class="slot-sidebar">Sidebar</div>',
      },
    })

    expect(wrapper.find('.workbench-layout').exists()).toBe(true)
    expect(wrapper.find('.workbench-top .slot-header').exists()).toBe(true)
    expect(wrapper.find('.workbench-menu .slot-menu').exists()).toBe(true)
    expect(wrapper.find('.workbench-main .slot-main').exists()).toBe(true)
    expect(wrapper.find('.workbench-sidebar .slot-sidebar').exists()).toBe(true)
  })

  it('omits header wrapper when header slot is absent', () => {
    const wrapper = mount(TodoWorkbenchLayout, {
      slots: {
        default: '<div class="slot-main">Main</div>',
      },
    })

    expect(wrapper.find('.workbench-top').exists()).toBe(false)
    expect(wrapper.find('.workbench-main .slot-main').exists()).toBe(true)
  })

  it('omits menu wrapper and adds no-menu modifier when menu slot is absent', () => {
    const wrapper = mount(TodoWorkbenchLayout, {
      slots: {
        default: '<div class="slot-main">Main</div>',
        sidebar: '<div class="slot-sidebar">Sidebar</div>',
      },
    })

    expect(wrapper.find('.workbench-menu').exists()).toBe(false)
    expect(wrapper.find('.workbench-body').classes()).toContain('workbench-body--no-menu')
    expect(wrapper.find('.workbench-sidebar .slot-sidebar').exists()).toBe(true)
  })

  it('omits sidebar wrapper and adds no-sidebar modifier when sidebar slot is absent', () => {
    const wrapper = mount(TodoWorkbenchLayout, {
      slots: {
        menu: '<div class="slot-menu">Menu</div>',
        default: '<div class="slot-main">Main</div>',
      },
    })

    expect(wrapper.find('.workbench-sidebar').exists()).toBe(false)
    expect(wrapper.find('.workbench-body').classes()).toContain('workbench-body--no-sidebar')
    expect(wrapper.find('.workbench-menu .slot-menu').exists()).toBe(true)
  })

  it('supports main-only layout', () => {
    const wrapper = mount(TodoWorkbenchLayout, {
      slots: {
        default: '<div class="slot-main">Main</div>',
      },
    })

    const body = wrapper.find('.workbench-body')

    expect(wrapper.find('.workbench-menu').exists()).toBe(false)
    expect(wrapper.find('.workbench-sidebar').exists()).toBe(false)
    expect(body.classes()).toContain('workbench-body--no-menu')
    expect(body.classes()).toContain('workbench-body--no-sidebar')
    expect(wrapper.find('.workbench-main .slot-main').exists()).toBe(true)
  })
})
