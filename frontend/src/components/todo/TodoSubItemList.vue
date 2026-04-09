<script setup lang="ts">
import TodoSubItemRow from './TodoSubItemRow.vue'
import type { TodoSubItem, TodoSubItemSummary } from './types'

defineProps<{
  items: TodoSubItem[]
  summary?: TodoSubItemSummary
  draftTitle: string
  loading: boolean
  creating: boolean
  pendingIds: number[]
}>()

defineEmits<{
  (e: 'update:draftTitle', value: string): void
  (e: 'create'): void
  (e: 'toggleStatus', item: TodoSubItem): void
  (e: 'delete', item: TodoSubItem): void
}>()
</script>

<template>
  <section class="todo-subtasks-panel ui-card">
    <div class="todo-subtasks-header">
      <strong>{{ $t('checklist.title') }}</strong>
      <span class="badge badge-category">
        {{ $t('checklist.progress', { completed: summary?.completedCount ?? 0, total: summary?.totalCount ?? 0 }) }}
      </span>
    </div>

    <div class="todo-subtasks-create">
      <input
        class="cyber-input form-sm"
        type="text"
        :value="draftTitle"
        :placeholder="$t('form.addSubtask')"
        :disabled="creating"
        @input="$emit('update:draftTitle', ($event.target as HTMLInputElement).value)"
        @keyup.enter="$emit('create')"
      />
      <button class="btn btn-sm btn-primary" type="button" :disabled="creating || !draftTitle.trim()" @click="$emit('create')">
        {{ $t('form.addSubtask') }}
      </button>
    </div>

    <div v-if="loading" class="empty-state compact">{{ $t('status.initiating') }}</div>
    <div v-else-if="items.length === 0" class="empty-state compact">{{ $t('checklist.empty') }}</div>
    <ul v-else class="todo-sub-item-list">
      <TodoSubItemRow
        v-for="item in items"
        :key="item.id"
        :item="item"
        :pending="pendingIds.includes(item.id)"
        @toggleStatus="$emit('toggleStatus', item)"
        @delete="$emit('delete', item)"
      />
    </ul>
  </section>
</template>
