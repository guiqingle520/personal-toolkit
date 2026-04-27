<script setup lang="ts">
import type { TodoDisplayMode, TodoViewMode } from '../../utils/todoView'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = defineProps<{
  routeName: 'tasks' | 'statistics'
  viewMode?: TodoViewMode
  displayMode?: TodoDisplayMode
  showOptionsPanel?: boolean
}>()

const emit = defineEmits<{
  (e: 'navigate:statistics'): void
  (e: 'navigate:tasks', payload: {
    viewMode?: TodoViewMode
    displayMode?: TodoDisplayMode
    showOptionsPanel?: boolean
  }): void
  (e: 'update:viewMode', value: TodoViewMode): void
  (e: 'update:displayMode', value: TodoDisplayMode): void
  (e: 'update:showOptionsPanel', value: boolean): void
}>()

function activateTasks(viewMode: TodoViewMode) {
  if (props.routeName === 'tasks') {
    emit('update:viewMode', viewMode)
    return
  }

  emit('navigate:tasks', { viewMode, displayMode: 'LIST' })
}

function activateDisplay(displayMode: TodoDisplayMode) {
  if (props.routeName === 'tasks') {
    emit('update:displayMode', displayMode)
    return
  }

  emit('navigate:tasks', { viewMode: 'ACTIVE', displayMode })
}

function openStatistics() {
  emit('navigate:statistics')
}

function toggleOptionsPanel() {
  if (props.routeName === 'tasks') {
    emit('update:showOptionsPanel', !props.showOptionsPanel)
    return
  }

  emit('navigate:tasks', {
    viewMode: 'ACTIVE',
    displayMode: 'LIST',
    showOptionsPanel: true,
  })
}
</script>

<template>
  <nav class="workbench-menu-panel" :aria-label="t('app.tasks')">
    <div class="workbench-menu-section">
      <div class="workbench-menu-label">{{ t('app.tasks') }}</div>
      <div class="workbench-menu-group">
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active': routeName === 'tasks' && viewMode === 'ACTIVE' }"
          :aria-pressed="routeName === 'tasks' && viewMode === 'ACTIVE'"
          @click="activateTasks('ACTIVE')"
        >
          {{ t('app.activeTasks') }}
        </button>
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active': routeName === 'tasks' && viewMode === 'RECYCLE_BIN' }"
          :aria-pressed="routeName === 'tasks' && viewMode === 'RECYCLE_BIN'"
          @click="activateTasks('RECYCLE_BIN')"
        >
          {{ t('app.recycleBin') }}
        </button>
      </div>
    </div>

    <div class="workbench-menu-section">
      <div class="workbench-menu-label">{{ t('app.statistics') }}</div>
      <div class="workbench-menu-group">
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active': routeName === 'statistics' }"
          :aria-pressed="routeName === 'statistics'"
          @click="openStatistics"
        >
          {{ t('app.statistics') }}
        </button>
      </div>
    </div>

    <div class="workbench-menu-section">
      <div class="workbench-menu-label">{{ t('app.views') }}</div>
      <div class="workbench-menu-group">
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active': routeName === 'tasks' && displayMode === 'LIST' }"
          :aria-pressed="routeName === 'tasks' && displayMode === 'LIST'"
          @click="activateDisplay('LIST')"
        >
          {{ t('app.listView') }}
        </button>
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active': routeName === 'tasks' && displayMode === 'KANBAN' }"
          :aria-pressed="routeName === 'tasks' && displayMode === 'KANBAN'"
          :disabled="routeName === 'tasks' && viewMode !== 'ACTIVE'"
          @click="activateDisplay('KANBAN')"
        >
          {{ t('app.kanbanView') }}
        </button>
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active': routeName === 'tasks' && displayMode === 'CALENDAR' }"
          :aria-pressed="routeName === 'tasks' && displayMode === 'CALENDAR'"
          :disabled="routeName === 'tasks' && viewMode !== 'ACTIVE'"
          @click="activateDisplay('CALENDAR')"
        >
          {{ t('app.calendarView') }}
        </button>
      </div>
    </div>

    <div class="workbench-menu-section">
      <div class="workbench-menu-label">{{ t('app.settings') }}</div>
      <div class="workbench-menu-group">
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active': routeName === 'tasks' && showOptionsPanel }"
          :aria-pressed="routeName === 'tasks' && showOptionsPanel"
          @click="toggleOptionsPanel"
        >
          {{ t('app.manageCategories') }}
        </button>
      </div>
    </div>
  </nav>
</template>
