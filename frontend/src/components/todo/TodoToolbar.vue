<script setup lang="ts">
import { computed } from 'vue'
import type { AppLocale } from '../../i18n'
import type { PageData, TodoItem } from './types'
import TodoWorkbenchHeader from './TodoWorkbenchHeader.vue'

const props = defineProps<{
  displayMode: 'LIST' | 'KANBAN' | 'CALENDAR'
  pageData: PageData<TodoItem> | null
  pendingCount: number
  loading: boolean
  viewMode: 'ACTIVE' | 'RECYCLE_BIN'
  showOptionsPanel: boolean
  locale: AppLocale
}>()

const emit = defineEmits<{
  (e: 'update:displayMode', val: 'LIST' | 'KANBAN' | 'CALENDAR'): void
  (e: 'refresh'): void
  (e: 'update:viewMode', value: 'ACTIVE' | 'RECYCLE_BIN'): void
  (e: 'update:showOptionsPanel', value: boolean): void
  (e: 'update:locale', value: AppLocale): void
}>()

const localeOptions: Array<{ value: AppLocale; labelKey: 'locale.en' | 'locale.zhCN' }> = [
  { value: 'en', labelKey: 'locale.en' },
  { value: 'zh-CN', labelKey: 'locale.zhCN' }
]

const showSummary = computed(() => Boolean(props.pageData))
const refreshLabel = computed(() => (props.loading ? 'app.syncing' : 'app.refresh'))

function handleLocaleChange(event: Event) {
  emit('update:locale', (event.target as HTMLSelectElement).value as AppLocale)
}
</script>

<template>
  <TodoWorkbenchHeader :title="$t('app.title')">
    <template v-if="showSummary" #summary>
      <p class="subtitle">
        {{ $t('app.total', { total: pageData!.totalElements }) }}
        <span class="divider">&bull;</span>
        <span class="highlight">{{ $t('app.pending', { pending: pendingCount }) }}</span>
        {{ $t('app.onThisPage') }}
      </p>
    </template>

    <template #actions>
      <div class="control-cluster control-cluster--end control-cluster--stretch-sm">
        <label class="sr-only" for="locale-switcher">{{ $t('app.localeLabel') }}</label>
        <select id="locale-switcher" :value="locale" class="cyber-input form-sm" :title="$t('app.localeLabel')" :aria-label="$t('app.localeLabel')" @change="handleLocaleChange">
          <option v-for="option in localeOptions" :key="option.value" :value="option.value">{{ $t(option.labelKey) }}</option>
        </select>
        <button type="button" class="btn btn-outline workbench-refresh-button" :disabled="loading" @click="$emit('refresh')">
          {{ $t(refreshLabel) }}
        </button>
      </div>
    </template>
  </TodoWorkbenchHeader>
</template>
