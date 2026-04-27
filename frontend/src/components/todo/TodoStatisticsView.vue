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
import type { TodoStatsOverview, TodoStatsCategoryItem, TodoStatsTrendItem } from './types'

const router = useRouter()
const { t } = useI18n()

const overview = ref<TodoStatsOverview | null>(null)
const categories = ref<TodoStatsCategoryItem[]>([])
const trend = ref<TodoStatsTrendItem[]>([])
const loading = ref(false)
const errorMessage = ref('')

function buildTasksQuery(payload: {
  viewMode?: TodoViewMode
  displayMode?: TodoDisplayMode
  showOptionsPanel?: boolean
}) {
  const queryString = serializeTodoUrlState({
    filters: createDefaultTodoFilters(),
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
    const [overviewRes, categoryRes, trendRes] = await Promise.all([
      fetchApi<TodoStatsOverview>('/api/todos/stats/overview'),
      fetchApi<TodoStatsCategoryItem[]>('/api/todos/stats/by-category'),
      fetchApi<{ range: string; items: TodoStatsTrendItem[] }>('/api/todos/stats/trend?range=7d'),
    ])

    overview.value = overviewRes.data || null
    categories.value = categoryRes.data || []
    trend.value = trendRes.data?.items || []
  } catch (error) {
    overview.value = null
    categories.value = []
    trend.value = []
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

onMounted(() => {
  void loadStats()
})
</script>

<template>
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
        page-mode
      />
    </div>
  </TodoWorkbenchLayout>
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
