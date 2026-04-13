<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

import TodoToolbar from './todo/TodoToolbar.vue'
import TodoOptionsPanel from './todo/TodoOptionsPanel.vue'
import TodoStatsPanel from './todo/TodoStatsPanel.vue'
import TodoFilters from './todo/TodoFilters.vue'
import TodoCreateForm from './todo/TodoCreateForm.vue'
import TodoEmptyState from './todo/TodoEmptyState.vue'
import TodoPagination from './todo/TodoPagination.vue'
import TodoItemsList from './todo/TodoItemsList.vue'
import TodoKanbanView from './todo/TodoKanbanView.vue'
import type { PageData, TodoDraft, TodoFiltersModel, TodoItem, TodoOptions, TodoSubItem, TodoSubItemSummary, TodoStatsOverview, TodoStatsCategoryItem, TodoStatsTrend, TodoStatsTrendItem } from './todo/types'

function handleSelectedUpdate(id: number, selected: boolean) {
  if (selected) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value.push(id)
    }
  } else {
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  }
}

import { persistLocale, type AppLocale } from '../i18n'
import {
  formatDateForInput,
  toDateTimeValue,
} from '../utils/todoView'
import { fetchApi } from '../api'
import type { ApiError } from '../api'

const { t, locale } = useI18n()

const todos = ref<TodoItem[]>([])
const pageData = ref<PageData<TodoItem> | null>(null)
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const validationErrors = ref<Record<string, string[]>>({})

const statsOverview = ref<TodoStatsOverview | null>(null)
const statsCategories = ref<TodoStatsCategoryItem[]>([])
const statsTrend = ref<TodoStatsTrendItem[]>([])

const CATEGORY_LIST_ID = 'category-options'
const TAG_LIST_ID = 'tag-options'

const newTodo = ref<TodoDraft>({
  title: '',
  priority: 3,
  category: '',
  dueDate: '',
  tags: '',
  recurrenceType: '',
  recurrenceInterval: 1,
  recurrenceEndTime: ''
})

const editingId = ref<number | null>(null)
const editTodoForm = ref<TodoDraft>({
  title: '',
  priority: 3,
  category: '',
  dueDate: '',
  tags: '',
  recurrenceType: '',
  recurrenceInterval: 1,
  recurrenceEndTime: ''
})

const filters = ref<TodoFiltersModel>({
  page: 0,
  size: 10,
  status: '',
  priority: '',
  category: '',
  keyword: '',
  tag: '',
  dueDateFrom: '',
  dueDateTo: '',
  sortBy: 'createTime',
  sortDir: 'DESC'
})

const pendingCount = computed(() => todos.value.filter(t => t.status !== 'DONE').length)

const viewMode = ref<'ACTIVE' | 'RECYCLE_BIN'>('ACTIVE')
const displayMode = ref<'LIST' | 'KANBAN'>('LIST')
const selectedIds = ref<number[]>([])
const options = ref<TodoOptions>({ categories: [], tags: [] })
const showOptionsPanel = ref(false)
const expandedTodoIds = ref<number[]>([])
const checklistItemsByTodoId = ref<Record<number, TodoSubItem[]>>({})
const checklistSummaryByTodoId = ref<Record<number, TodoSubItemSummary | undefined>>({})
const checklistDraftByTodoId = ref<Record<number, string>>({})
const checklistLoadingTodoIds = ref<number[]>([])
const checklistCreatingTodoIds = ref<number[]>([])
const checklistPendingSubItemIdsByTodoId = ref<Record<number, number[]>>({})

const isAllSelected = computed({
  get: () => todos.value.length > 0 && selectedIds.value.length === todos.value.length,
  set: (val) => {
    if (val) selectedIds.value = todos.value.map(t => t.id)
    else selectedIds.value = []
  }
})

watch(viewMode, () => {
  selectedIds.value = []
  filters.value.page = 0
  if (viewMode.value !== 'ACTIVE') displayMode.value = 'LIST'
  expandedTodoIds.value = []
  if (viewMode.value !== 'ACTIVE') {
    statsOverview.value = null
    statsCategories.value = []
    statsTrend.value = []
  }
  loadTodos()
})

watch(locale, (newLocale) => {
  persistLocale(newLocale as AppLocale)
})

