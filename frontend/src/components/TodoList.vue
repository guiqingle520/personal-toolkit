<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
import TodoReminderPanel from './todo/TodoReminderPanel.vue'
import TodoSavedViewsBar from './todo/TodoSavedViewsBar.vue'
import type { PageData, TodoDraft, TodoFiltersModel, TodoItem, TodoOptions, TodoReminderItem, TodoSavedView, TodoSubItem, TodoSubItemSummary, TodoStatsOverview, TodoStatsCategoryItem, TodoStatsTrend, TodoStatsTrendItem } from './todo/types'

function handleSelectedUpdate(id: number, selected: boolean) {
  if (selected) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value.push(id)
    }
  } else {
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  }
}

import { persistLocale, syncDocumentLocale, type AppLocale } from '../i18n'
import {
  createDefaultTodoFilters,
  formatDateForInput,
  hasMeaningfulTodoQuery,
  isReminderAfterDueDate,
  parseTodoUrlState,
  serializeTodoUrlState,
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
const infoMessage = ref('')
const validationErrors = ref<Record<string, string[]>>({})

const statsOverview = ref<TodoStatsOverview | null>(null)
const statsCategories = ref<TodoStatsCategoryItem[]>([])
const statsTrend = ref<TodoStatsTrendItem[]>([])
const reminders = ref<TodoReminderItem[]>([])
const reminderLoading = ref(false)
const savedViews = ref<TodoSavedView[]>([])
const hasAppliedDefaultSavedView = ref(false)

const CATEGORY_LIST_ID = 'category-options'
const TAG_LIST_ID = 'tag-options'

const newTodo = ref<TodoDraft>({
  title: '',
  priority: 3,
  category: '',
  dueDate: '',
  remindAt: '',
  tags: '',
  notes: '',
  attachmentLinks: '',
  ownerLabel: '',
  collaborators: '',
  watchers: '',
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
  remindAt: '',
  tags: '',
  notes: '',
  attachmentLinks: '',
  ownerLabel: '',
  collaborators: '',
  watchers: '',
  recurrenceType: '',
  recurrenceInterval: 1,
  recurrenceEndTime: ''
})

const filters = ref<TodoFiltersModel>(createDefaultTodoFilters())

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
const syncingFromUrl = ref(false)
let infoMessageTimer: ReturnType<typeof setTimeout> | null = null
const hiddenCreatedTodoId = ref<number | null>(null)

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
  syncUrlState(false)
  loadTodos()
})

watch(locale, (newLocale) => {
  persistLocale(newLocale as AppLocale)
  syncDocumentLocale(newLocale as AppLocale)
})

function handleLocaleUpdate(nextLocale: AppLocale) {
  locale.value = nextLocale
}

function handleDisplayModeUpdate(nextDisplayMode: 'LIST' | 'KANBAN') {
  displayMode.value = viewMode.value === 'ACTIVE' ? nextDisplayMode : 'LIST'
  syncUrlState(false)
}

function handleViewModeUpdate(nextViewMode: 'ACTIVE' | 'RECYCLE_BIN') {
  viewMode.value = nextViewMode
}

function handleFiltersUpdate(nextFilters: TodoFiltersModel) {
  selectedIds.value = []
  filters.value = {
    ...nextFilters,
    page: 0
  }
  syncUrlState(false)
}

function handleNewTodoUpdate(nextDraft: TodoDraft) {
  newTodo.value = nextDraft
}

function handleEditFormUpdate(nextDraft: TodoDraft) {
  editTodoForm.value = nextDraft
}

function showInfoMessage(message: string) {
  infoMessage.value = message
  if (infoMessageTimer) {
    clearTimeout(infoMessageTimer)
  }
  infoMessageTimer = setTimeout(() => {
    infoMessage.value = ''
    hiddenCreatedTodoId.value = null
    infoMessageTimer = null
  }, 2500)
}

function dismissInfoMessage() {
  infoMessage.value = ''
  hiddenCreatedTodoId.value = null
  if (infoMessageTimer) {
    clearTimeout(infoMessageTimer)
    infoMessageTimer = null
  }
}

function clearFiltersAndRevealCreatedTodo() {
  hiddenCreatedTodoId.value = null
  dismissInfoMessage()
  resetFilters()
}

