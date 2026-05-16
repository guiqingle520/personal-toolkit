<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import TodoSubItemList from './TodoSubItemList.vue'
import {
  formatDateTimeLabel,
  formatPriorityLabel,
  formatRecurrenceLabelKey,
  parseTags,
  priorityBadgeClass
} from '../../utils/todoView'
import type { TodoItem, TodoSubItem, TodoSubItemSummary } from './types'

const { locale, t } = useI18n()

const props = defineProps<{
  todo: TodoItem
  isSelected: boolean
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
  tag?: string
}>()

defineEmits<{
  (e: 'update:selected', value: boolean): void
  (e: 'toggleStatus'): void
  (e: 'startEdit'): void
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

function buildTodoActionLabel(actionLabel: string) {
  return `${actionLabel}: ${props.todo.title}`
}
</script>

<template>
  <component :is="tag || 'li'" class="todo-item" :class="{ 'is-done': todo.status === 'DONE' }">
    <div class="todo-actions-left">
      <input type="checkbox" :checked="isSelected" @change="$emit('update:selected', ($event.target as HTMLInputElement).checked)" class="cyber-checkbox todo-select-checkbox" :aria-labelledby="`todo-title-${todo.id}`" />
      <button class="status-toggle" :class="todo.status.toLowerCase()" @click="$emit('toggleStatus')" :disabled="submitting" :title="buildTodoActionLabel(todo.status === 'DONE' ? t('status.markAsPending') : t('status.markAsDone'))" :aria-label="buildTodoActionLabel(todo.status === 'DONE' ? t('status.markAsPending') : t('status.markAsDone'))">
        <span v-if="todo.status === 'DONE'">✓</span>
      </button>
    </div>
    
    <div class="todo-content">
      <div class="view-mode">
        <strong :id="`todo-title-${todo.id}`" class="todo-title">{{ todo.title }}</strong>
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
          <button v-if="viewMode === 'ACTIVE'" class="badge badge-category checklist-toggle-btn" type="button" @click="$emit('toggleChecklist')" :aria-expanded="checklistExpanded" :aria-controls="`checklist-${todo.id}`" :title="buildTodoActionLabel(checklistExpanded ? t('action.hideChecklist') : t('action.showChecklist'))" :aria-label="buildTodoActionLabel(checklistExpanded ? t('action.hideChecklist') : t('action.showChecklist'))">
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
          :id="`checklist-${todo.id}`"
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
    
    <div class="todo-actions-right">
      <button class="action-btn edit-btn" @click="$emit('startEdit')" :disabled="submitting" :title="buildTodoActionLabel(t('action.edit'))" :aria-label="buildTodoActionLabel(t('action.edit'))">✎</button>
      <button v-if="viewMode === 'ACTIVE'" class="action-btn delete-btn" @click="$emit('deleteTodo')" :disabled="submitting" :title="buildTodoActionLabel(t('action.delete'))" :aria-label="buildTodoActionLabel(t('action.delete'))">×</button>
      <button v-else class="action-btn" @click="$emit('restoreTodo')" :disabled="submitting" :title="buildTodoActionLabel(t('action.restore'))" :aria-label="buildTodoActionLabel(t('action.restore'))">↺</button>
    </div>
  </component>
</template>

<style scoped>
.todo-item {
  display: flex;
  gap: 16px;
  padding: 16px 20px;
  margin-bottom: 8px;
  background: rgba(10, 10, 10, 0.6);
  border: 1px solid rgba(212, 175, 55, 0.1);
  border-radius: var(--radius-md);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.todo-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--color-primary);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.todo-item:hover {
  background: rgba(20, 20, 20, 0.8);
  border-color: rgba(212, 175, 55, 0.3);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.todo-item:hover::before {
  opacity: 1;
}

.todo-item.is-done {
  opacity: 0.6;
  background: rgba(5, 5, 5, 0.4);
  border-color: rgba(255, 255, 255, 0.05);
}

.todo-item.is-done::before {
  background: var(--color-success);
}

.todo-actions-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding-top: 2px;
}

.status-toggle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1.5px solid var(--color-primary);
  background: transparent;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s ease;
}

.status-toggle:hover {
  background: rgba(212, 175, 55, 0.1);
  box-shadow: 0 0 8px rgba(212, 175, 55, 0.3);
}

.status-toggle.done {
  background: var(--color-success);
  border-color: var(--color-success);
  color: #000;
}

.todo-content {
  flex: 1;
  min-width: 0;
}

.todo-title {
  font-size: 1.05rem;
  font-weight: 500;
  color: var(--color-text-bright, #fff);
  letter-spacing: 0.01em;
  margin-bottom: 6px;
  display: block;
}

.is-done .todo-title {
  text-decoration: line-through;
  color: var(--color-text-muted, #888);
}

.todo-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  font-size: 0.75rem;
  margin-bottom: 8px;
}

.badge {
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--color-text-muted, #aaa);
  border: 1px solid rgba(255, 255, 255, 0.1);
  font-weight: 500;
  letter-spacing: 0.02em;
  transition: all 0.2s ease;
}

.badge-info {
  background: rgba(212, 175, 55, 0.1);
  color: var(--color-primary);
  border-color: rgba(212, 175, 55, 0.2);
}

.badge-success {
  background: rgba(16, 185, 129, 0.1);
  color: var(--color-success);
  border-color: rgba(16, 185, 129, 0.2);
}

.badge-tag {
  background: transparent;
  border-color: transparent;
  color: rgba(255, 255, 255, 0.5);
  padding: 0;
}

.badge-tag:hover {
  color: var(--color-primary);
}

.checklist-toggle-btn {
  cursor: pointer;
}

.checklist-toggle-btn:hover {
  background: rgba(212, 175, 55, 0.15);
  color: var(--color-primary);
}

.time {
  color: rgba(255, 255, 255, 0.3);
  font-variant-numeric: tabular-nums;
}

.todo-notes-preview {
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 8px 0 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.todo-attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.todo-attachment-link {
  text-decoration: none;
  font-size: 0.75rem;
  display: inline-flex;
  align-items: center;
}

.todo-attachment-link:hover {
  background: rgba(212, 175, 55, 0.2);
  text-decoration: underline;
}

.todo-actions-right {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.todo-item:hover .todo-actions-right,
.todo-item:focus-within .todo-actions-right {
  opacity: 1;
}

.action-btn {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s ease;
  font-size: 1.1rem;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.edit-btn:hover {
  color: var(--color-primary);
}

.delete-btn:hover {
  color: var(--color-danger, #ef4444);
}
</style>