function handleLocaleUpdate(nextLocale: AppLocale) {
  locale.value = nextLocale
}

function handleFiltersUpdate(nextFilters: TodoFiltersModel) {
  selectedIds.value = []
  filters.value = {
    ...nextFilters,
    page: 0
  }
}

function handleNewTodoUpdate(nextDraft: TodoDraft) {
  newTodo.value = nextDraft
}

function handleEditFormUpdate(nextDraft: TodoDraft) {
  editTodoForm.value = nextDraft
}

function reconcileSelectedIds() {
  const visibleIds = new Set(todos.value.map((todo) => todo.id))
  selectedIds.value = selectedIds.value.filter((id) => visibleIds.has(id))
}

function hydrateChecklistSummaries(items: TodoItem[]) {
  const nextSummaries: Record<number, TodoSubItemSummary | undefined> = {}
  items.forEach((todo) => {
    nextSummaries[todo.id] = todo.subItemSummary
  })
  checklistSummaryByTodoId.value = nextSummaries
}

function setChecklistLoading(todoId: number, loading: boolean) {
  if (loading) {
    if (!checklistLoadingTodoIds.value.includes(todoId)) {
      checklistLoadingTodoIds.value.push(todoId)
    }
  } else {
    checklistLoadingTodoIds.value = checklistLoadingTodoIds.value.filter((id) => id !== todoId)
  }
}

function setChecklistCreating(todoId: number, creating: boolean) {
  if (creating) {
    if (!checklistCreatingTodoIds.value.includes(todoId)) {
      checklistCreatingTodoIds.value.push(todoId)
    }
  } else {
    checklistCreatingTodoIds.value = checklistCreatingTodoIds.value.filter((id) => id !== todoId)
  }
}

function setChecklistPending(todoId: number, subItemId: number, pending: boolean) {
  const currentIds = checklistPendingSubItemIdsByTodoId.value[todoId] || []
  checklistPendingSubItemIdsByTodoId.value = {
    ...checklistPendingSubItemIdsByTodoId.value,
    [todoId]: pending
      ? Array.from(new Set([...currentIds, subItemId]))
      : currentIds.filter((id) => id !== subItemId),
  }
}

function updateChecklistSummary(todoId: number) {
  const items = checklistItemsByTodoId.value[todoId] || []
  const completedCount = items.filter((item) => item.status === 'DONE').length
  const totalCount = items.length
  checklistSummaryByTodoId.value = {
    ...checklistSummaryByTodoId.value,
    [todoId]: {
      totalCount,
      completedCount,
      progressPercent: totalCount === 0 ? 0 : Math.floor((completedCount * 100) / totalCount),
    },
  }
}

function handleError(error: unknown) {
  if (error instanceof Error) {
    try {
      const parsed = JSON.parse(error.message) as ApiError
      errorMessage.value = parsed.message || t('feedback.genericError')
      validationErrors.value = parsed.validation || {}
    } catch {
      errorMessage.value = error.message
      validationErrors.value = {}
    }
  } else {
    errorMessage.value = t('feedback.unexpectedError')
    validationErrors.value = {}
  }
}

async function loadStats() {
  if (viewMode.value !== 'ACTIVE') {
    statsOverview.value = null
    statsCategories.value = []
    statsTrend.value = []
    return
  }

  try {
    const [overviewRes, categoryRes, trendRes] = await Promise.all([
      fetchApi<TodoStatsOverview>('/api/todos/stats/overview'),
      fetchApi<TodoStatsCategoryItem[]>('/api/todos/stats/by-category'),
      fetchApi<TodoStatsTrend>('/api/todos/stats/trend?range=7d')
    ])
    statsOverview.value = overviewRes.data || null
    statsCategories.value = categoryRes.data || []
    statsTrend.value = trendRes.data?.items || []
  } catch (error) {
    statsOverview.value = null
    statsCategories.value = []
    statsTrend.value = []
  }
}

