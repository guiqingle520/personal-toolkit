<script setup lang="ts">
import LocalizedDateInput from './LocalizedDateInput.vue'
import type { TodoDraft } from './types'

defineProps<{
  isOpen: boolean
  editForm: TodoDraft
  categoryListId: string
  tagListId: string
  submitting: boolean
}>()

defineEmits<{
  (e: 'update:editForm', value: TodoDraft): void
  (e: 'save'): void
  (e: 'cancel'): void
}>()
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer">
      <div v-if="isOpen" class="todo-edit-drawer-overlay" @click.self="$emit('cancel')">
        <div class="todo-edit-drawer" role="dialog" aria-modal="true" aria-labelledby="drawer-title">
          <header class="drawer-header">
            <h2 id="drawer-title" class="drawer-title">{{ $t('action.edit') }}</h2>
            <button type="button" class="drawer-close" :aria-label="$t('form.cancel')" @click="$emit('cancel')">×</button>
          </header>

          <div class="drawer-body">
            <div class="edit-mode">
              <input :value="editForm.title" type="text" class="cyber-input form-sm" :placeholder="$t('form.title')" autofocus @input="$emit('update:editForm', { ...editForm, title: ($event.target as HTMLInputElement).value })" />
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
                <LocalizedDateInput :modelValue="editForm.remindAt" :max="editForm.dueDate || undefined" :placeholder="$t('reminder.remindAt')" class="cyber-input form-sm" @update:modelValue="$emit('update:editForm', { ...editForm, remindAt: $event })" />
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
            </div>
          </div>

          <footer class="drawer-footer">
            <button type="button" class="btn btn-sm btn-ghost" :disabled="submitting" @click="$emit('cancel')">{{ $t('form.cancel') }}</button>
            <button type="button" class="btn btn-sm btn-success" :disabled="submitting" @click="$emit('save')">{{ $t('form.save') }}</button>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.todo-edit-drawer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(6px);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}

.todo-edit-drawer {
  width: 100%;
  max-width: 480px;
  background: var(--color-surface-base);
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 32px rgba(0, 0, 0, 0.4);
  border-left: 1px solid var(--color-border-subtle);
  overflow: hidden;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px;
  border-bottom: 1px solid var(--color-border-subtle);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0));
}

.drawer-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-strong);
  letter-spacing: -0.01em;
}

.drawer-close {
  background: transparent;
  border: none;
  color: var(--color-text-muted);
  font-size: 1.6rem;
  cursor: pointer;
  padding: 4px;
  line-height: 1;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.drawer-close:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--color-text-bright);
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 28px;
}

.edit-mode {
  background: transparent;
  border: none;
  padding: 0;
  box-shadow: none;
}

.drawer-footer {
  padding: 20px 28px;
  border-top: 1px solid var(--color-border-subtle);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: linear-gradient(0deg, rgba(255, 255, 255, 0.02), rgba(255, 255, 255, 0));
}

.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.3s ease;
}
.drawer-enter-active .todo-edit-drawer,
.drawer-leave-active .todo-edit-drawer {
  transition: transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}
.drawer-enter-from .todo-edit-drawer,
.drawer-leave-to .todo-edit-drawer {
  transform: translateX(100%);
}

.edit-row {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}
</style>
