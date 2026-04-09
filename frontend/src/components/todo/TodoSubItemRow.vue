<script setup lang="ts">
import type { TodoSubItem } from './types'

defineProps<{
  item: TodoSubItem
  pending: boolean
}>()

defineEmits<{
  (e: 'toggleStatus', item: TodoSubItem): void
  (e: 'delete', item: TodoSubItem): void
}>()
</script>

<template>
  <li class="todo-sub-item" :class="{ 'is-done': item.status === 'DONE' }">
    <label class="checkbox-label todo-sub-item-toggle">
      <input
        class="cyber-checkbox"
        type="checkbox"
        :checked="item.status === 'DONE'"
        :disabled="pending"
        @change="$emit('toggleStatus', item)"
      />
      <span class="todo-sub-item-title">{{ item.title || $t('checklist.untitled') }}</span>
    </label>

    <button
      class="action-btn delete-btn"
      type="button"
      :disabled="pending"
      :title="$t('action.delete')"
      :aria-label="$t('action.delete')"
      @click="$emit('delete', item)"
    >
      ×
    </button>
  </li>
</template>
