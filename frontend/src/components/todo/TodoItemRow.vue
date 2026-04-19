<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import TodoSubItemList from './TodoSubItemList.vue'
import LocalizedDateInput from './LocalizedDateInput.vue'
import {
  formatDateTimeLabel,
  formatPriorityLabel,
  formatRecurrenceLabelKey,
  parseTags,
  priorityBadgeClass
} from '../../utils/todoView'
import type { TodoDraft, TodoItem, TodoSubItem, TodoSubItemSummary } from './types'

const { locale } = useI18n()

defineProps<{
  todo: TodoItem
  isSelected: boolean
  isEditing: boolean
  editForm: TodoDraft
  categoryListId: string
  tagListId: string
  viewMode: 'ACTIVE' | 'RECYCLE_BIN'
  submitting: boolean
  checklistExpanded: boolean
  checklistItems: TodoSubItem[]
  checklistSummary?: TodoSubItemSummary
  checklistDraftTitle: string
  checklistLoading: boolean
  checklistCreating: boolean
  checklistPendingIds: number[]
}>()

defineEmits<{
  (e: 'update:selected', value: boolean): void
  (e: 'update:editForm', value: TodoDraft): void
  (e: 'toggleStatus'): void
  (e: 'startEdit'): void
  (e: 'cancelEdit'): void
  (e: 'saveEdit'): void
  (e: 'deleteTodo'): void
  (e: 'restoreTodo'): void
  (e: 'toggleChecklist'): void
  (e: 'update:checklistDraftTitle', value: string): void
  (e: 'createSubItem'): void
  (e: 'toggleSubItemStatus', item: TodoSubItem): void
  (e: 'deleteSubItem', item: TodoSubItem): void
}>()

