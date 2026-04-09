import { describe, expect, it } from 'vitest'
import TodoItemRow from './TodoItemRow.vue'
import { mountWithI18n } from '../../test/test-utils'
import type { TodoDraft, TodoItem, TodoSubItemSummary } from './types'

function createTodo(): TodoItem {
  return {
    id: 1,
    title: 'Parent task',
    status: 'PENDING',
    priority: 3,
    category: 'Work',
    tags: 'backend',
    recurrenceType: 'DAILY',
    nextTriggerTime: '2026-04-09T09:00:00',
    completedAt: '2026-04-08T10:00:00',
    createTime: '2026-04-08T08:00:00',
    updateTime: '2026-04-08T08:00:00',
  }
}

function createDraft(): TodoDraft {
  return {
    title: 'Parent task',
    priority: 3,
    category: 'Work',
    dueDate: '',
    tags: 'backend',
    recurrenceType: 'daily',
    recurrenceInterval: 1
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
        isEditing: false,
        editForm: createDraft(),
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

    await wrapper.find('.checklist-toggle-btn').trigger('click')
    expect(wrapper.emitted('toggleChecklist')).toHaveLength(1)
  })
})
