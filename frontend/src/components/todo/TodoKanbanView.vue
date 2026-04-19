<script setup lang="ts">
import { computed, ref } from 'vue'

import TodoItemRow from './TodoItemRow.vue'
import type { TodoDraft, TodoItem, TodoSubItem, TodoSubItemSummary } from './types'

const props = defineProps<{
  todos: TodoItem[]
  selectedIds: number[]
  editingId: number | null
  editTodoForm: TodoDraft
  viewMode: 'ACTIVE' | 'RECYCLE_BIN'
  submitting: boolean
  categoryListId: string
  tagListId: string
  expandedTodoIds: number[]
  checklistItemsByTodoId: Record<number, TodoSubItem[]>
  checklistSummaryByTodoId: Record<number, TodoSubItemSummary | undefined>
  checklistDraftByTodoId: Record<number, string>
  checklistLoadingTodoIds: number[]
  checklistCreatingTodoIds: number[]
  checklistPendingSubItemIdsByTodoId: Record<number, number[]>
}>()

const emit = defineEmits<{
  (e: 'update:selected', id: number, selected: boolean): void
  (e: 'update:editForm', value: TodoDraft): void
  (e: 'toggleStatus', todo: TodoItem): void
  (e: 'moveTodo', todo: TodoItem, status: 'PENDING' | 'DONE'): void
  (e: 'startEdit', todo: TodoItem): void
  (e: 'cancelEdit'): void
  (e: 'saveEdit', todo: TodoItem): void
  (e: 'deleteTodo', id: number): void
  (e: 'restoreTodo', id: number): void
  (e: 'toggleChecklist', todoId: number): void
  (e: 'update:checklistDraftTitle', todoId: number, value: string): void
  (e: 'createSubItem', todoId: number): void
  (e: 'toggleSubItemStatus', todoId: number, item: TodoSubItem): void
  (e: 'deleteSubItem', todoId: number, item: TodoSubItem): void
}>()

const columns = computed(() => [
  {
    key: 'PENDING',
    titleKey: 'kanban.pendingColumn',
    items: props.todos.filter((todo) => todo.status !== 'DONE'),
  },
  {
    key: 'DONE',
    titleKey: 'kanban.doneColumn',
    items: props.todos.filter((todo) => todo.status === 'DONE'),
  },
])

const draggingTodoId = ref<number | null>(null)
const draggingTodo = ref<TodoItem | null>(null)
const dragOverColumn = ref<'PENDING' | 'DONE' | null>(null)

function handleDragStart(todo: TodoItem) {
  draggingTodoId.value = todo.id
  draggingTodo.value = todo
}

function handleDragEnd() {
  draggingTodoId.value = null
  draggingTodo.value = null
  dragOverColumn.value = null
}

function handleDragOver(columnKey: 'PENDING' | 'DONE', event: DragEvent) {
  event.preventDefault()
  dragOverColumn.value = columnKey
}

function handleDrop(columnKey: 'PENDING' | 'DONE') {
  const todo = draggingTodo.value
  dragOverColumn.value = null
  draggingTodoId.value = null
  draggingTodo.value = null
  if (!todo || todo.status === columnKey) {
    return
  }
  emit('moveTodo', todo, columnKey)
}
</script>

<template>
  <section class="kanban-board" aria-label="Todo kanban board">
    <div
      v-for="column in columns"
      :key="column.key"
      class="kanban-column"
      :class="{ 'is-drag-over': dragOverColumn === column.key }"
      @dragover="handleDragOver(column.key as 'PENDING' | 'DONE', $event)"
      @dragleave="dragOverColumn = null"
      @drop.prevent="handleDrop(column.key as 'PENDING' | 'DONE')"
    >
      <header class="kanban-column-header">
        <h3>{{ $t(column.titleKey) }}</h3>
        <span class="badge badge-info">{{ column.items.length }}</span>
      </header>

      <ul v-if="column.items.length > 0" class="kanban-column-list">
        <li
          v-for="todo in column.items"
          :key="todo.id"
          class="kanban-card-shell"
          :class="{ 'is-dragging': draggingTodoId === todo.id }"
          draggable="true"
          @dragstart="handleDragStart(todo)"
          @dragend="handleDragEnd"
        >
        <TodoItemRow
          :todo="todo"
          :isSelected="selectedIds.includes(todo.id)"
          :isEditing="editingId === todo.id"
          :editForm="editTodoForm"
          :categoryListId="categoryListId"
          :tagListId="tagListId"
          :viewMode="viewMode"
          :submitting="submitting"
          :checklistExpanded="expandedTodoIds.includes(todo.id)"
          :checklistItems="checklistItemsByTodoId[todo.id] || []"
          :checklistSummary="checklistSummaryByTodoId[todo.id]"
          :checklistDraftTitle="checklistDraftByTodoId[todo.id] || ''"
          :checklistLoading="checklistLoadingTodoIds.includes(todo.id)"
          :checklistCreating="checklistCreatingTodoIds.includes(todo.id)"
          :checklistPendingIds="checklistPendingSubItemIdsByTodoId[todo.id] || []"
          @update:selected="emit('update:selected', todo.id, $event)"
          @toggleStatus="emit('toggleStatus', todo)"
          @startEdit="emit('startEdit', todo)"
          @update:editForm="emit('update:editForm', $event)"
          @cancelEdit="emit('cancelEdit')"
          @saveEdit="emit('saveEdit', todo)"
          @deleteTodo="emit('deleteTodo', todo.id)"
          @restoreTodo="emit('restoreTodo', todo.id)"
          @toggleChecklist="emit('toggleChecklist', todo.id)"
          @update:checklistDraftTitle="emit('update:checklistDraftTitle', todo.id, $event)"
          @createSubItem="emit('createSubItem', todo.id)"
          @toggleSubItemStatus="emit('toggleSubItemStatus', todo.id, $event)"
          @deleteSubItem="emit('deleteSubItem', todo.id, $event)"
        />
        </li>
      </ul>

      <div v-else class="kanban-empty-state">
        {{ dragOverColumn === column.key ? $t('kanban.dropHere') : $t('kanban.emptyColumn') }}
      </div>
    </div>
  </section>
</template>
