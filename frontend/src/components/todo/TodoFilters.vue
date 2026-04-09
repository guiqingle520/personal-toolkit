<script setup lang="ts">
import type { TodoFiltersModel } from './types'

defineProps<{
  filters: TodoFiltersModel
  categoryListId: string
  tagListId: string
}>()
defineEmits<{
  (e: 'update:filters', value: TodoFiltersModel): void
  (e: 'loadTodos'): void
  (e: 'resetFilters'): void
}>()
</script>
<template>
  <div class="filter-section">
    <div class="filter-grid">
      <input :value="filters.keyword" type="text" :placeholder="$t('filter.search')" class="cyber-input form-sm" @input="$emit('update:filters', { ...filters, keyword: ($event.target as HTMLInputElement).value })" @keyup.enter="$emit('loadTodos')" />
      <select :value="filters.status" class="cyber-input form-sm" @change="$emit('update:filters', { ...filters, status: ($event.target as HTMLSelectElement).value }); $emit('loadTodos')">
        <option value="">{{ $t('filter.allStatus') }}</option>
        <option value="PENDING">{{ $t('filter.pending') }}</option>
        <option value="DONE">{{ $t('filter.done') }}</option>
      </select>
      <select :value="filters.priority" class="cyber-input form-sm" @change="$emit('update:filters', { ...filters, priority: ($event.target as HTMLSelectElement).value }); $emit('loadTodos')">
        <option value="">{{ $t('filter.allPriorities') }}</option>
        <option :value="1">{{ $t('priority.backlog') }}</option>
        <option :value="2">{{ $t('priority.low') }}</option>
        <option :value="3">{{ $t('priority.medium') }}</option>
        <option :value="4">{{ $t('priority.high') }}</option>
        <option :value="5">{{ $t('priority.critical') }}</option>
      </select>
      <input :value="filters.category" type="text" :placeholder="$t('filter.category')" class="cyber-input form-sm" :list="categoryListId" @input="$emit('update:filters', { ...filters, category: ($event.target as HTMLInputElement).value })" @keyup.enter="$emit('loadTodos')" />
      <input :value="filters.tag" type="text" :placeholder="$t('filter.tag')" class="cyber-input form-sm" :list="tagListId" @input="$emit('update:filters', { ...filters, tag: ($event.target as HTMLInputElement).value })" @keyup.enter="$emit('loadTodos')" />
      <input :value="filters.dueDateFrom" type="date" class="cyber-input form-sm" @change="$emit('update:filters', { ...filters, dueDateFrom: ($event.target as HTMLInputElement).value }); $emit('loadTodos')" />
      <input :value="filters.dueDateTo" type="date" class="cyber-input form-sm" @change="$emit('update:filters', { ...filters, dueDateTo: ($event.target as HTMLInputElement).value }); $emit('loadTodos')" />
      <button class="btn btn-ghost btn-sm" @click="$emit('resetFilters')">{{ $t('filter.reset') }}</button>
    </div>
  </div>
</template>