async function loadTodos() {
  loading.value = true
  errorMessage.value = ''
  validationErrors.value = {}

  try {
    const params = new URLSearchParams()
    Object.entries(filters.value).forEach(([k, v]) => {
      if (v !== '' && v !== null && v !== undefined) {
        params.append(k, String(v))
      }
    })
    
    const endpoint = viewMode.value === 'RECYCLE_BIN' ? '/api/todos/recycle-bin' : '/api/todos'
    const response = await fetchApi<PageData<TodoItem>>(`${endpoint}?${params.toString()}`)
    pageData.value = response.data || null
    todos.value = response.data?.content || []
    hydrateChecklistSummaries(todos.value)
    reconcileSelectedIds()
    if (viewMode.value === 'ACTIVE') {
      loadStats()
    }
  } catch (error) {
    handleError(error)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  selectedIds.value = []
  filters.value = {
    page: 0,
    size: 10,
    status: '',
    priority: '',
    category: '',
    keyword: '',
    tag: '',
    dueDateFrom: '',
    dueDateTo: '',
    sortBy: 'createTime',
    sortDir: 'DESC'
  }
  loadTodos()
}

function prevPage() {
  if (pageData.value && !pageData.value.first) {
    filters.value.page--
    loadTodos()
  }
}

function nextPage() {
  if (pageData.value && !pageData.value.last) {
    filters.value.page++
    loadTodos()
  }
}

async function createTodo() {
  if (!newTodo.value.title.trim()) return
  
  submitting.value = true
  errorMessage.value = ''
  validationErrors.value = {}

  try {
        const payload = {
          title: newTodo.value.title,
          status: 'PENDING',
          priority: newTodo.value.priority,
          category: newTodo.value.category,
          dueDate: toDateTimeValue(newTodo.value.dueDate),
          tags: newTodo.value.tags,
          recurrenceType: newTodo.value.recurrenceType || undefined,
          recurrenceInterval: newTodo.value.recurrenceType ? (newTodo.value.recurrenceInterval || 1) : undefined,
          recurrenceEndTime: newTodo.value.recurrenceType ? toDateTimeValue(newTodo.value.recurrenceEndTime || '') : undefined
        }
    
    await fetchApi<TodoItem>('/api/todos', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    
    await loadTodos()
    newTodo.value = { title: '', priority: 3, category: '', dueDate: '', tags: '', recurrenceType: '', recurrenceInterval: 1, recurrenceEndTime: '' }
  } catch (error) {
    handleError(error)
  } finally {
    submitting.value = false
  }
}

async function startEdit(todo: TodoItem) {
  editingId.value = todo.id
    editTodoForm.value = {
      title: todo.title,
      priority: todo.priority || 3,
      category: todo.category || '',
      dueDate: formatDateForInput(todo.dueDate),
      tags: todo.tags || '',
      recurrenceType: todo.recurrenceType || '',
      recurrenceInterval: todo.recurrenceInterval || 1,
      recurrenceEndTime: formatDateForInput(todo.recurrenceEndTime)
    }
}

function cancelEdit() {
  editingId.value = null
}

async function saveEdit(todo: TodoItem) {
  if (!editTodoForm.value.title.trim()) {
    cancelEdit()
    return
  }
  
  submitting.value = true
  errorMessage.value = ''
  validationErrors.value = {}

  try {
      const payload = {
        ...todo,
        title: editTodoForm.value.title,
        priority: editTodoForm.value.priority,
        category: editTodoForm.value.category,
        dueDate: toDateTimeValue(editTodoForm.value.dueDate),
        tags: editTodoForm.value.tags,
        recurrenceType: editTodoForm.value.recurrenceType || undefined,
        recurrenceInterval: editTodoForm.value.recurrenceType ? (editTodoForm.value.recurrenceInterval || 1) : undefined,
        recurrenceEndTime: editTodoForm.value.recurrenceType ? toDateTimeValue(editTodoForm.value.recurrenceEndTime || '') : undefined
      }
    
    await fetchApi<TodoItem>(`/api/todos/${todo.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
    
    await loadTodos()
    cancelEdit()
  } catch (error) {
    handleError(error)
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(todo: TodoItem) {
  const newStatus = todo.status === 'DONE' ? 'PENDING' : 'DONE'
  submitting.value = true
  errorMessage.value = ''

  try {
    await fetchApi<TodoItem>(`/api/todos/${todo.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        ...todo,
        status: newStatus,
        priority: todo.priority ?? 3,
        dueDate: todo.dueDate ?? null,
        category: todo.category ?? '',
        tags: todo.tags ?? '',
        recurrenceType: todo.recurrenceType || undefined,
        recurrenceInterval: todo.recurrenceType ? (todo.recurrenceInterval || 1) : undefined,
        recurrenceEndTime: todo.recurrenceType ? (todo.recurrenceEndTime || undefined) : undefined
      }),
    })
    await loadTodos()
  } catch (error) {
    handleError(error)
  } finally {
    submitting.value = false
  }
}

