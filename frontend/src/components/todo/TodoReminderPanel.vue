<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import type { TodoReminderItem } from './types'

const props = defineProps<{
  reminders: TodoReminderItem[]
  loading: boolean
}>()

defineEmits<{
  (e: 'mark-read', id: number): void
  (e: 'mark-all-read'): void
  (e: 'open-todo', todoId: number): void
}>()

const { t } = useI18n()

function formatDateTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ')
}
</script>

<template>
  <section class="reminder-panel">
    <div class="reminder-panel-header">
      <h3>{{ t('reminder.panelTitle') }}</h3>
      <button type="button" class="btn btn-sm btn-outline" :disabled="loading || reminders.length === 0" @click="$emit('mark-all-read')">
        {{ t('reminder.markAllRead') }}
      </button>
    </div>

    <div v-if="loading" class="reminder-empty-state">{{ $t('app.syncing') }}</div>
    <div v-else-if="reminders.length === 0" class="reminder-empty-state">{{ t('reminder.empty') }}</div>
    <ul v-else class="reminder-list">
      <li v-for="reminder in props.reminders" :key="reminder.id" class="reminder-item">
        <div class="reminder-item-main">
          <div class="reminder-title">{{ reminder.todoTitle }}</div>
          <div class="reminder-meta">
            <span>{{ t('reminder.scheduledAtLabel', { time: formatDateTime(reminder.scheduledAt) }) }}</span>
            <span v-if="reminder.category">{{ reminder.category }}</span>
          </div>
        </div>
        <div class="reminder-actions">
          <button type="button" class="btn btn-sm btn-ghost" @click="$emit('open-todo', reminder.todoId)">{{ t('reminder.openTodo') }}</button>
          <button type="button" class="btn btn-sm btn-outline" @click="$emit('mark-read', reminder.id)">{{ t('reminder.markRead') }}</button>
        </div>
      </li>
    </ul>
  </section>
</template>
