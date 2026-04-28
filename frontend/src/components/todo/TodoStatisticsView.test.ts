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
})
