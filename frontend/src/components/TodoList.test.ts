import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TodoList from './TodoList.vue'
import i18n from '../i18n'

const fetchMock = vi.fn()

function createPageResponse() {
  return {
    success: true,
    message: 'ok',
    timestamp: '2026-04-07T00:00:00',
    data: {
      content: [
        {
          id: 1,
          title: 'Alpha',
          status: 'PENDING',
          priority: 3,
          dueDate: '2026-04-08',
          category: 'Work',
          tags: 'backend',
          recurrenceType: 'DAILY',
          recurrenceInterval: 1,
          recurrenceEndTime: '2026-05-08T00:00:00',
          nextTriggerTime: '2026-04-09T09:00:00',
          subItemSummary: {
            totalCount: 2,
            completedCount: 1,
            progressPercent: 50,
          },
          createTime: '2026-04-07T08:00:00',
          updateTime: '2026-04-07T08:00:00',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 10,
      first: true,
      last: true,
    },
  }
}

function createOptionsResponse() {
  return {
    success: true,
    message: 'ok',
    timestamp: '2026-04-07T00:00:00',
    data: {
      categories: ['Work'],
      tags: ['backend'],
    },
  }
}

function createSubItemsResponse() {
  return {
    success: true,
    message: 'ok',
    timestamp: '2026-04-07T00:00:00',
    data: [
      {
        id: 101,
        todoId: 1,
        title: 'Write API tests',
        status: 'PENDING',
        sortOrder: 0,
        createTime: '2026-04-07T00:00:00',
        updateTime: '2026-04-07T00:00:00',
      },
    ],
  }
}

function createSuccessResponse(data: unknown) {
  return {
    success: true,
    message: 'ok',
    timestamp: '2026-04-07T00:00:00',
    data,
  }
}

function createValidationErrorResponse() {
  return {
    success: false,
    message: 'Validation failed',
    timestamp: '2026-04-07T00:00:00',
    validation: {
      title: ['title must not be blank'],
    },
  }
}

async function mountTodoList(): Promise<VueWrapper<any>> {
  const wrapper = mount(TodoList, {
    global: {
      plugins: [i18n],
    },
  })

  await flushPromises()
  return wrapper
}

function getRowSelectionCheckbox(wrapper: VueWrapper<any>) {
  return wrapper.find('.todo-list .todo-select-checkbox')
}

function getFilterControls(wrapper: VueWrapper<any>) {
  const filterSection = wrapper.find('.filter-section')
  return {
    search: filterSection.findAll('input')[0],
    status: filterSection.findAll('select')[0],
    priority: filterSection.findAll('select')[1],
    dueDateFrom: filterSection.findAll('input[type="date"]')[0],
    resetButton: filterSection.find('button.btn-ghost'),
  }
}

describe('TodoList reset behavior', () => {
  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
    fetchMock
      .mockResolvedValueOnce({ ok: true, json: async () => createPageResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createOptionsResponse() })
      .mockResolvedValue({ ok: true, json: async () => createPageResponse() })
  })

  it('resets filters back to defaults and clears current selection', async () => {
    const wrapper = await mountTodoList()

    await getRowSelectionCheckbox(wrapper).setValue(true)
    expect((wrapper.vm as any).selectedIds).toEqual([1])

    const controls = getFilterControls(wrapper)
    await controls.search.setValue('Alpha')
    await controls.status.setValue('DONE')
    await controls.priority.setValue('4')
    await controls.dueDateFrom.setValue('2026-04-09')

    ;(wrapper.vm as any).filters.page = 3

    await controls.resetButton.trigger('click')
    await flushPromises()

    expect((wrapper.vm as any).selectedIds).toEqual([])
    expect((wrapper.vm as any).filters).toMatchObject({
      page: 0,
      status: '',
      priority: '',
      category: '',
      keyword: '',
      tag: '',
      dueDateFrom: '',
      dueDateTo: '',
      sortBy: 'createTime',
      sortDir: 'DESC',
    })
  })

  it('clears selection and forces page zero when child filter updates arrive', async () => {
    const wrapper = await mountTodoList()

    await getRowSelectionCheckbox(wrapper).setValue(true)
    expect((wrapper.vm as any).selectedIds).toEqual([1])

    const nextFilters = {
      ...(wrapper.vm as any).filters,
      keyword: 'beta',
      page: 5,
    }

    wrapper.findComponent({ name: 'TodoFilters' }).vm.$emit('update:filters', nextFilters)
    await wrapper.vm.$nextTick()

    expect((wrapper.vm as any).selectedIds).toEqual([])
    expect((wrapper.vm as any).filters).toMatchObject({
      keyword: 'beta',
      page: 0,
    })
  })

  it('requests initial list and options contracts on mount', async () => {
    await mountTodoList()

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      '/api/todos?page=0&size=10&sortBy=createTime&sortDir=DESC',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/todos/options',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
  })

  it('sends create payload with serialized due date', async () => {
    fetchMock
      .mockReset()
      .mockResolvedValueOnce({ ok: true, json: async () => createPageResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createOptionsResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createSuccessResponse({ id: 2 }) })
      .mockResolvedValueOnce({ ok: true, json: async () => createPageResponse() })

    const wrapper = await mountTodoList()
    const createRows = wrapper.findAll('.create-form .create-row')
    const titleInput = createRows[0].find('input[type="text"]')
    const prioritySelect = createRows[0].find('select')
    const dueDateInput = createRows[1].find('input[type="date"]')
    const tagsInput = createRows[1].findAll('input[type="text"]')[0]
    const addButton = createRows[1].find('button.btn-primary')

    await titleInput.setValue('Ship contract tests')
    await prioritySelect.setValue('4')
    await dueDateInput.setValue('2026-04-10')
    await tagsInput.setValue('qa,api')
    await addButton.trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      '/api/todos',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          title: 'Ship contract tests',
          status: 'PENDING',
          priority: 4,
          category: '',
          dueDate: '2026-04-10T00:00:00',
          tags: 'qa,api',
        }),
      }),
    )
  })

  it('sends recurrence fields in create payload when recurrence is enabled', async () => {
    fetchMock
      .mockReset()
      .mockResolvedValueOnce({ ok: true, json: async () => createPageResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createOptionsResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createSuccessResponse({ id: 2 }) })
      .mockResolvedValueOnce({ ok: true, json: async () => createPageResponse() })

    const wrapper = await mountTodoList()
    const createRows = wrapper.findAll('.create-form .create-row')
    const titleInput = createRows[0].find('input[type="text"]')
    const recurrenceSelect = createRows[1].find('select')
    const dueDateInput = createRows[1].find('input[type="date"]')

    await titleInput.setValue('Daily standup')
    await dueDateInput.setValue('2026-04-10')
    await recurrenceSelect.setValue('DAILY')
    await createRows[1].findAll('input[type="number"]')[0].setValue('2')
    await createRows[1].findAll('input[type="date"]')[1].setValue('2026-05-10')
    await createRows[1].find('button.btn-primary').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      '/api/todos',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          title: 'Daily standup',
          status: 'PENDING',
          priority: 3,
          category: '',
          dueDate: '2026-04-10T00:00:00',
          tags: '',
          recurrenceType: 'DAILY',
          recurrenceInterval: 2,
          recurrenceEndTime: '2026-05-10T00:00:00',
        }),
      }),
    )
  })

  it('preserves recurrence fields when toggling status', async () => {
    fetchMock
      .mockReset()
      .mockResolvedValueOnce({ ok: true, json: async () => createPageResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createOptionsResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createSuccessResponse({ id: 1 }) })
      .mockResolvedValueOnce({ ok: true, json: async () => createPageResponse() })

    const wrapper = await mountTodoList()

    await wrapper.find('.status-toggle').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      '/api/todos/1',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({
          id: 1,
          title: 'Alpha',
          status: 'DONE',
          priority: 3,
          dueDate: '2026-04-08',
          category: 'Work',
          tags: 'backend',
          recurrenceType: 'DAILY',
          recurrenceInterval: 1,
          recurrenceEndTime: '2026-05-08T00:00:00',
          nextTriggerTime: '2026-04-09T09:00:00',
          subItemSummary: {
            totalCount: 2,
            completedCount: 1,
            progressPercent: 50,
          },
          createTime: '2026-04-07T08:00:00',
          updateTime: '2026-04-07T08:00:00',
        }),
      }),
    )
  })

  it('sends restore request to the recycle-bin endpoint', async () => {
    fetchMock
      .mockReset()
      .mockResolvedValueOnce({ ok: true, json: async () => createPageResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createOptionsResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createSuccessResponse({ id: 1 }) })
      .mockResolvedValueOnce({ ok: true, json: async () => createPageResponse() })

    const wrapper = await mountTodoList()

    wrapper.findComponent({ name: 'TodoToolbar' }).vm.$emit('update:viewMode', 'RECYCLE_BIN')
    await flushPromises()

    wrapper.findComponent({ name: 'TodoItemsList' }).vm.$emit('restoreTodo', 1)
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos/1/restore',
      expect.objectContaining({
        method: 'PUT',
      }),
    )
  })

  it('renders backend validation errors from the error envelope', async () => {
    fetchMock
      .mockReset()
      .mockResolvedValueOnce({ ok: true, json: async () => createPageResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createOptionsResponse() })
      .mockResolvedValueOnce({ ok: false, json: async () => createValidationErrorResponse() })

    const wrapper = await mountTodoList()
    const titleInput = wrapper.find('.create-form .create-row input[type="text"]')
    const addButton = wrapper.find('.create-form button.btn-primary')

    await titleInput.setValue('Trigger validation envelope')
    await addButton.trigger('click')
    await flushPromises()

    expect(wrapper.find('.error-banner').text()).toContain('Validation failed')
    expect(wrapper.find('.validation-list').text()).toContain('title')
    expect(wrapper.find('.validation-list').text()).toContain('title must not be blank')
  })

  it('lazy-loads sub-items when a row checklist is expanded', async () => {
    fetchMock
      .mockReset()
      .mockResolvedValueOnce({ ok: true, json: async () => createPageResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createOptionsResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createSubItemsResponse() })

    const wrapper = await mountTodoList()

    await wrapper.find('.checklist-toggle-btn').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      '/api/todos/1/sub-items',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
    expect(wrapper.text()).toContain('Write API tests')
  })

  it('creates and updates checklist state without reloading the full todo list', async () => {
    fetchMock
      .mockReset()
      .mockResolvedValueOnce({ ok: true, json: async () => createPageResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createOptionsResponse() })
      .mockResolvedValueOnce({ ok: true, json: async () => createSubItemsResponse() })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => createSuccessResponse({
          id: 102,
          todoId: 1,
          title: 'Ship checklist UI',
          status: 'PENDING',
          sortOrder: 1,
          createTime: '2026-04-07T00:00:00',
          updateTime: '2026-04-07T00:00:00',
        }),
      })

    const wrapper = await mountTodoList()

    await wrapper.find('.checklist-toggle-btn').trigger('click')
    await flushPromises()

    const subtaskInput = wrapper.find('.todo-subtasks-create input')
    const addSubtaskButton = wrapper.find('.todo-subtasks-create button')

    await subtaskInput.setValue('Ship checklist UI')
    await addSubtaskButton.trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenNthCalledWith(
      4,
      '/api/todos/1/sub-items',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ title: 'Ship checklist UI' }),
      }),
    )
    expect(fetchMock).toHaveBeenCalledTimes(4)
    expect(wrapper.text()).toContain('Ship checklist UI')
  })

  it('preserves layout stability during empty state transitions', async () => {
    fetchMock
      .mockReset()
      .mockResolvedValueOnce({ ok: true, json: async () => ({ ...createPageResponse(), data: { ...createPageResponse().data, content: [], totalElements: 0 } }) })
      .mockResolvedValueOnce({ ok: true, json: async () => createOptionsResponse() })

    const wrapper = await mountTodoList()

    // empty state div should be visible and have state-message class, not wrapped in a conditional that unmounts
    const emptyState = wrapper.find('.state-message')
    expect(emptyState.exists()).toBe(true)
    expect(emptyState.classes()).toContain('empty')

    // list container is preserved via v-show even when empty to prevent layout shifts
    const todoList = wrapper.find('.todo-list')
    expect(todoList.exists()).toBe(true)
    // with v-show it should exist, whereas v-if would remove it
  })
})
