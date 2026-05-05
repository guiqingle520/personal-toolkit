import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TodoKanbanView from '../todo/TodoKanbanView.vue'
import { createI18n } from 'vue-i18n'
import type { TodoItem } from '../todo/types'

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      kanban: {
        pendingColumn: 'Pending',
        doneColumn: 'Done',
        dropHere: 'Drop Here',
        emptyColumn: 'Empty',
      }
    }
  }
})

describe('TodoKanbanView.vue', () => {
  const dummyTodos: TodoItem[] = [
    {
      id: 1,
      title: 'Pending Todo',
      status: 'PENDING',
      createTime: '2026-01-01T00:00:00Z',
      updateTime: '2026-01-01T00:00:00Z'
    },
    {
      id: 2,
      title: 'Done Todo',
      status: 'DONE',
      createTime: '2026-01-01T00:00:00Z',
      updateTime: '2026-01-01T00:00:00Z'
    }
  ]

  const defaultProps = {
    todos: dummyTodos,
    selectedIds: [],
    viewMode: 'ACTIVE' as const,
    submitting: false,
    categoryListId: '',
    tagListId: '',
    expandedTodoIds: [],
    checklistItemsByTodoId: {},
    checklistSummaryByTodoId: {},
    checklistDraftByTodoId: {},
    checklistLoadingTodoIds: [],
    checklistCreatingTodoIds: [],
    checklistPendingSubItemIdsByTodoId: {}
  }

  it('renders PENDING and DONE columns with correct CSS hooks', () => {
    const wrapper = mount(TodoKanbanView, {
      props: defaultProps,
      global: {
        plugins: [i18n]
      }
    })

    const pendingColumn = wrapper.find('.kanban-column--PENDING')
    expect(pendingColumn.exists()).toBe(true)
    expect(pendingColumn.attributes('data-status')).toBe('PENDING')
    expect(pendingColumn.find('h3').text()).toBe('Pending')

    const doneColumn = wrapper.find('.kanban-column--DONE')
    expect(doneColumn.exists()).toBe(true)
    expect(doneColumn.attributes('data-status')).toBe('DONE')
    expect(doneColumn.find('h3').text()).toBe('Done')
  })

  it('applies is-drag-over class when dragover event is emitted', async () => {
    const wrapper = mount(TodoKanbanView, {
      props: defaultProps,
      global: {
        plugins: [i18n]
      }
    })

    const pendingColumn = wrapper.find('.kanban-column--PENDING')
    await pendingColumn.trigger('dragover')
    
    expect(pendingColumn.classes()).toContain('is-drag-over')
  })
})