async function deleteTodo(id: number) {
  if (!confirm(t('action.confirmDeleteSingle'))) return
  
  submitting.value = true
  errorMessage.value = ''

  try {
    await fetchApi(`/api/todos/${id}`, { method: 'DELETE' })
    await loadTodos()
  } catch (error) {
    handleError(error)
  } finally {
    submitting.value = false
  }
}

onMounted(() => { loadTodos(); loadOptions(); })

async function loadOptions() {
  try {
    const response = await fetchApi<TodoOptions>('/api/todos/options')
    if (response.data) {
      options.value = response.data
    }
  } catch {
    options.value = { categories: [], tags: [] }
  }
}

async function batchComplete() {
  if (!selectedIds.value.length) return
  submitting.value = true
  try {
    await fetchApi('/api/todos/batch/complete', {
      method: 'POST',
      body: JSON.stringify({ ids: selectedIds.value })
    })
    selectedIds.value = []
    await loadTodos()
  } catch(e) { handleError(e) } finally { submitting.value = false }
}

async function batchDelete() {
  if (!selectedIds.value.length) return
  if (!confirm(t('action.confirmDeleteBatch'))) return
  submitting.value = true
  try {
    await fetchApi('/api/todos/batch/delete', {
      method: 'POST',
      body: JSON.stringify({ ids: selectedIds.value })
    })
    selectedIds.value = []
    await loadTodos()
  } catch(e) { handleError(e) } finally { submitting.value = false }
}

async function batchRestore() {
  if (!selectedIds.value.length) return
  submitting.value = true
  try {
    await fetchApi('/api/todos/batch/restore', {
      method: 'POST',
      body: JSON.stringify({ ids: selectedIds.value })
    })
    selectedIds.value = []
    await loadTodos()
  } catch(e) { handleError(e) } finally { submitting.value = false }
}

async function restoreTodo(id: number) {
  submitting.value = true
  try {
    await fetchApi(`/api/todos/${id}/restore`, { method: 'PUT' })
    await loadTodos()
  } catch(e) { handleError(e) } finally { submitting.value = false }
}

async function loadSubItems(todoId: number) {
  setChecklistLoading(todoId, true)
  try {
    const response = await fetchApi<TodoSubItem[]>(`/api/todos/${todoId}/sub-items`)
    checklistItemsByTodoId.value = {
      ...checklistItemsByTodoId.value,
      [todoId]: response.data || [],
    }
    updateChecklistSummary(todoId)
  } catch (error) {
    handleError(error)
  } finally {
    setChecklistLoading(todoId, false)
  }
}

async function toggleChecklist(todoId: number) {
  if (expandedTodoIds.value.includes(todoId)) {
    expandedTodoIds.value = expandedTodoIds.value.filter((id) => id !== todoId)
    return
  }

  expandedTodoIds.value.push(todoId)
  if (!checklistItemsByTodoId.value[todoId]) {
    await loadSubItems(todoId)
  }
}

function updateChecklistDraftTitle(todoId: number, value: string) {
  checklistDraftByTodoId.value = {
    ...checklistDraftByTodoId.value,
    [todoId]: value,
  }
}

async function createSubItem(todoId: number) {
  const title = checklistDraftByTodoId.value[todoId]?.trim()
  if (!title) return

  setChecklistCreating(todoId, true)
  errorMessage.value = ''
  validationErrors.value = {}

  try {
    const response = await fetchApi<TodoSubItem>(`/api/todos/${todoId}/sub-items`, {
      method: 'POST',
      body: JSON.stringify({ title }),
    })

    checklistItemsByTodoId.value = {
      ...checklistItemsByTodoId.value,
      [todoId]: [...(checklistItemsByTodoId.value[todoId] || []), response.data as TodoSubItem],
    }
    checklistDraftByTodoId.value = {
      ...checklistDraftByTodoId.value,
      [todoId]: '',
    }
    updateChecklistSummary(todoId)
  } catch (error) {
    handleError(error)
  } finally {
    setChecklistCreating(todoId, false)
  }
}

