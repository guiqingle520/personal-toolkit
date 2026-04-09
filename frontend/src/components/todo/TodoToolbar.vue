<script setup lang="ts">
import type { AppLocale } from '../../i18n'
import type { PageData, TodoItem } from './types'

defineProps<{
  pageData: PageData<TodoItem> | null
  pendingCount: number
  loading: boolean
  viewMode: 'ACTIVE' | 'RECYCLE_BIN'
  showOptionsPanel: boolean
  locale: AppLocale
}>()

defineEmits<{
  (e: 'refresh'): void
  (e: 'update:viewMode', value: 'ACTIVE' | 'RECYCLE_BIN'): void
  (e: 'update:showOptionsPanel', value: boolean): void
  (e: 'update:locale', value: AppLocale): void
}>()

const localeOptions: Array<{ value: AppLocale; labelKey: 'locale.en' | 'locale.zhCN' }> = [
  { value: 'en', labelKey: 'locale.en' },
  { value: 'zh-CN', labelKey: 'locale.zhCN' }
]

</script>

<template>
  <div>
    <header class="todo-header">
      <div class="title-group">
        <h1>{{ $t('app.title') }}</h1>
        <p class="subtitle" v-if="pageData">
          {{ $t('app.total', { total: pageData.totalElements }) }} 
          <span class="divider">&bull;</span> 
          <span class="highlight">{{ $t('app.pending', { pending: pendingCount }) }}</span> 
          {{ $t('app.onThisPage') }}
        </p>
      </div>
      <div style="display: flex; gap: 8px; align-items: center;">
        <label class="sr-only" for="locale-switcher">{{ $t('app.localeLabel') }}</label>
        <select id="locale-switcher" :value="locale" class="cyber-input form-sm" :title="$t('app.localeLabel')" :aria-label="$t('app.localeLabel')" @change="$emit('update:locale', ($event.target as HTMLSelectElement).value as AppLocale)">
          <option v-for="option in localeOptions" :key="option.value" :value="option.value">{{ $t(option.labelKey) }}</option>
        </select>
        <button type="button" class="btn btn-outline" style="min-width: 100px; text-align: center;" :disabled="loading" @click="$emit('refresh')">
          {{ loading ? $t('app.syncing') : $t('app.refresh') }}
        </button>
      </div>
    </header>

    <!-- VIEW TOGGLE -->
    <div class="view-toggle-bar">
      <button :class="['btn btn-sm', viewMode === 'ACTIVE' ? 'btn-primary' : 'btn-outline']" @click="$emit('update:viewMode', 'ACTIVE')">{{ $t('app.activeTasks') }}</button>
      <button :class="['btn btn-sm', viewMode === 'RECYCLE_BIN' ? 'btn-primary' : 'btn-outline']" @click="$emit('update:viewMode', 'RECYCLE_BIN')">{{ $t('app.recycleBin') }}</button>
      <button class="btn btn-sm btn-outline" @click="$emit('update:showOptionsPanel', !showOptionsPanel)">{{ $t('app.manageCategories') }}</button>
    </div>
  </div>
</template>