function formatTimestamp(value: string, loc: string): string {
  return new Intl.DateTimeFormat(loc, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}
</script>

<template>
  <li class="todo-item" :class="{ 'is-done': todo.status === 'DONE' }">
    <div class="todo-actions-left">
      <input type="checkbox" :checked="isSelected" @change="$emit('update:selected', ($event.target as HTMLInputElement).checked)" class="cyber-checkbox todo-select-checkbox" />
      <button class="status-toggle" :class="todo.status.toLowerCase()" @click="$emit('toggleStatus')" :disabled="submitting" :title="todo.status === 'DONE' ? $t('status.markAsPending') : $t('status.markAsDone')" :aria-label="todo.status === 'DONE' ? $t('status.markAsPending') : $t('status.markAsDone')">
        <span v-if="todo.status === 'DONE'">✓</span>
      </button>
    </div>
    
    <div class="todo-content">
      <!-- EDIT MODE -->
      <div v-if="isEditing" class="edit-mode">
          <input :value="editForm.title" type="text" class="cyber-input form-sm" :placeholder="$t('form.title')" autoFocus @input="$emit('update:editForm', { ...editForm, title: ($event.target as HTMLInputElement).value })" />
          <div class="edit-row">
          <select :value="editForm.priority" class="cyber-input form-sm" @change="$emit('update:editForm', { ...editForm, priority: Number(($event.target as HTMLSelectElement).value) })">
            <option :value="1">{{ $t('priority.backlog') }}</option>
            <option :value="2">{{ $t('priority.low') }}</option>
            <option :value="3">{{ $t('priority.medium') }}</option>
            <option :value="4">{{ $t('priority.high') }}</option>
            <option :value="5">{{ $t('priority.critical') }}</option>
          </select>
          <input :value="editForm.category" type="text" :placeholder="$t('filter.category')" class="cyber-input form-sm" :list="categoryListId" @input="$emit('update:editForm', { ...editForm, category: ($event.target as HTMLInputElement).value })" />
          <LocalizedDateInput :modelValue="editForm.dueDate" class="cyber-input form-sm" @update:modelValue="$emit('update:editForm', { ...editForm, dueDate: $event })" />
          <LocalizedDateInput :modelValue="editForm.remindAt" :placeholder="$t('reminder.remindAt')" class="cyber-input form-sm" @update:modelValue="$emit('update:editForm', { ...editForm, remindAt: $event })" />
          <input :value="editForm.tags" type="text" :placeholder="$t('form.tagsCsvEdit')" class="cyber-input form-sm" :list="tagListId" @input="$emit('update:editForm', { ...editForm, tags: ($event.target as HTMLInputElement).value })" />
          </div>
          <div class="edit-row">
            <textarea :value="editForm.notes" :placeholder="$t('form.notes')" class="cyber-input form-sm todo-notes-input" @input="$emit('update:editForm', { ...editForm, notes: ($event.target as HTMLTextAreaElement).value })" />
            <textarea :value="editForm.attachmentLinks" :placeholder="$t('form.attachmentLinks')" class="cyber-input form-sm todo-notes-input" @input="$emit('update:editForm', { ...editForm, attachmentLinks: ($event.target as HTMLTextAreaElement).value })" />
          </div>
          <div class="edit-row">
            <input :value="editForm.ownerLabel" :placeholder="$t('form.ownerLabel')" class="cyber-input form-sm" @input="$emit('update:editForm', { ...editForm, ownerLabel: ($event.target as HTMLInputElement).value })" />
            <input :value="editForm.collaborators" :placeholder="$t('form.collaborators')" class="cyber-input form-sm" @input="$emit('update:editForm', { ...editForm, collaborators: ($event.target as HTMLInputElement).value })" />
            <input :value="editForm.watchers" :placeholder="$t('form.watchers')" class="cyber-input form-sm" @input="$emit('update:editForm', { ...editForm, watchers: ($event.target as HTMLInputElement).value })" />
          </div>
          <div class="edit-row">
            <select :value="editForm.recurrenceType || ''" class="cyber-input form-sm" @change="$emit('update:editForm', { ...editForm, recurrenceType: ($event.target as HTMLSelectElement).value || undefined })">
              <option value="">{{ $t('recurrence.none') }}</option>
              <option value="DAILY">{{ $t('recurrence.daily') }}</option>
              <option value="WEEKLY">{{ $t('recurrence.weekly') }}</option>
              <option value="MONTHLY">{{ $t('recurrence.monthly') }}</option>
            </select>
            <input v-if="editForm.recurrenceType" :value="editForm.recurrenceInterval || 1" type="number" min="1" :placeholder="$t('recurrence.interval')" class="cyber-input form-sm" @input="$emit('update:editForm', { ...editForm, recurrenceInterval: Number(($event.target as HTMLInputElement).value) })" />
            <LocalizedDateInput v-if="editForm.recurrenceType" :modelValue="editForm.recurrenceEndTime" :placeholder="$t('recurrence.endTime')" class="cyber-input form-sm" @update:modelValue="$emit('update:editForm', { ...editForm, recurrenceEndTime: $event })" />
          </div>
        <div class="edit-actions">
          <button class="btn btn-sm btn-success" @click="$emit('saveEdit')" :disabled="submitting">{{ $t('form.save') }}</button>
          <button class="btn btn-sm btn-ghost" @click="$emit('cancelEdit')" :disabled="submitting">{{ $t('form.cancel') }}</button>
        </div>
      </div>
      <!-- VIEW MODE -->
      <div v-else class="view-mode">
        <strong class="todo-title">{{ todo.title }}</strong>
        <div class="todo-meta">
          <span v-if="todo.priority" class="badge" :class="priorityBadgeClass(todo.priority)">{{ $t(formatPriorityLabel(todo.priority)) }}</span>
          <span v-if="todo.recurrenceType && todo.recurrenceType !== 'NONE'" class="badge badge-info">🔄 {{ $t(formatRecurrenceLabelKey(todo.recurrenceType)) }}</span>
          <span v-if="todo.nextTriggerTime" class="badge badge-info">{{ $t('recurrence.nextTrigger', { time: formatTimestamp(todo.nextTriggerTime, locale) }) }}</span>
          <span v-if="todo.completedAt" class="badge badge-success">{{ $t('recurrence.completedAt', { time: formatTimestamp(todo.completedAt, locale) }) }}</span>
          <span v-if="todo.category" class="badge badge-category">{{ todo.category }}</span>
          <span v-if="todo.dueDate" class="badge badge-date">📅 {{ todo.dueDate }}</span>
          <span v-if="todo.remindAt" class="badge badge-info">⏰ {{ $t('reminder.scheduledAt', { time: formatDateTimeLabel(todo.remindAt, locale) }) }}</span>
          <span v-if="todo.ownerLabel" class="badge badge-category">👤 {{ $t('collaboration.ownerLabel', { value: todo.ownerLabel }) }}</span>
          <span v-if="todo.collaborators" class="badge badge-info">🤝 {{ $t('collaboration.collaboratorsLabel', { value: todo.collaborators }) }}</span>
          <span v-if="todo.watchers" class="badge badge-info">👁 {{ $t('collaboration.watchersLabel', { value: todo.watchers }) }}</span>
          <span v-for="tag in parseTags(todo.tags)" :key="tag" class="badge badge-tag">#{{ tag }}</span>
          <button v-if="viewMode === 'ACTIVE'" class="badge badge-category checklist-toggle-btn" type="button" @click="$emit('toggleChecklist')">
            {{ $t('checklist.progress', { completed: checklistSummary?.completedCount ?? 0, total: checklistSummary?.totalCount ?? 0 }) }}
          </button>
          <span class="time">{{ formatTimestamp(todo.createTime, locale) }}</span>
        </div>

        <p v-if="todo.notes" class="todo-notes-preview">{{ todo.notes }}</p>

        <div v-if="todo.attachmentLinks" class="todo-attachment-list">
          <a
            v-for="link in todo.attachmentLinks.split(/\r?\n/).filter(Boolean)"
            :key="link"
            :href="link"
            target="_blank"
            rel="noreferrer"
            class="badge badge-info todo-attachment-link"
          >
            {{ $t('form.attachmentLink') }}
          </a>
        </div>

        <TodoSubItemList
          v-if="viewMode === 'ACTIVE' && checklistExpanded"
          :items="checklistItems"
          :summary="checklistSummary"
          :draftTitle="checklistDraftTitle"
          :loading="checklistLoading"
          :creating="checklistCreating"
          :pendingIds="checklistPendingIds"
          @update:draftTitle="$emit('update:checklistDraftTitle', $event)"
          @create="$emit('createSubItem')"
          @toggleStatus="$emit('toggleSubItemStatus', $event)"
          @delete="$emit('deleteSubItem', $event)"
        />
      </div>
    </div>
    
    <div class="todo-actions-right" v-if="!isEditing">
      <button class="action-btn edit-btn" @click="$emit('startEdit')" :disabled="submitting" :title="$t('action.edit')" :aria-label="$t('action.edit')">✎</button>
      <button v-if="viewMode === 'ACTIVE'" class="action-btn delete-btn" @click="$emit('deleteTodo')" :disabled="submitting" :title="$t('action.delete')" :aria-label="$t('action.delete')">×</button>
      <button v-else class="action-btn" @click="$emit('restoreTodo')" :disabled="submitting" :title="$t('action.restore')" :aria-label="$t('action.restore')">↺</button>
    </div>
  </li>
</template>
