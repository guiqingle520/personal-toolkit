<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

import { fetchApi } from '../../api'
import {
  createDefaultTodoFilters,
  serializeTodoUrlState,
  type TodoDisplayMode,
  type TodoViewMode,
} from '../../utils/todoView'
import { getLastTasksPath } from '../../utils/taskRouteMemory'
import TodoWorkbenchLayout from './TodoWorkbenchLayout.vue'
import TodoWorkbenchHeader from './TodoWorkbenchHeader.vue'
import TodoSidebarNav from './TodoSidebarNav.vue'
import TodoStatsPanel from './TodoStatsPanel.vue'
import type { 
  TodoStatsOverview, 
  TodoStatsCategoryItem, 
  TodoStatsTrendItem,
  TodoStatsTrendSummary,
  TodoStatsDueBuckets,
  TodoStatsPriorityDistribution,
  TodoStatsAging,
  TodoReminderSummary,
  TodoStatsRecurrenceDistribution
} from './types'

const router = useRouter()
const { t } = useI18n()

const overview = ref<TodoStatsOverview | null>(null)
const categories = ref<TodoStatsCategoryItem[]>([])
const trend = ref<TodoStatsTrendItem[]>([])
const trendSummary = ref<TodoStatsTrendSummary | undefined>(undefined)
const dueBuckets = ref<TodoStatsDueBuckets | null>(null)
const priorityDistribution = ref<TodoStatsPriorityDistribution | null>(null)
const aging = ref<TodoStatsAging | null>(null)
const reminderSummary = ref<TodoReminderSummary | null>(null)
const recurrenceDistribution = ref<TodoStatsRecurrenceDistribution | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const trendRange = ref('7d')

function buildTasksQuery(payload: {
  viewMode?: TodoViewMode
  displayMode?: TodoDisplayMode
  showOptionsPanel?: boolean
  overrides?: Partial<ReturnType<typeof createDefaultTodoFilters>>
}) {
  const filters = createDefaultTodoFilters()
  if (payload.overrides) {
    Object.assign(filters, payload.overrides)
  }
  const queryString = serializeTodoUrlState({
    filters,
    viewMode: payload.viewMode ?? 'ACTIVE',
    displayMode: payload.displayMode ?? 'LIST',
  })

  const params = new URLSearchParams(queryString)
  if (payload.showOptionsPanel) {
    params.set('options', '1')
  }

  return Object.fromEntries(params.entries())
}

async function loadStats() {
  loading.value = true
  errorMessage.value = ''

  try {
    const [overviewRes, categoryRes, trendRes, dueBucketsRes, priorityDistRes, agingRes, reminderRes, recurrenceRes] = await Promise.all([
      fetchApi<TodoStatsOverview>('/api/todos/stats/overview'),
      fetchApi<TodoStatsCategoryItem[]>('/api/todos/stats/by-category'),
      fetchApi<{ range: string; items: TodoStatsTrendItem[]; summary: TodoStatsTrendSummary }>(`/api/todos/stats/trend?range=${trendRange.value}`),
      fetchApi<TodoStatsDueBuckets>('/api/todos/stats/due-buckets'),
      fetchApi<TodoStatsPriorityDistribution>('/api/todos/stats/priority-distribution'),
      fetchApi<TodoStatsAging>('/api/todos/stats/aging'),
      fetchApi<TodoReminderSummary>('/api/todo-reminders/stats/summary'),
      fetchApi<TodoStatsRecurrenceDistribution>('/api/todos/stats/recurrence-distribution'),
    ])

    overview.value = overviewRes.data || null
    categories.value = categoryRes.data || []
    trend.value = trendRes.data?.items || []
    trendSummary.value = trendRes.data?.summary
    dueBuckets.value = dueBucketsRes.data || null
    priorityDistribution.value = priorityDistRes.data || null
    aging.value = agingRes.data || null
    reminderSummary.value = reminderRes.data || null
    recurrenceDistribution.value = recurrenceRes.data || null
  } catch (error) {
    overview.value = null
    categories.value = []
    trend.value = []
    trendSummary.value = undefined
    dueBuckets.value = null
    priorityDistribution.value = null
    aging.value = null
    reminderSummary.value = null
    recurrenceDistribution.value = null
    errorMessage.value = error instanceof Error ? error.message : t('feedback.unexpectedError')
  } finally {
    loading.value = false
  }
}