function todoMatchesCurrentFilters(todo: TodoItem) {
  if (filters.value.status && (todo.status || '').toUpperCase() !== filters.value.status.toUpperCase()) {
    return false
  }

  if (filters.value.priority && String(todo.priority || '') !== String(filters.value.priority)) {
    return false
  }

  if (filters.value.category && (todo.category || '').trim().toLowerCase() !== filters.value.category.trim().toLowerCase()) {
    return false
  }

  if (filters.value.keyword) {
    const keyword = filters.value.keyword.trim().toLowerCase()
    const haystack = [todo.title, todo.category, todo.tags].filter(Boolean).join(' ').toLowerCase()
    if (!haystack.includes(keyword)) {
      return false
    }
  }

  if (filters.value.tag) {
    const tag = filters.value.tag.trim().toLowerCase()
    if (!(todo.tags || '').toLowerCase().includes(tag)) {
      return false
    }
  }

  if (filters.value.recurrenceType && (todo.recurrenceType || '').toUpperCase() !== filters.value.recurrenceType.toUpperCase()) {
    return false
  }

  if (filters.value.timePreset) {
    const now = new Date()
    const dueDate = todo.dueDate ? new Date(todo.dueDate) : null
    const remindAt = todo.remindAt ? new Date(todo.remindAt) : null
    switch (filters.value.timePreset) {
      case 'DUE_TODAY': {
        if (!dueDate) return false
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
        const dueDay = new Date(dueDate.getFullYear(), dueDate.getMonth(), dueDate.getDate())
        if (today.getTime() !== dueDay.getTime()) return false
        break
      }
      case 'OVERDUE': {
        if ((todo.status || '').toUpperCase() === 'DONE' || !dueDate || dueDate >= now) return false
        break
      }
      case 'UPCOMING_REMINDER': {
        const nextDay = new Date(now.getTime() + 24 * 60 * 60 * 1000)
        if ((todo.status || '').toUpperCase() === 'DONE' || !remindAt || remindAt < now || remindAt > nextDay) return false
        break
      }
    }
  }

  if (filters.value.dueDateFrom && (!todo.dueDate || todo.dueDate < `${filters.value.dueDateFrom}T00:00:00`)) {
    return false
  }

  if (filters.value.dueDateTo && (!todo.dueDate || todo.dueDate > `${filters.value.dueDateTo}T23:59:59`)) {
    return false
  }

  if (filters.value.remindDateFrom && (!todo.remindAt || todo.remindAt < `${filters.value.remindDateFrom}T00:00:00`)) {
    return false
  }

  if (filters.value.remindDateTo && (!todo.remindAt || todo.remindAt > `${filters.value.remindDateTo}T23:59:59`)) {
    return false
  }

  return true
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

async function loadReminders() {
  reminderLoading.value = true
  try {
    const response = await fetchApi<PageData<TodoReminderItem>>('/api/todo-reminders?status=SENT&page=0&size=10')
    reminders.value = response.data?.content || []
  } catch (error) {
    handleError(error)
  } finally {
    reminderLoading.value = false
  }
}

function serializableFilters() {
  return {
    status: filters.value.status,
    priority: filters.value.priority,
    category: filters.value.category,
    keyword: filters.value.keyword,
    tag: filters.value.tag,
    recurrenceType: filters.value.recurrenceType,
    timePreset: filters.value.timePreset,
    dueDateFrom: filters.value.dueDateFrom,
    dueDateTo: filters.value.dueDateTo,
    remindDateFrom: filters.value.remindDateFrom,
    remindDateTo: filters.value.remindDateTo,
    sortBy: filters.value.sortBy,
    sortDir: filters.value.sortDir,
  }
}

async function loadSavedViews() {
  try {
    const response = await fetchApi<TodoSavedView[]>('/api/todo-saved-views')
    savedViews.value = response.data || []
    return savedViews.value
  } catch (error) {
    handleError(error)
    return []
  }
}

function applySavedView(savedView: TodoSavedView, options: { skipLoad?: boolean } = {}) {
  filters.value = {
    ...filters.value,
    status: savedView.filters.status || '',
    priority: savedView.filters.priority || '',
    category: savedView.filters.category || '',
    keyword: savedView.filters.keyword || '',
    tag: savedView.filters.tag || '',
    recurrenceType: savedView.filters.recurrenceType || '',
    timePreset: savedView.filters.timePreset || '',
    dueDateFrom: savedView.filters.dueDateFrom || '',
    dueDateTo: savedView.filters.dueDateTo || '',
    remindDateFrom: savedView.filters.remindDateFrom || '',
    remindDateTo: savedView.filters.remindDateTo || '',
    sortBy: savedView.filters.sortBy || 'createTime',
    sortDir: savedView.filters.sortDir || 'DESC',
    page: 0,
  }
  syncUrlState(false)
  if (!options.skipLoad) {
    loadTodos()
  }
}

function currentUrlState() {
  return {
    filters: filters.value,
    viewMode: viewMode.value,
    displayMode: displayMode.value,
  }
}

function syncUrlState(replace: boolean) {
  if (typeof window === 'undefined' || syncingFromUrl.value) {
    return
  }

  const query = serializeTodoUrlState(currentUrlState())
  const nextUrl = query ? `${window.location.pathname}?${query}` : window.location.pathname
  const currentUrl = `${window.location.pathname}${window.location.search}`
  if (nextUrl === currentUrl) {
    return
  }

  const state = currentUrlState()
  if (replace) {
    window.history.replaceState(state, '', nextUrl)
  } else {
    window.history.pushState(state, '', nextUrl)
  }
}

function applyUrlState(search: string, options: { skipLoad?: boolean } = {}) {
  syncingFromUrl.value = true
  const parsedState = parseTodoUrlState(search)
  filters.value = parsedState.filters
  viewMode.value = parsedState.viewMode
  displayMode.value = parsedState.displayMode
  syncingFromUrl.value = false

  if (!options.skipLoad) {
    loadTodos()
  }
}

function handlePopState() {
  applyUrlState(window.location.search)
}

async function initializeTodoPage() {
  loadOptions()
  loadReminders()

  const hasUrlState = typeof window !== 'undefined' && hasMeaningfulTodoQuery(window.location.search)
  if (hasUrlState) {
    applyUrlState(window.location.search, { skipLoad: true })
  }

  const loadedSavedViews = await loadSavedViews()
  if (!hasAppliedDefaultSavedView.value && !hasUrlState) {
    const defaultSavedView = loadedSavedViews.find((savedView) => savedView.isDefault)
    if (defaultSavedView) {
      applySavedView(defaultSavedView, { skipLoad: true })
    }
    hasAppliedDefaultSavedView.value = true
  } else {
    hasAppliedDefaultSavedView.value = true
  }

  syncUrlState(true)
  await loadTodos()
}

async function saveCurrentView() {
  const name = window.prompt(t('savedViews.promptName'))?.trim()
  if (!name) return

  try {
    await fetchApi<TodoSavedView>('/api/todo-saved-views', {
      method: 'POST',
      body: JSON.stringify({
        name,
        isDefault: false,
        filters: serializableFilters(),
      }),
    })
    await loadSavedViews()
  } catch (error) {
    handleError(error)
  }
}

async function renameSavedView(savedView: TodoSavedView) {
  const name = window.prompt(t('savedViews.promptRename'), savedView.name)?.trim()
  if (!name) return

  try {
    await fetchApi<TodoSavedView>(`/api/todo-saved-views/${savedView.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        name,
        isDefault: savedView.isDefault,
        filters: savedView.filters,
      }),
    })
    await loadSavedViews()
  } catch (error) {
    handleError(error)
  }
}

async function deleteSavedView(id: number) {
  if (!window.confirm(t('savedViews.confirmDelete'))) return
  try {
    await fetchApi(`/api/todo-saved-views/${id}`, { method: 'DELETE' })
    await loadSavedViews()
  } catch (error) {
    handleError(error)
  }
}

async function setDefaultSavedView(id: number) {
  try {
    await fetchApi(`/api/todo-saved-views/${id}/default`, { method: 'POST' })
    await loadSavedViews()
  } catch (error) {
    handleError(error)
  }
}

async function markReminderAsRead(id: number) {
  try {
    await fetchApi(`/api/todo-reminders/${id}/read`, { method: 'POST' })
    await Promise.all([loadReminders(), loadStats()])
  } catch (error) {
    handleError(error)
  }
}

async function markAllRemindersAsRead() {
  try {
    await fetchApi('/api/todo-reminders/read-all', { method: 'POST' })
    await Promise.all([loadReminders(), loadStats()])
  } catch (error) {
    handleError(error)
  }
}

function openReminderTodo(todoId: number) {
  displayMode.value = 'LIST'
  const targetTodo = todos.value.find((todo) => todo.id === todoId)
  if (targetTodo) {
    startEdit(targetTodo)
  }
}

function resetFilters() {
  selectedIds.value = []
  filters.value = createDefaultTodoFilters()
  syncUrlState(false)
  loadTodos()
}

function prevPage() {
  if (pageData.value && !pageData.value.first) {
    filters.value.page--
    syncUrlState(false)
    loadTodos()
  }
}

function nextPage() {
  if (pageData.value && !pageData.value.last) {
    filters.value.page++
    syncUrlState(false)
    loadTodos()
  }
}

async function createTodo() {
  if (!newTodo.value.title.trim()) return

  if (isReminderAfterDueDate(newTodo.value.remindAt, newTodo.value.dueDate)) {
    errorMessage.value = t('feedback.reminderAfterDueDate')
    validationErrors.value = {}
    return
  }
  
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
          remindAt: toDateTimeValue(newTodo.value.remindAt),
          tags: newTodo.value.tags,
          notes: newTodo.value.notes,
          attachmentLinks: newTodo.value.attachmentLinks,
          ownerLabel: newTodo.value.ownerLabel,
          collaborators: newTodo.value.collaborators,
          watchers: newTodo.value.watchers,
          recurrenceType: newTodo.value.recurrenceType || undefined,
          recurrenceInterval: newTodo.value.recurrenceType ? (newTodo.value.recurrenceInterval || 1) : undefined,
          recurrenceEndTime: newTodo.value.recurrenceType ? toDateTimeValue(newTodo.value.recurrenceEndTime || '') : undefined
        }
    
    const createdResponse = await fetchApi<TodoItem>('/api/todos', {
      method: 'POST',
      body: JSON.stringify(payload),
    })

    const createdTodo = createdResponse.data || null
    
    await loadTodos()
    if (createdTodo && !todoMatchesCurrentFilters(createdTodo)) {
      hiddenCreatedTodoId.value = createdTodo.id
      showInfoMessage(t('feedback.createdTodoHiddenByFilters'))
    }
    newTodo.value = { title: '', priority: 3, category: '', dueDate: '', remindAt: '', tags: '', notes: '', attachmentLinks: '', ownerLabel: '', collaborators: '', watchers: '', recurrenceType: '', recurrenceInterval: 1, recurrenceEndTime: '' }
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
      remindAt: formatDateForInput(todo.remindAt),
      tags: todo.tags || '',
      notes: todo.notes || '',
      attachmentLinks: todo.attachmentLinks || '',
      ownerLabel: todo.ownerLabel || '',
      collaborators: todo.collaborators || '',
      watchers: todo.watchers || '',
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

  if (isReminderAfterDueDate(editTodoForm.value.remindAt, editTodoForm.value.dueDate)) {
    errorMessage.value = t('feedback.reminderAfterDueDate')
    validationErrors.value = {}
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
        remindAt: toDateTimeValue(editTodoForm.value.remindAt),
        tags: editTodoForm.value.tags,
        notes: editTodoForm.value.notes,
        attachmentLinks: editTodoForm.value.attachmentLinks,
        ownerLabel: editTodoForm.value.ownerLabel,
        collaborators: editTodoForm.value.collaborators,
        watchers: editTodoForm.value.watchers,
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
  await moveTodoToStatus(todo, newStatus)
}

async function moveTodoToStatus(todo: TodoItem, newStatus: 'PENDING' | 'DONE') {
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
        remindAt: todo.remindAt ?? null,
        category: todo.category ?? '',
        tags: todo.tags ?? '',
        notes: todo.notes ?? '',
        attachmentLinks: todo.attachmentLinks ?? '',
        ownerLabel: todo.ownerLabel ?? '',
        collaborators: todo.collaborators ?? '',
        watchers: todo.watchers ?? '',
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

onMounted(() => {
  if (typeof window !== 'undefined') {
    window.addEventListener('popstate', handlePopState)
  }
  initializeTodoPage()
})

onBeforeUnmount(() => {
  if (infoMessageTimer) {
    clearTimeout(infoMessageTimer)
  }
  if (typeof window !== 'undefined') {
    window.removeEventListener('popstate', handlePopState)
  }
})

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
          @update:displayMode="handleDisplayModeUpdate" 
          :pageData="pageData" 
          :pendingCount="pendingCount"
          :loading="loading"
          :viewMode="viewMode"
          :showOptionsPanel="showOptionsPanel"
          :locale="locale as AppLocale"
          @refresh="loadTodos"
          @update:locale="handleLocaleUpdate"
          @update:viewMode="handleViewModeUpdate"
          @update:showOptionsPanel="showOptionsPanel = $event"
        />
      </div>

      <datalist id="category-options">
        <option v-for="c in options.categories" :key="c" :value="c"></option>
      </datalist>
      <datalist id="tag-options">
        <option v-for="t in options.tags" :key="t" :value="t"></option>
      </datalist>

      <div class="workbench-body">
        <aside class="workbench-menu">
          <div class="workbench-menu-panel">
            <div class="workbench-menu-section">
              <span class="workbench-menu-label">{{ $t('app.title') }}</span>
              <div class="workbench-menu-group">
                <button
                  type="button"
                  class="btn btn-outline workbench-menu-button"
                  :class="{ 'is-active': viewMode === 'ACTIVE' }"
                  :aria-pressed="viewMode === 'ACTIVE'"
                  @click="handleViewModeUpdate('ACTIVE')"
                >
                  {{ $t('app.activeTasks') }}
                </button>
                <button
                  type="button"
                  class="btn btn-outline workbench-menu-button"
                  :class="{ 'is-active': viewMode === 'RECYCLE_BIN' }"
                  :aria-pressed="viewMode === 'RECYCLE_BIN'"
                  @click="handleViewModeUpdate('RECYCLE_BIN')"
                >
                  {{ $t('app.recycleBin') }}
                </button>
              </div>
            </div>

            <div class="workbench-menu-section">
              <span class="workbench-menu-label">{{ $t('app.listView') }} / {{ $t('app.kanbanView') }}</span>
              <div class="workbench-menu-group">
                <button
                  type="button"
                  class="btn btn-outline workbench-menu-button"
                  :class="{ 'is-active': displayMode === 'LIST' }"
                  :aria-pressed="displayMode === 'LIST'"
                  @click="handleDisplayModeUpdate('LIST')"
                >
                  {{ $t('app.listView') }}
                </button>
                <button
                  type="button"
                  class="btn btn-outline workbench-menu-button"
                  :class="{ 'is-active': displayMode === 'KANBAN' }"
                  :aria-pressed="displayMode === 'KANBAN'"
                  :disabled="viewMode !== 'ACTIVE'"
                  @click="handleDisplayModeUpdate('KANBAN')"
                >
                  {{ $t('app.kanbanView') }}
                </button>
              </div>
            </div>

            <div class="workbench-menu-section">
              <span class="workbench-menu-label">{{ $t('options.knownCategories') }}</span>
              <div class="workbench-menu-group">
                <button
                  type="button"
                  class="btn btn-outline workbench-menu-button"
                  :class="{ 'is-active': showOptionsPanel }"
                  :aria-pressed="showOptionsPanel"
                  @click="showOptionsPanel = !showOptionsPanel"
                >
                  {{ $t('app.manageCategories') }}
                </button>
              </div>
            </div>
          </div>
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

          <Transition name="fade-banner">
            <div v-if="infoMessage" class="info-banner">
              <span>{{ infoMessage }}</span>
              <button v-if="hiddenCreatedTodoId" type="button" class="btn btn-sm btn-outline info-banner-action" @click="clearFiltersAndRevealCreatedTodo">{{ $t('filter.reset') }}</button>
              <button type="button" class="info-banner-close" :aria-label="$t('action.closeInfo')" @click="dismissInfoMessage">×</button>
            </div>
          </Transition>

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
            @moveTodo="moveTodoToStatus"
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

        <aside class="workbench-sidebar">
          <TodoStatsPanel 
            v-if="viewMode === 'ACTIVE'"
            :overview="statsOverview"
            :categories="statsCategories"
            :trend="statsTrend"
          />

          <TodoReminderPanel
            v-if="viewMode === 'ACTIVE'"
            :reminders="reminders"
            :loading="reminderLoading"
            @mark-read="markReminderAsRead"
            @mark-all-read="markAllRemindersAsRead"
            @open-todo="openReminderTodo"
          />

          <TodoSavedViewsBar
            v-if="viewMode === 'ACTIVE'"
            :savedViews="savedViews"
            @apply="applySavedView"
            @set-default="setDefaultSavedView"
            @rename="renameSavedView"
            @delete="deleteSavedView"
          />

          <TodoFilters 
            v-if="displayMode === 'LIST'"
            :filters="filters"
            :categoryListId="CATEGORY_LIST_ID"
            :tagListId="TAG_LIST_ID"
            @update:filters="handleFiltersUpdate"
            @loadTodos="loadTodos"
            @resetFilters="resetFilters"
            @saveCurrentView="saveCurrentView"
          />
        </aside>
      </div>
    </div>
  </section>
</template>
