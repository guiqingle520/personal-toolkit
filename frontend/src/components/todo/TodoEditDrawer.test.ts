import { describe, it, expect } from 'vitest'
import type { ComponentPublicInstance, DefineComponent } from 'vue'
import { mountWithI18n } from '../../test/test-utils'
import TodoEditDrawer from './TodoEditDrawer.vue'

describe('TodoEditDrawer', () => {
  const defaultDraft = {
    title: 'Test Todo',
    priority: 3,
    category: 'Work',
    dueDate: '',
    remindAt: '',
    tags: '',
    notes: '',
    attachmentLinks: '',
    ownerLabel: '',
    collaborators: '',
    watchers: '',
    recurrenceType: '',
    recurrenceInterval: 1,
    recurrenceEndTime: ''
  }

  const createWrapper = (props = {}) => {
    return mountWithI18n(TodoEditDrawer as DefineComponent<unknown, unknown, unknown, unknown, unknown, unknown, unknown, unknown, string, ComponentPublicInstance>, {
      props: {
        isOpen: true,
        editForm: defaultDraft,
        categoryListId: 'cat-list',
        tagListId: 'tag-list',
        submitting: false,
        ...props
      },
      global: {
        stubs: {
          Teleport: true
        }
      }
    })
  }

  it('renders correctly when open', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.todo-edit-drawer').exists()).toBe(true)
  })

  it('emits cancel when close button is clicked', async () => {
    const wrapper = createWrapper()
    await wrapper.find('.drawer-close').trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })

  it('emits cancel when overlay is clicked', async () => {
    const wrapper = createWrapper()
    await wrapper.find('.todo-edit-drawer-overlay').trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })

  it('emits save when save button is clicked', async () => {
    const wrapper = createWrapper()
    await wrapper.find('.drawer-footer .btn-success').trigger('click')
    expect(wrapper.emitted('save')).toBeTruthy()
  })

  it('updates form when title changes', async () => {
    const wrapper = createWrapper()
    const input = wrapper.find('input.title-input')
    await input.setValue('New Title')
    
    const updates = wrapper.emitted('update:editForm')
    expect(updates).toBeTruthy()
    expect(updates![0][0]).toEqual({
      ...defaultDraft,
      title: 'New Title'
    })
  })
})