function handleNavigateStatistics() {
  void router.push({ name: 'statistics' })
}

function handleNavigateTasks(payload: {
  viewMode?: TodoViewMode
  displayMode?: TodoDisplayMode
  showOptionsPanel?: boolean
}) {
  const lastTasksPath = getLastTasksPath()
  if (lastTasksPath) {
    void router.push(lastTasksPath)
    return
  }

  void router.push({
    name: 'tasks',
    query: buildTasksQuery(payload),
  })
}

function handleDrillDown(overrides: Partial<ReturnType<typeof createDefaultTodoFilters>>) {
  void router.push({
    name: 'tasks',
    query: buildTasksQuery({ overrides }),
  })
}

function handleTrendRangeUpdate(range: string) {
  trendRange.value = range
  void loadStats()
}

function getLocalIsoDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function handleDueClick(bucketKey: string) {
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())

  if (bucketKey === 'bucketOverdue') {
    const yesterday = new Date(startOfToday)
    yesterday.setDate(startOfToday.getDate() - 1)
    handleDrillDown({
      status: 'PENDING',
      dueDateTo: getLocalIsoDate(yesterday),
    })
  } else if (bucketKey === 'bucketToday') {
    handleDrillDown({ status: 'PENDING', timePreset: 'DUE_TODAY' })
  } else if (bucketKey === 'bucket3Days') {
    const day1 = new Date(now)
    day1.setDate(now.getDate() + 1)
    const day3 = new Date(now)
    day3.setDate(now.getDate() + 3)
    handleDrillDown({
      status: 'PENDING',
      dueDateFrom: getLocalIsoDate(day1),
      dueDateTo: getLocalIsoDate(day3),
    })
  } else if (bucketKey === 'bucket7Days') {
    const day4 = new Date(now)
    day4.setDate(now.getDate() + 4)
    const day7 = new Date(now)
    day7.setDate(now.getDate() + 7)
    handleDrillDown({
      status: 'PENDING',
      dueDateFrom: getLocalIsoDate(day4),
      dueDateTo: getLocalIsoDate(day7),
    })
  }
}

function handlePriorityClick(priority: number) {
  handleDrillDown({ status: 'PENDING', priority: priority.toString() })
}

function handleRecurrenceClick(recurrenceType: string) {
  handleDrillDown({ status: 'PENDING', recurrenceType })
}

onMounted(() => {
  void loadStats()
})
</script>

<template>
  <section class="todo-panel">
    <div class="glass-bg"></div>
    <div class="content-wrapper">
      <TodoWorkbenchLayout>
        <template #header>
          <TodoWorkbenchHeader :title="t('app.statistics')">
            <template #actions>
              <button
                type="button"
                class="btn btn-outline"
                data-testid="statistics-refresh-button"
                :disabled="loading"
                @click="loadStats"
              >
                <span class="icon">↻</span>
                {{ loading ? t('app.syncing') : t('app.refresh') }}
              </button>
            </template>
          </TodoWorkbenchHeader>
        </template>

        <template #menu>
          <TodoSidebarNav
            route-name="statistics"
            @navigate:statistics="handleNavigateStatistics"
            @navigate:tasks="handleNavigateTasks"
          />
        </template>

        <div class="statistics-page-content" data-testid="statistics-page-content">
          <div v-if="errorMessage" class="error-banner">
            <strong>{{ t('status.error') }}</strong> {{ errorMessage }}
          </div>

          <div v-else-if="loading && !overview" class="state-message">
            {{ t('app.syncing') }}
          </div>

          <TodoStatsPanel
            v-else
            :overview="overview"
            :categories="categories"
            :trend="trend"
            :trend-summary="trendSummary"
            :due-buckets="dueBuckets"
            :priority-distribution="priorityDistribution"
            :aging="aging"
            :reminder-summary="reminderSummary"
            :recurrence-distribution="recurrenceDistribution"
            :trend-range="trendRange"
            page-mode
            @update:trend-range="handleTrendRangeUpdate"
            @click:due="handleDueClick"
            @click:priority="handlePriorityClick"
            @click:recurrence="handleRecurrenceClick"
          />
        </div>
      </TodoWorkbenchLayout>
    </div>
  </section>
</template>

<style scoped>
.statistics-page-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.icon {
  margin-right: 6px;
}
</style>