async function toggleSubItemStatus(todoId: number, item: TodoSubItem) {
  const nextStatus = item.status === 'DONE' ? 'PENDING' : 'DONE'
  setChecklistPending(todoId, item.id, true)
  try {
    const response = await fetchApi<TodoSubItem>(`/api/todos/${todoId}/sub-items/${item.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        title: item.title,
        status: nextStatus,
        sortOrder: item.sortOrder,
      }),
    })
    checklistItemsByTodoId.value = {
      ...checklistItemsByTodoId.value,
      [todoId]: (checklistItemsByTodoId.value[todoId] || []).map((subItem) => subItem.id === item.id ? response.data as TodoSubItem : subItem),
    }
    updateChecklistSummary(todoId)
  } catch (error) {
    handleError(error)
  } finally {
    setChecklistPending(todoId, item.id, false)
  }
}

async function deleteSubItem(todoId: number, item: TodoSubItem) {
  setChecklistPending(todoId, item.id, true)
  try {
    await fetchApi(`/api/todos/${todoId}/sub-items/${item.id}`, { method: 'DELETE' })
    checklistItemsByTodoId.value = {
      ...checklistItemsByTodoId.value,
      [todoId]: (checklistItemsByTodoId.value[todoId] || []).filter((subItem) => subItem.id !== item.id),
    }
    updateChecklistSummary(todoId)
  } catch (error) {
    handleError(error)
  } finally {
    setChecklistPending(todoId, item.id, false)
  }
}

</script>

<template>
  <section class="todo-panel">
    <div class="glass-bg"></div>
    <div class="content-wrapper workbench-layout">
      <div class="workbench-top">
        <TodoToolbar
          :displayMode="displayMode"
          @update:displayMode="displayMode = $event" 
          :pageData="pageData" 
          :pendingCount="pendingCount"
          :loading="loading"
          :viewMode="viewMode"
          :showOptionsPanel="showOptionsPanel"
          :locale="locale as AppLocale"
          @refresh="loadTodos"
          @update:locale="handleLocaleUpdate"
          @update:viewMode="viewMode = $event"
          @update:showOptionsPanel="showOptionsPanel = $event"
        />

        <datalist id="category-options">
          <option v-for="c in options.categories" :key="c" :value="c"></option>
        </datalist>
        <datalist id="tag-options">
          <option v-for="t in options.tags" :key="t" :value="t"></option>
        </datalist>
      </div>

      <div class="workbench-body">
        <aside class="workbench-sidebar">
          <TodoStatsPanel 
            v-if="viewMode === 'ACTIVE'"
            :overview="statsOverview"
            :categories="statsCategories"
            :trend="statsTrend"
          />

          <TodoFilters 
            v-if="displayMode === 'LIST'"
            :filters="filters"
            :categoryListId="CATEGORY_LIST_ID"
            :tagListId="TAG_LIST_ID"
            @update:filters="handleFiltersUpdate"
            @loadTodos="loadTodos"
            @resetFilters="resetFilters"
          />
        </aside>

        <main class="workbench-main">
          <TodoOptionsPanel 
            :options="options" 
            :show="showOptionsPanel"
          />

          <TodoCreateForm 
            v-if="displayMode === 'LIST'"
            :newTodo="newTodo"
            :submitting="submitting"
            :categoryListId="CATEGORY_LIST_ID"
            :tagListId="TAG_LIST_ID"
            @update:newTodo="handleNewTodoUpdate"
            @createTodo="createTodo"
          />

          <div v-if="errorMessage" class="error-banner">
            <strong>{{ $t('status.error') }}</strong> {{ errorMessage }}
            <ul v-if="Object.keys(validationErrors).length > 0" class="validation-list">
              <li v-for="(errors, field) in validationErrors" :key="field">
                {{ field }}: {{ errors.join(', ') }}
              </li>
            </ul>
          </div>

          <TodoEmptyState 
            :loading="loading"
            :isEmpty="todos.length === 0"
          />

          <div class="batch-actions-bar" v-show="todos.length > 0 && displayMode === 'LIST'">
            <label class="checkbox-label">
              <input type="checkbox" v-model="isAllSelected" class="cyber-checkbox" /> {{ $t('batch.selectAll') }}
            </label>
            <span class="selected-count" v-if="selectedIds.length > 0">{{ $t('batch.selected', { count: selectedIds.length }) }}</span>
            
            <div class="batch-buttons" v-if="selectedIds.length > 0">
              <template v-if="viewMode === 'ACTIVE'">
                <button class="btn btn-sm btn-success" @click="batchComplete">{{ $t('batch.complete') }}</button>
                <button class="btn btn-sm btn-danger-outline" @click="batchDelete">{{ $t('batch.delete') }}</button>
              </template>
              <template v-else>
                <button class="btn btn-sm btn-success" @click="batchRestore">{{ $t('batch.restore') }}</button>
              </template>
            </div>
          </div>

          <TodoItemsList
            v-if="displayMode === 'LIST'"
            :todos="todos"
            :selectedIds="selectedIds"
            :editingId="editingId"
            :editTodoForm="editTodoForm"
            :viewMode="viewMode"
            :submitting="submitting"
            :categoryListId="CATEGORY_LIST_ID"
            :tagListId="TAG_LIST_ID"
            :expandedTodoIds="expandedTodoIds"
            :checklistItemsByTodoId="checklistItemsByTodoId"
            :checklistSummaryByTodoId="checklistSummaryByTodoId"
            :checklistDraftByTodoId="checklistDraftByTodoId"
            :checklistLoadingTodoIds="checklistLoadingTodoIds"
            :checklistCreatingTodoIds="checklistCreatingTodoIds"
            :checklistPendingSubItemIdsByTodoId="checklistPendingSubItemIdsByTodoId"
            @update:selected="handleSelectedUpdate"
            @update:editForm="handleEditFormUpdate"
            @toggleStatus="toggleStatus"
            @startEdit="startEdit"
            @cancelEdit="cancelEdit"
            @saveEdit="saveEdit"
            @deleteTodo="deleteTodo"
            @restoreTodo="restoreTodo"
            @toggleChecklist="toggleChecklist"
            @update:checklistDraftTitle="updateChecklistDraftTitle"
            @createSubItem="createSubItem"
            @toggleSubItemStatus="toggleSubItemStatus"
            @deleteSubItem="deleteSubItem"
          />

          <TodoKanbanView
            v-if="displayMode === 'KANBAN' && viewMode === 'ACTIVE'"
            :todos="todos"
            :selectedIds="selectedIds"
            :editingId="editingId"
            :editTodoForm="editTodoForm"
            :viewMode="viewMode"
            :submitting="submitting"
            :categoryListId="CATEGORY_LIST_ID"
            :tagListId="TAG_LIST_ID"
            :expandedTodoIds="expandedTodoIds"
            :checklistItemsByTodoId="checklistItemsByTodoId"
            :checklistSummaryByTodoId="checklistSummaryByTodoId"
            :checklistDraftByTodoId="checklistDraftByTodoId"
            :checklistLoadingTodoIds="checklistLoadingTodoIds"
            :checklistCreatingTodoIds="checklistCreatingTodoIds"
            :checklistPendingSubItemIdsByTodoId="checklistPendingSubItemIdsByTodoId"
            @update:selected="handleSelectedUpdate"
            @update:editForm="handleEditFormUpdate"
            @toggleStatus="toggleStatus"
            @startEdit="startEdit"
            @cancelEdit="cancelEdit"
            @saveEdit="saveEdit"
            @deleteTodo="deleteTodo"
            @restoreTodo="restoreTodo"
            @toggleChecklist="toggleChecklist"
            @update:checklistDraftTitle="updateChecklistDraftTitle"
            @createSubItem="createSubItem"
            @toggleSubItemStatus="toggleSubItemStatus"
            @deleteSubItem="deleteSubItem"
          />

          <TodoPagination 
            v-if="displayMode === 'LIST'" 
            :pageData="pageData"
            :loading="loading"
            @prevPage="prevPage"
            @nextPage="nextPage"
          />
        </main>
      </div>

    </div>
  </section>
</template>
