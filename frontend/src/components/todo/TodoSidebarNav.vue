<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
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

const expanded = ref({
  tasks: true,
  statistics: false,
  views: false,
  settings: false
})

function checkAutoExpand() {
  if (props.routeName === 'statistics') expanded.value.statistics = true
  if (props.routeName === 'tasks') {
    if (props.showOptionsPanel) expanded.value.settings = true
    else {
      expanded.value.tasks = true
      if (props.displayMode) expanded.value.views = true
    }
  }
}

onMounted(checkAutoExpand)
watch(() => [props.routeName, props.showOptionsPanel, props.displayMode], checkAutoExpand)

function toggleSection(section: keyof typeof expanded.value) {
  expanded.value[section] = !expanded.value[section]
}

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
      <button 
        type="button"
        class="workbench-menu-label" 
        :aria-expanded="expanded.tasks"
        aria-controls="menu-group-tasks"
        @click="toggleSection('tasks')"
      >
        <span class="workbench-menu-chevron" :class="{ 'is-expanded': expanded.tasks }">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </span>
        {{ t('app.tasks') }}
      </button>
      <div v-show="expanded.tasks" id="menu-group-tasks" class="workbench-menu-group">
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active is-active--route': routeName === 'tasks' && viewMode === 'ACTIVE' }"
          :aria-pressed="routeName === 'tasks' && viewMode === 'ACTIVE'"
          @click="activateTasks('ACTIVE')"
        >
          <svg class="leaf-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
          </svg>
          {{ t('app.activeTasks') }}
        </button>
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active is-active--route': routeName === 'tasks' && viewMode === 'RECYCLE_BIN' }"
          :aria-pressed="routeName === 'tasks' && viewMode === 'RECYCLE_BIN'"
          @click="activateTasks('RECYCLE_BIN')"
        >
          <svg class="leaf-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
          {{ t('app.recycleBin') }}
        </button>
      </div>
    </div>

    <div class="workbench-menu-section">
      <button 
        type="button"
        class="workbench-menu-label" 
        :aria-expanded="expanded.statistics"
        aria-controls="menu-group-statistics"
        @click="toggleSection('statistics')"
      >
        <span class="workbench-menu-chevron" :class="{ 'is-expanded': expanded.statistics }">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </span>
        {{ t('app.statistics') }}
      </button>
      <div v-show="expanded.statistics" id="menu-group-statistics" class="workbench-menu-group">
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active is-active--route': routeName === 'statistics' }"
          :aria-pressed="routeName === 'statistics'"
          @click="openStatistics"
        >
          <svg class="leaf-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="20" x2="18" y2="10"></line>
            <line x1="12" y1="20" x2="12" y2="4"></line>
            <line x1="6" y1="20" x2="6" y2="14"></line>
          </svg>
          {{ t('app.statistics') }}
        </button>
      </div>
    </div>

    <div class="workbench-menu-section">
      <button 
        type="button"
        class="workbench-menu-label" 
        :aria-expanded="expanded.views"
        aria-controls="menu-group-views"
        @click="toggleSection('views')"
      >
        <span class="workbench-menu-chevron" :class="{ 'is-expanded': expanded.views }">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </span>
        {{ t('app.views') }}
      </button>
      <div v-show="expanded.views" id="menu-group-views" class="workbench-menu-group">
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active is-active--view': routeName === 'tasks' && displayMode === 'LIST' }"
          :aria-pressed="routeName === 'tasks' && displayMode === 'LIST'"
          @click="activateDisplay('LIST')"
        >
          <svg class="leaf-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="8" y1="6" x2="21" y2="6"></line>
            <line x1="8" y1="12" x2="21" y2="12"></line>
            <line x1="8" y1="18" x2="21" y2="18"></line>
            <line x1="3" y1="6" x2="3.01" y2="6"></line>
            <line x1="3" y1="12" x2="3.01" y2="12"></line>
            <line x1="3" y1="18" x2="3.01" y2="18"></line>
          </svg>
          {{ t('app.listView') }}
        </button>
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active is-active--view': routeName === 'tasks' && displayMode === 'KANBAN' }"
          :aria-pressed="routeName === 'tasks' && displayMode === 'KANBAN'"
          :disabled="routeName === 'tasks' && viewMode !== 'ACTIVE'"
          @click="activateDisplay('KANBAN')"
        >
          <svg class="leaf-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="9" y1="3" x2="9" y2="21"></line>
            <line x1="15" y1="3" x2="15" y2="21"></line>
          </svg>
          {{ t('app.kanbanView') }}
        </button>
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active is-active--view': routeName === 'tasks' && displayMode === 'CALENDAR' }"
          :aria-pressed="routeName === 'tasks' && displayMode === 'CALENDAR'"
          :disabled="routeName === 'tasks' && viewMode !== 'ACTIVE'"
          @click="activateDisplay('CALENDAR')"
        >
          <svg class="leaf-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
          {{ t('app.calendarView') }}
        </button>
      </div>
    </div>

    <div class="workbench-menu-section">
      <button 
        type="button"
        class="workbench-menu-label" 
        :aria-expanded="expanded.settings"
        aria-controls="menu-group-settings"
        @click="toggleSection('settings')"
      >
        <span class="workbench-menu-chevron" :class="{ 'is-expanded': expanded.settings }">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </span>
        {{ t('app.settings') }}
      </button>
      <div v-show="expanded.settings" id="menu-group-settings" class="workbench-menu-group">
        <button
          type="button"
          class="btn btn-outline workbench-menu-button"
          :class="{ 'is-active is-active--setting': routeName === 'tasks' && showOptionsPanel }"
          :aria-pressed="routeName === 'tasks' && showOptionsPanel"
          @click="toggleOptionsPanel"
        >
          <svg class="leaf-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="3"></circle>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
          </svg>
          {{ t('app.manageCategories') }}
        </button>
      </div>
    </div>
  </nav>
</template>
