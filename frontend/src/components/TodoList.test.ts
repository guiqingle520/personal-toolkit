import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'

import TodoList from './TodoList.vue'
import i18n from '../i18n'
import { syncDocumentLocale } from '../i18n'
import { createAppRouter } from '../router'
import { getLastTasksPath } from '../utils/taskRouteMemory'

const fetchMock = vi.fn()
const promptMock = vi.fn()
const confirmMock = vi.fn()

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
          remindAt: '2026-04-08T07:30:00',
          category: 'Work',
          tags: 'backend',
          notes: 'Draft release notes',
          attachmentLinks: 'https://example.com/spec',
          ownerLabel: 'Alice',
          collaborators: 'Bob,Carol',
          watchers: 'Dave',
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

function createReminderPageResponse() {
  return {
    success: true,
    message: 'ok',
    timestamp: '2026-04-07T00:00:00',
    data: {
      content: [
        {
          id: 201,
          todoId: 1,
          todoTitle: 'Alpha',
          scheduledAt: '2026-04-08T07:30:00',
          status: 'SENT',
          category: 'Work',
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

function createSavedViewsResponse() {
  return {
    success: true,
    message: 'ok',
    timestamp: '2026-04-07T00:00:00',
    data: [
      {
        id: 301,
        name: 'Ops Focus',
        isDefault: true,
        filters: { status: 'PENDING', keyword: 'Alpha' },
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

async function mountTodoList(route = '/tasks', options: { attachTo?: HTMLElement } = {}) {
  const router = createAppRouter(createMemoryHistory())
  await router.push(route)
  await router.isReady()

  const wrapper = mount(TodoList, {
    attachTo: options.attachTo,
    global: {
      plugins: [i18n, router],
      stubs: { Teleport: true },
    },
  })

  await flushPromises()
  return { wrapper, router }
}

function createFetchImplementation() {
  return async (url: string, options?: RequestInit) => {
    if (url.includes('/api/todo-saved-views/') && url.includes('/default')) {
      return { ok: true, json: async () => createSuccessResponse({ id: 301 }) }
    }
    if (url.includes('/api/todo-saved-views') && options?.method === 'POST') {
      return { ok: true, json: async () => createSuccessResponse({ id: 302 }) }
    }
    if (url.includes('/api/todo-saved-views')) {
      return { ok: true, json: async () => createSavedViewsResponse() }
    }
    if (url.includes('/api/todos/options')) {
      return { ok: true, json: async () => createOptionsResponse() }
    }
    if (url.includes('/api/todo-reminders/read-all')) {
      return { ok: true, json: async () => createSuccessResponse(null) }
    }
    if (url.includes('/api/todo-reminders/') && url.includes('/read')) {
      return { ok: true, json: async () => createSuccessResponse(null) }
    }
    if (url.includes('/api/todo-reminders')) {
      return { ok: true, json: async () => createReminderPageResponse() }
    }
    if (url.includes('/api/todos') && options?.method === 'POST') {
      return { ok: true, json: async () => createSuccessResponse({ id: 2, title: 'Hidden todo' }) }
    }
    if (url.includes('/api/todos/1/restore')) {
      return { ok: true, json: async () => createSuccessResponse({ id: 1 }) }
    }
    if (url.includes('/api/todos/') && options?.method === 'PUT') {
      return { ok: true, json: async () => createSuccessResponse({ id: 1 }) }
    }
    if (url.includes('/api/todos/') && options?.method === 'DELETE') {
      return { ok: true, json: async () => createSuccessResponse(null) }
    }
    if (url.includes('/api/todos/batch/')) {
      return { ok: true, json: async () => createSuccessResponse(null) }
    }

    return { ok: true, json: async () => createPageResponse() }
  }
}

describe('TodoList route-aware workbench', () => {
  beforeEach(() => {
    fetchMock.mockReset()
    promptMock.mockReset()
    confirmMock.mockReset()
    sessionStorage.clear()
    vi.stubGlobal('fetch', fetchMock)
    vi.stubGlobal('prompt', promptMock)
    vi.stubGlobal('confirm', confirmMock)
    confirmMock.mockReturnValue(true)
    fetchMock.mockImplementation(createFetchImplementation())
  })

  it('renders header, menu, main, and sidebar regions through TodoWorkbenchLayout', async () => {
    const { wrapper } = await mountTodoList()

    expect(wrapper.find('.workbench-layout').exists()).toBe(true)
    expect(wrapper.find('.workbench-top').exists()).toBe(true)
    expect(wrapper.find('.workbench-menu').exists()).toBe(true)
    expect(wrapper.find('.workbench-main').exists()).toBe(true)
    expect(wrapper.find('.workbench-sidebar').exists()).toBe(true)

    expect(wrapper.findComponent({ name: 'TodoToolbar' }).exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'TodoReminderPanel' }).exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'TodoSavedViewsBar' }).exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'TodoFilters' }).exists()).toBe(true)
  })

  it('applies default saved view before first list query', async () => {
    await mountTodoList()

    const todoCalls = fetchMock.mock.calls.filter(([url]) => typeof url === 'string' && url.startsWith('/api/todos?page='))
    expect(todoCalls[0]?.[0]).toBe('/api/todos?page=0&size=10&status=PENDING&keyword=Alpha&sortBy=createTime&sortDir=DESC')
  })

  it('uses route query ahead of default saved view on first load', async () => {
    await mountTodoList('/tasks?status=DONE&keyword=retro')

    const todoCalls = fetchMock.mock.calls.filter(([url]) => typeof url === 'string' && url.startsWith('/api/todos?page='))
    expect(todoCalls[0]?.[0]).toBe('/api/todos?page=0&size=10&status=DONE&keyword=retro&sortBy=createTime&sortDir=DESC')
  })

  it('keeps the left menu actions wired to task state transitions', async () => {
    const { wrapper } = await mountTodoList()

    const menuButtons = wrapper.findAll('.workbench-menu-button')
    const recycleButton = menuButtons.find((button) => button.text().includes('Recycle Bin'))
    const activeButton = menuButtons.find((button) => button.text().includes('Active Tasks'))

    await recycleButton!.trigger('click')
    await flushPromises()
    expect(wrapper.vm.viewMode).toBe('RECYCLE_BIN')

    await activeButton!.trigger('click')
    await flushPromises()
    expect(wrapper.vm.viewMode).toBe('ACTIVE')
  })

  it('toggles the options panel from the left menu and persists it to route query', async () => {
    const { wrapper, router } = await mountTodoList()

    const manageCategoriesButton = wrapper.findAll('.workbench-menu-button').find((button) => button.text().includes('Manage Categories'))
    await manageCategoriesButton!.trigger('click')
    await flushPromises()

    expect(wrapper.findComponent({ name: 'TodoOptionsPanel' }).props('show')).toBe(true)
    expect(router.currentRoute.value.query.options).toBe('1')
  })

  it('restores the options panel when opening tasks route with options query', async () => {
    const { wrapper, router } = await mountTodoList('/tasks?options=1')

    expect(wrapper.findComponent({ name: 'TodoOptionsPanel' }).props('show')).toBe(true)
    expect(router.currentRoute.value.query.options).toBe('1')
  })

  it('syncs display-related filters into router query', async () => {
    const { wrapper, router } = await mountTodoList()

    const menuButtons = wrapper.findAll('.workbench-menu-button')
    const calendarButton = menuButtons.find((button) => button.text().includes('Calendar View'))

    await calendarButton!.trigger('click')
    await flushPromises()

    expect(wrapper.find('.calendar-board').exists()).toBe(true)
    expect(router.currentRoute.value.query.displayMode).toBe('CALENDAR')
  })

  it('resets calendar display mode back to list when switching to recycle bin', async () => {
    const { wrapper, router } = await mountTodoList()

    const menuButtons = wrapper.findAll('.workbench-menu-button')
    const calendarButton = menuButtons.find((button) => button.text().includes('Calendar View'))
    const recycleButton = menuButtons.find((button) => button.text().includes('Recycle Bin'))

    await calendarButton!.trigger('click')
    await flushPromises()
    await recycleButton!.trigger('click')
    await flushPromises()

    expect(wrapper.find('.calendar-board').exists()).toBe(false)
    expect(wrapper.find('.todo-list').exists()).toBe(true)
    expect(wrapper.vm.displayMode).toBe('LIST')
    expect(router.currentRoute.value.query.viewMode).toBe('RECYCLE_BIN')
    expect(router.currentRoute.value.query.displayMode).toBeUndefined()
  })

  it('navigates to statistics route from the sidebar navigation', async () => {
    const { wrapper, router } = await mountTodoList()

    const statisticsButton = wrapper.findAll('.workbench-menu-button').find((button) => button.text().includes('Statistics'))
    await statisticsButton!.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/statistics')
    expect(getLastTasksPath()).toBe('/tasks?status=PENDING&keyword=Alpha')
  })

  it('loads reminder panel and saved views on mount', async () => {
    await mountTodoList()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todo-reminders?status=SENT&page=0&size=10',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todo-saved-views',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
  })

  it('shows recovery action when created todo is hidden by active filters', async () => {
    const { wrapper } = await mountTodoList()

    const statusSelect = wrapper.find('.filter-section select')
    await statusSelect.setValue('DONE')
    await flushPromises()

    await wrapper.find('.list-actions-bar button').trigger('click')
    const createRows = wrapper.findAll('.create-form .create-row')
    const titleInput = createRows[0].find('input[type="text"]')
    const addButton = createRows[1].find('button.btn-primary')

    await titleInput.setValue('Hidden todo')
    await addButton.trigger('click')
    await flushPromises()

    expect(wrapper.vm.hiddenCreatedTodoId).toBeTruthy()
    expect(wrapper.text()).toContain('The task was created, but it is hidden by the current filters')

    await wrapper.find('.info-banner-action').trigger('click')
    expect(wrapper.vm.hiddenCreatedTodoId).toBe(null)
    expect(wrapper.vm.filters.status).toBe('')
  })

  it('prevents submitting reminder date later than due date', async () => {
    const { wrapper } = await mountTodoList()
    await wrapper.find('.list-actions-bar button').trigger('click')
    const createRows = wrapper.findAll('.create-form .create-row')
    const titleInput = createRows[0].find('input[type="text"]')
    const dueDateInput = createRows[1].find('.localized-date-input-wrapper input')
    const remindAtInput = createRows[1].findAll('.localized-date-input-wrapper input')[1]
    const addButton = createRows[1].find('button.btn-primary')

    await titleInput.setValue('Broken reminder window')
    await dueDateInput.setValue('2026-04-09')
    await dueDateInput.trigger('input')
    await remindAtInput.setValue('2026-04-10')
    await remindAtInput.trigger('input')
    await addButton.trigger('click')
    await flushPromises()

    const createCall = fetchMock.mock.calls.find(([url, options]) => url === '/api/todos' && options?.method === 'POST')
    expect(createCall).toBeTruthy()
    expect(JSON.parse(String(createCall?.[1]?.body)).remindAt).toBe('2026-04-09T00:00:00')
  })

  it('restores filters from router query changes', async () => {
    const { wrapper, router } = await mountTodoList()

    await router.push('/tasks?status=DONE&keyword=retro')
    await flushPromises()

    expect(wrapper.vm.filters).toMatchObject({
      status: 'DONE',
      keyword: 'retro',
    })
  })

  it('opens and closes the create modal via button and close button', async () => {
    const { wrapper } = await mountTodoList()
    
    expect(wrapper.find('.todo-create-modal').exists()).toBe(false)
    
    await wrapper.find('.list-actions-bar button').trigger('click')
    expect(wrapper.find('.todo-create-modal').exists()).toBe(true)
    
    await wrapper.find('.todo-create-modal .modal-close').trigger('click')
    expect(wrapper.find('.todo-create-modal').exists()).toBe(false)
  })

  it('closes the create modal with escape and restores focus to the trigger', async () => {
    const { wrapper } = await mountTodoList('/tasks', { attachTo: document.body })

    const trigger = wrapper.find('.list-actions-bar button')
    const focusSpy = vi.spyOn(trigger.element as HTMLButtonElement, 'focus')
    await trigger.trigger('click')
    await flushPromises()
    expect(wrapper.find('.todo-create-modal').exists()).toBe(true)

    await wrapper.find('.todo-create-modal').trigger('keydown', { key: 'Escape' })
    await flushPromises()

    expect(wrapper.find('.todo-create-modal').exists()).toBe(false)
    expect(focusSpy).toHaveBeenCalled()

    wrapper.unmount()
  })

  it('keeps focus trapped inside the create modal when tabbing past the last focusable element', async () => {
    const { wrapper } = await mountTodoList('/tasks', { attachTo: document.body })

    await wrapper.find('.list-actions-bar button').trigger('click')
    await flushPromises()

    const focusableElements = wrapper.findAll('.todo-create-modal input, .todo-create-modal select, .todo-create-modal textarea, .todo-create-modal button')
    const firstElement = focusableElements[0]?.element as HTMLElement
    const lastElement = focusableElements[focusableElements.length - 1]?.element as HTMLElement

    lastElement.focus()
    await wrapper.find('.todo-create-modal').trigger('keydown', { key: 'Tab' })
    expect(document.activeElement).toBe(firstElement)

    firstElement.focus()
    await wrapper.find('.todo-create-modal').trigger('keydown', { key: 'Tab', shiftKey: true })
    expect(document.activeElement).toBe(lastElement)

    wrapper.unmount()
  })

  it('focuses the title field when the create modal opens', async () => {
    const { wrapper } = await mountTodoList('/tasks', { attachTo: document.body })

    await wrapper.find('.list-actions-bar button').trigger('click')
    await flushPromises()

    const titleInput = wrapper.find('.todo-create-modal input[type="text"]')
    expect(document.activeElement).toBe(titleInput.element)

    wrapper.unmount()
  })

  it('does not force-close the create modal with escape while submitting', async () => {
    let resolveCreateRequest: (() => void) | null = null
    fetchMock.mockImplementation(async (url: string, options?: RequestInit) => {
      if (url.includes('/api/todos') && options?.method === 'POST') {
        await new Promise<void>((resolve) => {
          resolveCreateRequest = resolve
        })
        return { ok: true, json: async () => createSuccessResponse({ id: 2, title: 'Pending create' }) }
      }

      return createFetchImplementation()(url, options)
    })

    const { wrapper } = await mountTodoList('/tasks', { attachTo: document.body })

    await wrapper.find('.list-actions-bar button').trigger('click')
    await wrapper.find('.todo-create-modal input[type="text"]').setValue('Pending create')
    await wrapper.find('.todo-create-modal .btn.btn-primary').trigger('click')
    await flushPromises()

    expect(wrapper.vm.submitting).toBe(true)

    await wrapper.find('.todo-create-modal').trigger('keydown', { key: 'Escape' })
    await flushPromises()

    expect(wrapper.find('.todo-create-modal').exists()).toBe(true)

    resolveCreateRequest?.()
    await flushPromises()
    await flushPromises()

    wrapper.unmount()
  })

  it('does not close the create modal from backdrop click while submitting', async () => {
    let resolveCreateRequest: (() => void) | null = null
    fetchMock.mockImplementation(async (url: string, options?: RequestInit) => {
      if (url.includes('/api/todos') && options?.method === 'POST') {
        await new Promise<void>((resolve) => {
          resolveCreateRequest = resolve
        })
        return { ok: true, json: async () => createSuccessResponse({ id: 2, title: 'Pending create' }) }
      }

      return createFetchImplementation()(url, options)
    })

    const { wrapper } = await mountTodoList('/tasks', { attachTo: document.body })

    await wrapper.find('.list-actions-bar button').trigger('click')
    await wrapper.find('.todo-create-modal input[type="text"]').setValue('Pending create')
    await wrapper.find('.todo-create-modal .btn.btn-primary').trigger('click')
    await flushPromises()

    await wrapper.find('.todo-create-modal-overlay').trigger('click')
    await flushPromises()

    expect(wrapper.find('.todo-create-modal').exists()).toBe(true)

    resolveCreateRequest?.()
    await flushPromises()
    await flushPromises()

    wrapper.unmount()
  })

  it('keeps modal open and shows inline error feedback when create request fails', async () => {
    fetchMock.mockImplementation(async (url: string, options?: RequestInit) => {
      if (url.includes('/api/todos') && options?.method === 'POST') {
        return {
          ok: false,
          json: async () => ({
            success: false,
            message: 'Create failed in modal',
            timestamp: '2026-04-07T00:00:00',
            validation: {
              title: ['Title already exists'],
            },
          }),
        }
      }

      return createFetchImplementation()(url, options)
    })

    const { wrapper } = await mountTodoList()

    await wrapper.find('.list-actions-bar button').trigger('click')
    const titleInput = wrapper.find('.todo-create-modal input[type="text"]')
    await titleInput.setValue('Broken create modal')
    await wrapper.find('.todo-create-modal .btn.btn-primary').trigger('click')
    await flushPromises()

    expect(wrapper.find('.todo-create-modal').exists()).toBe(true)
    expect(wrapper.find('.todo-create-modal .error-banner-inline').text()).toContain('Create failed in modal')
    expect((wrapper.find('.todo-create-modal input[type="text"]').element as HTMLInputElement).value).toBe('Broken create modal')
  })

  it('closes modal and clears draft after successful create', async () => {
    const { wrapper } = await mountTodoList()

    await wrapper.find('.list-actions-bar button').trigger('click')
    await wrapper.find('.todo-create-modal input[type="text"]').setValue('Ship modal create')
    await wrapper.find('.todo-create-modal .btn.btn-primary').trigger('click')
    await flushPromises()
    await flushPromises()

    expect(wrapper.vm.isCreateModalOpen).toBe(false)
    expect(wrapper.find('.todo-create-modal').exists()).toBe(false)
    expect(wrapper.vm.newTodo.title).toBe('')
  })

  it('syncs document lang when locale changes so native date inputs can follow app locale', () => {
    document.documentElement.lang = 'zh-CN'
    syncDocumentLocale('en')
    expect(document.documentElement.lang).toBe('en')
  })
})
