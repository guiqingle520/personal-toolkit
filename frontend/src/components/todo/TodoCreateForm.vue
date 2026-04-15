<script setup lang="ts">
import type { TodoDraft } from './types'
import LocalizedDateInput from './LocalizedDateInput.vue'

defineProps<{
  newTodo: TodoDraft
  submitting: boolean
  categoryListId: string
  tagListId: string
}>()
defineEmits<{
  (e: 'update:newTodo', value: TodoDraft): void
  (e: 'createTodo'): void
}>()
</script>
<template>
  <div class="create-form">
    <div class="create-row">
      <input :value="newTodo.title" type="text" :placeholder="$t('form.whatNeedsToBeDone')" class="cyber-input flex-2" @input="$emit('update:newTodo', { ...newTodo, title: ($event.target as HTMLInputElement).value })" @keyup.enter="$emit('createTodo')" :disabled="submitting" />
      <select :value="newTodo.priority" class="cyber-input" :disabled="submitting" @change="$emit('update:newTodo', { ...newTodo, priority: Number(($event.target as HTMLSelectElement).value) })">
        <option :value="1">{{ $t('priority.backlog') }}</option>
        <option :value="2">{{ $t('priority.low') }}</option>
        <option :value="3">{{ $t('priority.medium') }}</option>
        <option :value="4">{{ $t('priority.high') }}</option>
        <option :value="5">{{ $t('priority.critical') }}</option>
      </select>
      <input :value="newTodo.category" type="text" :placeholder="$t('filter.category')" class="cyber-input" :list="categoryListId" :disabled="submitting" @input="$emit('update:newTodo', { ...newTodo, category: ($event.target as HTMLInputElement).value })" />
    </div>
    <div class="create-row">
      <LocalizedDateInput :modelValue="newTodo.dueDate" class="cyber-input" :disabled="submitting" @update:modelValue="$emit('update:newTodo', { ...newTodo, dueDate: $event })" />
      <input :value="newTodo.tags" type="text" :placeholder="$t('form.tagsCsv')" class="cyber-input flex-2" :list="tagListId" :disabled="submitting" @input="$emit('update:newTodo', { ...newTodo, tags: ($event.target as HTMLInputElement).value })" @keyup.enter="$emit('createTodo')" />
      <select :value="newTodo.recurrenceType || ''" class="cyber-input" :disabled="submitting" @change="$emit('update:newTodo', { ...newTodo, recurrenceType: ($event.target as HTMLSelectElement).value || undefined })">
        <option value="">{{ $t('recurrence.none') }}</option>
        <option value="DAILY">{{ $t('recurrence.daily') }}</option>
        <option value="WEEKLY">{{ $t('recurrence.weekly') }}</option>
        <option value="MONTHLY">{{ $t('recurrence.monthly') }}</option>
      </select>
      <input v-if="newTodo.recurrenceType" :value="newTodo.recurrenceInterval || 1" type="number" min="1" :placeholder="$t('recurrence.interval')" class="cyber-input" :disabled="submitting" @input="$emit('update:newTodo', { ...newTodo, recurrenceInterval: Number(($event.target as HTMLInputElement).value) })" />
      <LocalizedDateInput v-if="newTodo.recurrenceType" :modelValue="newTodo.recurrenceEndTime" :placeholder="$t('recurrence.endTime')" class="cyber-input" :disabled="submitting" @update:modelValue="$emit('update:newTodo', { ...newTodo, recurrenceEndTime: $event })" />
      <button class="btn btn-primary" :disabled="submitting || !newTodo.title.trim()" @click="$emit('createTodo')">
        {{ $t('form.addTask') }}
      </button>
    </div>
  </div>
</template>
