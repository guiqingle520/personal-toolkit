<script setup lang="ts">
import TodoItemRow from './TodoItemRow.vue'
import type { TodoItem, TodoSubItem, TodoSubItemSummary } from './types'

defineProps<{
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

defineEmits<{
  (e: 'update:selected', id: number, selected: boolean): void
  (e: 'toggleStatus', todo: TodoItem): void
  (e: 'startEdit', todo: TodoItem): void
  (e: 'deleteTodo', id: number): void
  (e: 'restoreTodo', id: number): void
  (e: 'toggleChecklist', todoId: number): void
  (e: 'update:checklistDraftTitle', todoId: number, value: string): void
  (e: 'createSubItem', todoId: number): void
  (e: 'toggleSubItemStatus', todoId: number, item: TodoSubItem): void
  (e: 'deleteSubItem', todoId: number, item: TodoSubItem): void
}>()
</script>

<template>
  <ul v-show="todos.length > 0" class="todo-list">
    <TodoItemRow
      v-for="todo in todos"
      :key="todo.id"
      :todo="todo"
      :isSelected="selectedIds.includes(todo.id)"
      @update:selected="$emit('update:selected', todo.id, $event)"
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
      @toggleStatus="$emit('toggleStatus', todo)"
      @startEdit="$emit('startEdit', todo)"
      @deleteTodo="$emit('deleteTodo', todo.id)"
      @restoreTodo="$emit('restoreTodo', todo.id)"
      @toggleChecklist="$emit('toggleChecklist', todo.id)"
      @update:checklistDraftTitle="$emit('update:checklistDraftTitle', todo.id, $event)"
      @createSubItem="$emit('createSubItem', todo.id)"
      @toggleSubItemStatus="$emit('toggleSubItemStatus', todo.id, $event)"
      @deleteSubItem="$emit('deleteSubItem', todo.id, $event)"
    />
  </ul>
</template>
