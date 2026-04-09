import { describe, expect, it } from 'vitest'
import TodoItemsList from './TodoItemsList.vue'
import { mountWithI18n } from '../../test/test-utils'
import type { TodoDraft, TodoItem, TodoSubItemSummary } from './types'

function createTodo(overrides: Partial<TodoItem> = {}): TodoItem {
  return {
    id: 1,
    title: 'Test task',
    status: 'PENDING',
    priority: 3,
    dueDate: '2026-04-07',
    category: 'Work',
    tags: 'backend,urgent',
    createTime: '2026-04-07T08:00:00',
    updateTime: '2026-04-07T08:00:00',
    ...overrides,
  }
}

function createDraft(): TodoDraft {
  return {
    title: 'Draft',
    priority: 3,
    category: 'Work',
    dueDate: '2026-04-07',
    tags: 'backend',
  }
}

function createSummary(): TodoSubItemSummary {
  return {
    totalCount: 2,
    completedCount: 1,
    progressPercent: 50,
  }
}

describe('TodoItemsList', () => {
  it('forwards row events with todo/id context', async () => {
    const wrapper = mountWithI18n(TodoItemsList, {
      props: {
        todos: [createTodo()],
        selectedIds: [],
        editingId: null,
        editTodoForm: createDraft(),
        viewMode: 'ACTIVE',
        submitting: false,
        categoryListId: 'category-options',
        tagListId: 'tag-options',
        expandedTodoIds: [],
        checklistItemsByTodoId: {},
        checklistSummaryByTodoId: { 1: createSummary() },
        checklistDraftByTodoId: {},
        checklistLoadingTodoIds: [],
        checklistCreatingTodoIds: [],
        checklistPendingSubItemIdsByTodoId: {},
      },
    })

    const checkbox = wrapper.find('input[type="checkbox"]')
    await checkbox.setValue(true)
    expect(wrapper.emitted('update:selected')?.[0]).toEqual([1, true])

    await wrapper.find('.status-toggle').trigger('click')
    await wrapper.find('.edit-btn').trigger('click')
    await wrapper.find('.delete-btn').trigger('click')

    expect(wrapper.emitted('toggleStatus')?.[0]?.[0]).toMatchObject({ id: 1 })
    expect(wrapper.emitted('startEdit')?.[0]?.[0]).toMatchObject({ id: 1 })
    expect(wrapper.emitted('deleteTodo')?.[0]).toEqual([1])

    await wrapper.find('.checklist-toggle-btn').trigger('click')
    expect(wrapper.emitted('toggleChecklist')?.[0]).toEqual([1])
  })

  it('preserves list container when empty using v-show to prevent layout shifts', async () => {
    const wrapper = mountWithI18n(TodoItemsList, {
      props: {
        todos: [],
        selectedIds: [],
        editingId: null,
        editTodoForm: createDraft(),
        viewMode: 'ACTIVE',
        submitting: false,
        categoryListId: 'category-options',
        tagListId: 'tag-options',
        expandedTodoIds: [],
        checklistItemsByTodoId: {},
        checklistSummaryByTodoId: {},
        checklistDraftByTodoId: {},
        checklistLoadingTodoIds: [],
        checklistCreatingTodoIds: [],
        checklistPendingSubItemIdsByTodoId: {},
      },
    })
    const ul = wrapper.find('ul.todo-list')
    expect(ul.exists()).toBe(true)
    // Testing layout stability: v-show means element exists but has display: none
    expect(ul.attributes('style')).toContain('display: none')
  })
})
