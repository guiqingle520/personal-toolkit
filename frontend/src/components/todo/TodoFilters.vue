<script setup lang="ts">
import type { TodoFiltersModel } from './types'
import LocalizedDateInput from './LocalizedDateInput.vue'

const props = defineProps<{
  filters: TodoFiltersModel
  categoryListId: string
  tagListId: string
}>()

defineEmits<{
  (e: 'update:filters', value: TodoFiltersModel): void
  (e: 'loadTodos'): void
  (e: 'resetFilters'): void
}>()

const presets = [
  { value: 'DUE_TODAY', labelKey: 'filter.presetToday' },
  { value: 'OVERDUE', labelKey: 'filter.presetOverdue' },
  { value: 'UPCOMING_REMINDER', labelKey: 'filter.presetUpcoming' },
] as const

type ActiveFilterChip = {
  key: keyof TodoFiltersModel
  labelKey: string
  value: string
}

function activeFilterChips(filters: TodoFiltersModel) {
  const chips: ActiveFilterChip[] = []

  if (filters.status) chips.push({ key: 'status', labelKey: 'filter.statusChip', value: filters.status })
  if (filters.priority) chips.push({ key: 'priority', labelKey: 'filter.priorityChip', value: filters.priority })
  if (filters.category) chips.push({ key: 'category', labelKey: 'filter.categoryChip', value: filters.category })
  if (filters.tag) chips.push({ key: 'tag', labelKey: 'filter.tagChip', value: filters.tag })
  if (filters.recurrenceType) chips.push({ key: 'recurrenceType', labelKey: 'filter.recurrenceChip', value: filters.recurrenceType })
  if (filters.timePreset) chips.push({ key: 'timePreset', labelKey: 'filter.presetChip', value: filters.timePreset })
  if (filters.dueDateFrom) chips.push({ key: 'dueDateFrom', labelKey: 'filter.dueFromChip', value: filters.dueDateFrom })
  if (filters.dueDateTo) chips.push({ key: 'dueDateTo', labelKey: 'filter.dueToChip', value: filters.dueDateTo })
  if (filters.remindDateFrom) chips.push({ key: 'remindDateFrom', labelKey: 'filter.remindFromChip', value: filters.remindDateFrom })
  if (filters.remindDateTo) chips.push({ key: 'remindDateTo', labelKey: 'filter.remindToChip', value: filters.remindDateTo })

  return chips
}
</script>
<template>
  <div class="filter-section">
    <div class="filter-presets">
      <button
        v-for="preset in presets"
        :key="preset.value"
        type="button"
        class="btn btn-outline btn-sm filter-preset-btn"
        :class="{ 'is-active': props.filters.timePreset === preset.value }"
        @click="$emit('update:filters', { ...props.filters, timePreset: props.filters.timePreset === preset.value ? '' : preset.value }); $emit('loadTodos')"
      >
        {{ $t(preset.labelKey) }}
      </button>
    </div>

    <div class="filter-grid">
      <input :value="props.filters.keyword" type="text" :placeholder="$t('filter.search')" class="cyber-input form-sm" @input="$emit('update:filters', { ...props.filters, keyword: ($event.target as HTMLInputElement).value })" @keyup.enter="$emit('loadTodos')" />
      <select :value="props.filters.status" class="cyber-input form-sm" @change="$emit('update:filters', { ...props.filters, status: ($event.target as HTMLSelectElement).value }); $emit('loadTodos')">
        <option value="">{{ $t('filter.allStatus') }}</option>
        <option value="PENDING">{{ $t('filter.pending') }}</option>
        <option value="DONE">{{ $t('filter.done') }}</option>
      </select>
      <select :value="props.filters.priority" class="cyber-input form-sm" @change="$emit('update:filters', { ...props.filters, priority: ($event.target as HTMLSelectElement).value }); $emit('loadTodos')">
        <option value="">{{ $t('filter.allPriorities') }}</option>
        <option :value="1">{{ $t('priority.backlog') }}</option>
        <option :value="2">{{ $t('priority.low') }}</option>
        <option :value="3">{{ $t('priority.medium') }}</option>
        <option :value="4">{{ $t('priority.high') }}</option>
        <option :value="5">{{ $t('priority.critical') }}</option>
      </select>
      <input :value="props.filters.category" type="text" :placeholder="$t('filter.category')" class="cyber-input form-sm" :list="props.categoryListId" @input="$emit('update:filters', { ...props.filters, category: ($event.target as HTMLInputElement).value })" @keyup.enter="$emit('loadTodos')" />
      <input :value="props.filters.tag" type="text" :placeholder="$t('filter.tag')" class="cyber-input form-sm" :list="props.tagListId" @input="$emit('update:filters', { ...props.filters, tag: ($event.target as HTMLInputElement).value })" @keyup.enter="$emit('loadTodos')" />
      <select :value="props.filters.recurrenceType" class="cyber-input form-sm" @change="$emit('update:filters', { ...props.filters, recurrenceType: ($event.target as HTMLSelectElement).value }); $emit('loadTodos')">
        <option value="">{{ $t('filter.allRecurrence') }}</option>
        <option value="NONE">{{ $t('recurrence.none') }}</option>
        <option value="DAILY">{{ $t('recurrence.daily') }}</option>
        <option value="WEEKLY">{{ $t('recurrence.weekly') }}</option>
        <option value="MONTHLY">{{ $t('recurrence.monthly') }}</option>
      </select>
      <LocalizedDateInput :modelValue="props.filters.dueDateFrom" class="cyber-input form-sm" @update:modelValue="$emit('update:filters', { ...props.filters, dueDateFrom: $event })" @change="$emit('loadTodos')" />
      <LocalizedDateInput :modelValue="props.filters.dueDateTo" class="cyber-input form-sm" @update:modelValue="$emit('update:filters', { ...props.filters, dueDateTo: $event })" @change="$emit('loadTodos')" />
      <LocalizedDateInput :modelValue="props.filters.remindDateFrom" :placeholder="$t('reminder.remindFrom')" class="cyber-input form-sm" @update:modelValue="$emit('update:filters', { ...props.filters, remindDateFrom: $event })" @change="$emit('loadTodos')" />
      <LocalizedDateInput :modelValue="props.filters.remindDateTo" :placeholder="$t('reminder.remindTo')" class="cyber-input form-sm" @update:modelValue="$emit('update:filters', { ...props.filters, remindDateTo: $event })" @change="$emit('loadTodos')" />
      <button class="btn btn-ghost btn-sm" @click="$emit('resetFilters')">{{ $t('filter.reset') }}</button>
    </div>

    <div v-if="activeFilterChips(props.filters).length" class="filter-chip-row">
      <button
        v-for="chip in activeFilterChips(props.filters)"
        :key="chip.key"
        type="button"
        class="filter-chip"
        @click="$emit('update:filters', { ...props.filters, [chip.key]: '' }); $emit('loadTodos')"
      >
        {{ $t(chip.labelKey, { value: chip.value }) }}
      </button>
    </div>
  </div>
</template>
