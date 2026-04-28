import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'

import i18n from '../../i18n'
import { createAppRouter } from '../../router'
import { clearLastTasksPath, saveLastTasksPath } from '../../utils/taskRouteMemory'
import TodoStatisticsView from './TodoStatisticsView.vue'

const fetchMock = vi.fn()

function createSuccessResponse(data: unknown) {
  return {
    success: true,
    message: 'ok',
    timestamp: '2026-04-07T00:00:00',
    data,
  }
}

async function mountStatisticsView() {
  const router = createAppRouter(createMemoryHistory())
  await router.push('/statistics')
  await router.isReady()

  const wrapper = mount(TodoStatisticsView, {
    global: {
      plugins: [i18n, router],
    },
  })

  await flushPromises()
  return { wrapper, router }
}

describe('TodoStatisticsView', () => {
  beforeEach(() => {
    fetchMock.mockReset()
    clearLastTasksPath()
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockImplementation(async (url: string) => {
      if (url.includes('/api/todos/stats/overview')) {
        return { ok: true, json: async () => createSuccessResponse({
          todayCompleted: 2,
          weekCompleted: 7,
          overdueCount: 3,
          activeCount: 11,
          upcomingReminderCount: 5,
          unreadReminderCount: 4,
        }) }
      }
      if (url.includes('/api/todos/stats/by-category')) {
        return { ok: true, json: async () => createSuccessResponse([
          { category: 'Work', activeCount: 4, completedCount: 2 },
          { category: '__UNCLASSIFIED__', activeCount: 1, completedCount: 3 },
        ]) }
      }
      if (url.includes('/api/todos/stats/due-buckets')) {
        return { ok: true, json: async () => createSuccessResponse({
          overdue: 1, dueToday: 2, dueIn3Days: 0, dueIn7Days: 0, noDueDate: 1, totalActive: 4
        }) }
      }
      if (url.includes('/api/todos/stats/priority-distribution')) {
        return { ok: true, json: async () => createSuccessResponse({
          items: [{ priority: 5, count: 2 }, { priority: 2, count: 2 }],
          totalActive: 4
        }) }
      }
      if (url.includes('/api/todos/stats/aging')) {
        return { ok: true, json: async () => createSuccessResponse({
          buckets: [{ label: '0-3 days', count: 5 }],
          totalPending: 5
        }) }
      }
      if (url.includes('/api/todo-reminders/stats/summary')) {
        return { ok: true, json: async () => createSuccessResponse({
          unreadCount: 4, readTodayCount: 2, scheduledCount: 1, overdueReminderCount: 0
        }) }
      }
      if (url.includes('/api/todos/stats/recurrence-distribution')) {
        return { ok: true, json: async () => createSuccessResponse({
          items: [{ recurrenceType: 'DAILY', count: 3 }],
          totalActive: 3
        }) }
      }

      return { ok: true, json: async () => createSuccessResponse({
        range: '7d',
        items: [
          { date: '2026-04-01', createdCount: 1, completedCount: 1 },
          { date: '2026-04-02', createdCount: 0, completedCount: 2 },
        ],
        summary: {
          totalCreated: 1,
          totalCompleted: 3,
          netChange: -2,
          completionRate: 0.85,
        }
      }) }
    })
  })

  it('renders statistics page in main content without right sidebar', async () => {
    const { wrapper } = await mountStatisticsView()

    expect(wrapper.find('.workbench-layout').exists()).toBe(true)
    expect(wrapper.find('.workbench-sidebar').exists()).toBe(false)
    expect(wrapper.find('[data-testid="statistics-page-content"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="page-stats-dashboard"]').exists()).toBe(true)
    expect(wrapper.find('.workbench-header-title').text()).toBe('Statistics')
  })

  it('loads all statistics endpoints on mount', async () => {
    await mountStatisticsView()

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
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos/stats/due-buckets',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos/stats/priority-distribution',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos/stats/aging',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todo-reminders/stats/summary',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos/stats/recurrence-distribution',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      }),
    )
  })

  it('marks statistics navigation active and returns to tasks route', async () => {
    const { wrapper, router } = await mountStatisticsView()

    const statisticsButton = wrapper.findAll('.workbench-menu-button').find((button) => button.text().includes('Statistics'))
    const activeTasksButton = wrapper.findAll('.workbench-menu-button').find((button) => button.text().includes('Active Tasks'))

    expect(statisticsButton?.classes()).toContain('is-active')

    await activeTasksButton!.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/tasks')
  })

  it('restores the last remembered tasks path when returning from statistics', async () => {
    saveLastTasksPath('/tasks?displayMode=CALENDAR&keyword=retro&options=1')

    const { wrapper, router } = await mountStatisticsView()
    const activeTasksButton = wrapper.findAll('.workbench-menu-button').find((button) => button.text().includes('Active Tasks'))

    await activeTasksButton!.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/tasks?displayMode=CALENDAR&keyword=retro&options=1')
  })

  it('refreshes statistics when the refresh button is clicked', async () => {
    const { wrapper } = await mountStatisticsView()
    const beforeCount = fetchMock.mock.calls.length

    await wrapper.find('[data-testid="statistics-refresh-button"]').trigger('click')
    await flushPromises()

    expect(fetchMock.mock.calls.length).toBeGreaterThan(beforeCount)
  })

  it('renders page-level statistics semantics for ratio completion rate and NONE recurrence', async () => {
    fetchMock.mockImplementation(async (url: string) => {
      if (url.includes('/api/todos/stats/overview')) {
        return { ok: true, json: async () => createSuccessResponse({
          todayCompleted: 2,
          weekCompleted: 7,
          overdueCount: 3,
          activeCount: 11,
          upcomingReminderCount: 5,
          unreadReminderCount: 4,
        }) }
      }
      if (url.includes('/api/todos/stats/by-category')) {
        return { ok: true, json: async () => createSuccessResponse([
          { category: 'Work', activeCount: 4, completedCount: 2 },
        ]) }
      }
      if (url.includes('/api/todos/stats/due-buckets')) {
        return { ok: true, json: async () => createSuccessResponse({
          overdue: 1, dueToday: 2, dueIn3Days: 0, dueIn7Days: 0, noDueDate: 1, totalActive: 4,
        }) }
      }
      if (url.includes('/api/todos/stats/priority-distribution')) {
        return { ok: true, json: async () => createSuccessResponse({
          items: [{ priority: 5, count: 2 }, { priority: 2, count: 2 }],
          totalActive: 4,
        }) }
      }
      if (url.includes('/api/todos/stats/aging')) {
        return { ok: true, json: async () => createSuccessResponse({
          buckets: [{ label: '0-3 days', count: 2 }],
          totalPending: 3,
        }) }
      }
      if (url.includes('/api/todo-reminders/stats/summary')) {
        return { ok: true, json: async () => createSuccessResponse({
          unreadCount: 4, readTodayCount: 2, scheduledCount: 1, overdueReminderCount: 0,
        }) }
      }
      if (url.includes('/api/todos/stats/recurrence-distribution')) {
        return { ok: true, json: async () => createSuccessResponse({
          items: [{ recurrenceType: 'NONE', count: 4 }, { recurrenceType: 'DAILY', count: 1 }],
          totalActive: 10,
        }) }
      }

      return { ok: true, json: async () => createSuccessResponse({
        range: '7d',
        items: [
          { date: '2026-04-01', createdCount: 1, completedCount: 1 },
        ],
        summary: {
          totalCreated: 3,
          totalCompleted: 2,
          netChange: 1,
          completionRate: 0.6667,
        },
      }) }
    })

    const { wrapper } = await mountStatisticsView()

    expect(wrapper.text()).toContain('67%')
    expect(wrapper.text()).not.toContain('0.6667')
    expect(wrapper.text()).toContain('None')
  })

  it('updates trend range and refetches stats', async () => {
    const { wrapper } = await mountStatisticsView()
    const beforeCount = fetchMock.mock.calls.length

    const select = wrapper.find('select.trend-range-select')
    await select.setValue('30d')
    await select.trigger('change')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/todos/stats/trend?range=30d',
      expect.any(Object)
    )
    expect(fetchMock.mock.calls.length).toBeGreaterThan(beforeCount)
  })

  it('navigates to tasks page with filters when drill-down is clicked', async () => {
    const { wrapper, router } = await mountStatisticsView()

    // Mock priority click
    const priorityItem = wrapper.findAll('.priority-section .dist-item')[0]
    await priorityItem.trigger('click')
    await flushPromises()

    // It should navigate to /tasks?priority=5
    expect(router.currentRoute.value.fullPath).toContain('/tasks')
    expect(router.currentRoute.value.fullPath).toContain('status=PENDING')
    expect(router.currentRoute.value.fullPath).toContain('priority=5')
  })

  it('uses date-range overdue drill-down semantics instead of the realtime overdue preset', async () => {
    const { wrapper, router } = await mountStatisticsView()

    const dueItem = wrapper.findAll('.due-section .dist-item')[0]
    await dueItem.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toContain('/tasks')
    expect(router.currentRoute.value.fullPath).toContain('status=PENDING')
    expect(router.currentRoute.value.fullPath).toContain('dueDateTo=')
    expect(router.currentRoute.value.fullPath).not.toContain('timePreset=OVERDUE')
  })

  it('navigates recurrence drill-down with active-task scope', async () => {
    const { wrapper, router } = await mountStatisticsView()

    const recurrenceItem = wrapper.findAll('.recurrence-section .dist-item')[0]
    await recurrenceItem.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toContain('/tasks')
    expect(router.currentRoute.value.fullPath).toContain('status=PENDING')
    expect(router.currentRoute.value.fullPath).toContain('recurrenceType=DAILY')
  })
})
