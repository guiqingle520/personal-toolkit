import { describe, expect, it } from 'vitest'
import TodoItemRow from './TodoItemRow.vue'
import { mountWithI18n } from '../../test/test-utils'
import type { TodoItem, TodoSubItemSummary } from './types'

function createTodo(): TodoItem {
  return {
    id: 1,
    title: 'Parent task',
    status: 'PENDING',
    priority: 3,
    category: 'Work',
    remindAt: '2026-04-08T07:30:00',
    tags: 'backend',
    notes: 'Draft release notes',
    attachmentLinks: 'https://example.com/spec',
    ownerLabel: 'Alice',
    collaborators: 'Bob,Carol',
    watchers: 'Dave',
    recurrenceType: 'DAILY',
    nextTriggerTime: '2026-04-09T09:00:00',
    completedAt: '2026-04-08T10:00:00',
    createTime: '2026-04-08T08:00:00',
    updateTime: '2026-04-08T08:00:00',
  }
}

function createSummary(): TodoSubItemSummary {
  return {
    totalCount: 3,
    completedCount: 1,
    progressPercent: 33,
  }
}

describe('TodoItemRow', () => {
  it('renders checklist summary and emits checklist toggle', async () => {
    const wrapper = mountWithI18n(TodoItemRow, {
      props: {
        todo: createTodo(),
        isSelected: false,
        categoryListId: 'category-options',
        tagListId: 'tag-options',
        viewMode: 'ACTIVE',
        submitting: false,
        checklistExpanded: false,
        checklistItems: [],
        checklistSummary: createSummary(),
        checklistDraftTitle: '',
        checklistLoading: false,
        checklistCreating: false,
        checklistPendingIds: [],
      },
    })

    expect(wrapper.text()).toContain('1/3 completed')
    expect(wrapper.text()).toContain('Daily')
    expect(wrapper.text()).toContain('Scheduled:')
    expect(wrapper.text()).toContain('Reminder:')
    expect(wrapper.text()).toContain('Draft release notes')
    expect(wrapper.text()).toContain('Open attachment')
    expect(wrapper.text()).toContain('Owner: Alice')
    expect(wrapper.text()).toContain('Collaborators: Bob,Carol')
    expect(wrapper.text()).toContain('Watchers: Dave')

    await wrapper.find('.checklist-toggle-btn').trigger('click')
    expect(wrapper.emitted('toggleChecklist')).toHaveLength(1)
  })

  it('exposes todo-specific accessible names for selection and row actions', () => {
    const wrapper = mountWithI18n(TodoItemRow, {
      props: {
        todo: createTodo(),
        isSelected: false,
        categoryListId: 'category-options',
        tagListId: 'tag-options',
        viewMode: 'ACTIVE',
        submitting: false,
        checklistExpanded: false,
        checklistItems: [],
        checklistSummary: createSummary(),
        checklistDraftTitle: '',
        checklistLoading: false,
        checklistCreating: false,
        checklistPendingIds: [],
      },
    })

    expect(wrapper.find('.todo-select-checkbox').attributes('aria-labelledby')).toBe('todo-title-1')
    expect(wrapper.find('#todo-title-1').text()).toBe('Parent task')
    expect(wrapper.find('.status-toggle').attributes('aria-label')).toBe('Mark as Done: Parent task')
    expect(wrapper.find('.checklist-toggle-btn').attributes('aria-label')).toBe('Show checklist: Parent task')
    expect(wrapper.find('.edit-btn').attributes('aria-label')).toBe('Edit: Parent task')
    expect(wrapper.find('.delete-btn').attributes('aria-label')).toBe('Delete: Parent task')
  })
})
