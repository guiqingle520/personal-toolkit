<script setup lang="ts">
import { computed, ref } from 'vue'

import TodoItemRow from './TodoItemRow.vue'
import type { TodoItem, TodoSubItem, TodoSubItemSummary } from './types'

const props = defineProps<{
  todos: TodoItem[]
  selectedIds: number[]
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
  (e: 'toggleStatus', todo: TodoItem): void
  (e: 'moveTodo', todo: TodoItem, status: 'PENDING' | 'DONE'): void
  (e: 'startEdit', todo: TodoItem): void
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
      :class="[{ 'is-drag-over': dragOverColumn === column.key }, `kanban-column--${column.key}`]"
      :data-status="column.key"
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
          tag="div"
          :todo="todo"
          :isSelected="selectedIds.includes(todo.id)"
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

<style scoped>
.kanban-board {
  display: flex;
  gap: 24px;
  overflow-x: auto;
  padding: 16px 28px;
  height: 100%;
  align-items: flex-start;
}

.kanban-column {
  flex: 1;
  min-width: 320px;
  max-width: 400px;
  background: rgba(15, 15, 15, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  max-height: 100%;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.kanban-column::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: transparent;
  transition: background 0.3s ease;
}

.kanban-column--PENDING::after {
  background: linear-gradient(90deg, transparent, var(--color-primary), transparent);
  opacity: 0.3;
}

.kanban-column--DONE::after {
  background: linear-gradient(90deg, transparent, var(--color-success), transparent);
  opacity: 0.2;
}

.kanban-column.is-drag-over {
  background: rgba(255, 255, 255, 0.05);
  border-color: var(--color-primary);
  box-shadow: 0 0 20px rgba(212, 175, 55, 0.1) inset;
}

.kanban-column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(0, 0, 0, 0.2);
}

.kanban-column-header h3 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--color-text-bright, #fff);
  text-transform: uppercase;
}

.kanban-column-list {
  list-style: none;
  padding: 12px;
  margin: 0;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.kanban-column-list::-webkit-scrollbar {
  width: 6px;
}

.kanban-column-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.kanban-card-shell {
  cursor: grab;
  border-radius: var(--radius-md);
  transition: transform 0.2s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 0.2s ease;
}

.kanban-card-shell:active {
  cursor: grabbing;
}

.kanban-card-shell.is-dragging {
  opacity: 0.4;
  transform: scale(0.98);
}

.kanban-card-shell :deep(.todo-item) {
  margin-bottom: 0; /* Remove margin since gap handles it */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  border-color: rgba(255, 255, 255, 0.08);
}

.kanban-card-shell:hover :deep(.todo-item) {
  border-color: rgba(212, 175, 55, 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
}

.kanban-empty-state {
  padding: 32px 20px;
  text-align: center;
  color: var(--color-text-muted, #888);
  font-style: italic;
  font-size: 0.85rem;
  border: 1px dashed rgba(255, 255, 255, 0.1);
  margin: 12px;
  border-radius: var(--radius-md);
  background: rgba(0, 0, 0, 0.1);
}
</style>
