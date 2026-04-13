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

function createSuccessResponse(data) {
  return {
    success: true,
    message: 'ok',
    timestamp: '2026-04-07T00:00:00',
    data,
  }
}

function createStatsOverviewResponse() {
  return createSuccessResponse({
    todayCompleted: 2,
    weekCompleted: 7,
    overdueCount: 3,
    activeCount: 11,
  })
}

function createStatsCategoryResponse() {
  return createSuccessResponse([
    { category: 'Work', activeCount: 4, completedCount: 2 },
    { category: '__UNCLASSIFIED__', activeCount: 3, completedCount: 1 },
  ])
}

function createStatsTrendResponse() {
  return createSuccessResponse({
    range: '7d',
    items: [
      { date: '2026-04-01', completedCount: 1 },
      { date: '2026-04-02', completedCount: 2 },
    ],
  })
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

async function mountTodoList() {
  const wrapper = mount(TodoList, {
    global: {
      plugins: [i18n],
    },
  })

  await flushPromises()
  return wrapper
}

function getRowSelectionCheckbox(wrapper) {
  return wrapper.find('.todo-list .todo-select-checkbox')
}

function getFilterControls(wrapper) {
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
    fetchMock.mockImplementation(async (url, options) => {
      if (url.includes('/api/todos/options')) return { ok: true, json: async () => createOptionsResponse() }
      if (url.includes('/api/todos/stats/overview')) return { ok: true, json: async () => createStatsOverviewResponse() }
      if (url.includes('/api/todos/stats/by-category')) return { ok: true, json: async () => createStatsCategoryResponse() }
      if (url.includes('/api/todos/stats/trend')) return { ok: true, json: async () => createStatsTrendResponse() }
      if (url.includes('/api/todos/1/sub-items') && options?.method === 'POST') return { ok: true, json: async () => createSuccessResponse({ id: 102, todoId: 1, title: 'Ship checklist UI', status: 'PENDING', sortOrder: 1, createTime: '2026-04-07T00:00:00', updateTime: '2026-04-07T00:00:00' }) }
      if (url.includes('/api/todos/1/sub-items') && (!options?.method || options.method === 'GET')) return { ok: true, json: async () => createSubItemsResponse() }
      if (url.includes('/api/todos') && options?.method === 'POST') {
        if (options.body && options.body.includes('Trigger validation envelope')) return { ok: false, json: async () => createValidationErrorResponse() }
        return { ok: true, json: async () => createSuccessResponse({ id: 2 }) }
      }
      if (url.includes('/api/todos/1/restore')) return { ok: true, json: async () => createSuccessResponse({ id: 1 }) }
      if (url.includes('/api/todos/') && options?.method === 'PUT') return { ok: true, json: async () => createSuccessResponse({ id: 1 }) }
      if (url.includes('/api/todos/1/sub-items') && options?.method === 'DELETE') return { ok: true, json: async () => createSuccessResponse(null) }
      if (url.includes('/api/todos/1/sub-items') && options?.method === 'PUT') return { ok: true, json: async () => createSuccessResponse({ id: 101, todoId: 1, title: 'Write API tests', status: 'DONE', sortOrder: 0, createTime: '2026-04-07T00:00:00', updateTime: '2026-04-07T00:00:00' }) }
      if (url.includes('/api/todos/') && options?.method === 'DELETE') return { ok: true, json: async () => createSuccessResponse(null) }
      if (url.includes('/api/todos/batch/')) return { ok: true, json: async () => createSuccessResponse(null) }
      return { ok: true, json: async () => createPageResponse() }
    })
  })

  it('resets filters back to defaults and clears current selection', async () => {
    const wrapper = await mountTodoList()

    await getRowSelectionCheckbox(wrapper).setValue(true)
    expect(wrapper.vm.selectedIds).toEqual([1])

    const controls = getFilterControls(wrapper)
    await controls.search.setValue('Alpha')
    await controls.status.setValue('DONE')
    await controls.priority.setValue('4')
    await controls.dueDateFrom.setValue('2026-04-09')

    ;(wrapper.vm).filters.page = 3

    await controls.resetButton.trigger('click')
    await flushPromises()

    expect(wrapper.vm.selectedIds).toEqual([])
    expect(wrapper.vm.filters).toMatchObject({
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
    expect(wrapper.vm.selectedIds).toEqual([1])

    const nextFilters = {
      ...wrapper.vm.filters,
      keyword: 'beta',
      page: 5,
    }

    wrapper.findComponent({ name: 'TodoFilters' }).vm.$emit('update:filters', nextFilters)
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.selectedIds).toEqual([])
    expect(wrapper.vm.filters).toMatchObject({
      keyword: 'beta',
      page: 0,
    })
  })

  it('requests initial list and options contracts on mount', async () => {
    await mountTodoList()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos?page=0&size=10&sortBy=createTime&sortDir=DESC',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos/options',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos/stats/overview',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos/stats/by-category',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos/stats/trend?range=7d',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
  })

  it('renders stats panel in active mode with localized uncategorized label', async () => {
    const wrapper = await mountTodoList()

    expect(wrapper.text()).toContain('Today Completed')
    expect(wrapper.text()).toContain('Week Completed')
    expect(wrapper.text()).toContain('Overdue Tasks')
    expect(wrapper.text()).toContain('Active Tasks')
    expect(wrapper.text()).toContain('Uncategorized')
    expect(wrapper.text()).toContain('7-Day Trend')
  })

  it('hides stats panel in recycle bin mode', async () => {
    const wrapper = await mountTodoList()

    wrapper.findComponent({ name: 'TodoToolbar' }).vm.$emit('update:viewMode', 'RECYCLE_BIN')
    await flushPromises()

    expect(wrapper.find('.todo-stats-panel').exists()).toBe(false)
  })

  it('switches active tasks to kanban view and hides list-only controls', async () => {
    const wrapper = await mountTodoList()

    const toolbarButtons = wrapper.findAll('.view-toggle-bar button')
    const kanbanButton = toolbarButtons.find((button) => button.text().includes('Kanban View'))
    expect(kanbanButton).toBeTruthy()

    await kanbanButton!.trigger('click')
    await flushPromises()

    expect(wrapper.find('.kanban-board').exists()).toBe(true)
    expect(wrapper.find('.filter-section').exists()).toBe(false)
    expect(wrapper.find('.create-form').exists()).toBe(false)
    expect(wrapper.find('.pagination').exists()).toBe(false)
    expect(wrapper.text()).toContain('Pending')
    expect(wrapper.text()).toContain('Done')
  })

  it('resets kanban display mode back to list when switching to recycle bin', async () => {
    const wrapper = await mountTodoList()

    const toolbarButtons = wrapper.findAll('.view-toggle-bar button')
    const kanbanButton = toolbarButtons.find((button) => button.text().includes('Kanban View'))
    await kanbanButton!.trigger('click')
    await flushPromises()

    wrapper.findComponent({ name: 'TodoToolbar' }).vm.$emit('update:viewMode', 'RECYCLE_BIN')
    await flushPromises()

    expect(wrapper.find('.kanban-board').exists()).toBe(false)
    expect(wrapper.find('.todo-list').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('Kanban View')
  })

  it('sends create payload with serialized due date', async () => {
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

    expect(fetchMock).toHaveBeenCalledWith(
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

    expect(fetchMock).toHaveBeenCalledWith(
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
    const wrapper = await mountTodoList()

    await wrapper.find('.status-toggle').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
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
    const wrapper = await mountTodoList()

    await wrapper.find('.checklist-toggle-btn').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
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
    const wrapper = await mountTodoList()

    await wrapper.find('.checklist-toggle-btn').trigger('click')
    await flushPromises()

    const subtaskInput = wrapper.find('.todo-subtasks-create input')
    const addSubtaskButton = wrapper.find('.todo-subtasks-create button')

    await subtaskInput.setValue('Ship checklist UI')
    await addSubtaskButton.trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos/1/sub-items',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ title: 'Ship checklist UI' }),
      }),
    )
    expect(wrapper.text()).toContain('Ship checklist UI')
  })

  it('preserves layout stability during empty state transitions', async () => {
    fetchMock.mockImplementation(async (url, options) => {
      if (url.includes('/api/todos/options')) return { ok: true, json: async () => createOptionsResponse() }
      if (url.includes('/api/todos/stats/overview')) return { ok: true, json: async () => createStatsOverviewResponse() }
      if (url.includes('/api/todos/stats/by-category')) return { ok: true, json: async () => createStatsCategoryResponse() }
      if (url.includes('/api/todos/stats/trend')) return { ok: true, json: async () => createStatsTrendResponse() }
      return { ok: true, json: async () => ({ ...createPageResponse(), data: { ...createPageResponse().data, content: [], totalElements: 0 } }) }
    });
    const wrapper = await mountTodoList()

    const emptyState = wrapper.find('.state-message')
    expect(emptyState.exists()).toBe(true)
    expect(emptyState.classes()).toContain('empty')

    const todoList = wrapper.find('.todo-list')
    expect(todoList.exists()).toBe(true)
  })
})
