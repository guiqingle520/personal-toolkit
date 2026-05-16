<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TodoItem } from './types'

const props = defineProps<{
  todos: TodoItem[]
}>()

defineEmits<{
  (e: 'startEdit', todo: TodoItem): void
  (e: 'toggleStatus', todo: TodoItem): void
}>()

const { t, locale } = useI18n()
const MAX_VISIBLE_EVENTS = 3

// Use current date for the month view
const currentDate = ref(new Date())
const today = ref(new Date())

const currentYear = computed(() => currentDate.value.getFullYear())
const currentMonth = computed(() => currentDate.value.getMonth())
const intlLocale = computed(() => locale.value.startsWith('zh') ? 'zh-CN' : 'en-US')

const currentMonthName = computed(() => {
  const formatter = new Intl.DateTimeFormat(intlLocale.value, { month: 'long' })
  return formatter.format(new Date(2000, currentMonth.value, 1))
})

function prevMonth() {
  currentDate.value = new Date(currentYear.value, currentMonth.value - 1, 1)
}

function nextMonth() {
  currentDate.value = new Date(currentYear.value, currentMonth.value + 1, 1)
}

function resetToToday() {
  currentDate.value = new Date(today.value)
}

// Generate calendar grid
const calendarDays = computed(() => {
  const days = []
  const firstDayOfMonth = new Date(currentYear.value, currentMonth.value, 1)
  const lastDayOfMonth = new Date(currentYear.value, currentMonth.value + 1, 0)
  
  // Week starts on Monday (1). Sunday is 0.
  let startDayOfWeek = firstDayOfMonth.getDay() - 1
  if (startDayOfWeek === -1) startDayOfWeek = 6 // Sunday becomes 6
  
  // Previous month days
  const prevMonthLastDay = new Date(currentYear.value, currentMonth.value, 0).getDate()
  for (let i = startDayOfWeek - 1; i >= 0; i--) {
      const d = prevMonthLastDay - i
      days.push({
        date: new Date(currentYear.value, currentMonth.value - 1, d),
        isCurrentMonth: false,
        isToday: isSameDay(new Date(currentYear.value, currentMonth.value - 1, d), today.value),
        dayNumber: d
      })
  }
  
  // Current month days
  for (let d = 1; d <= lastDayOfMonth.getDate(); d++) {
    const date = new Date(currentYear.value, currentMonth.value, d)
      days.push({
        date,
        isCurrentMonth: true,
        isToday: isSameDay(date, today.value),
        dayNumber: d
      })
  }
  
  // Next month days to fill grid (6 rows of 7 days = 42 days)
  const remainingDays = 42 - days.length
  for (let d = 1; d <= remainingDays; d++) {
      days.push({
        date: new Date(currentYear.value, currentMonth.value + 1, d),
        isCurrentMonth: false,
        isToday: isSameDay(new Date(currentYear.value, currentMonth.value + 1, d), today.value),
        dayNumber: d
      })
  }
  
  return days
})

const todosByDate = computed(() => {
  const map = new Map<string, TodoItem[]>()
  props.todos.forEach(todo => {
    if (todo.dueDate) {
      // dueDate format typically "YYYY-MM-DDThh:mm:ss" or similar
      const d = new Date(todo.dueDate)
      const dateKey = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      if (!map.has(dateKey)) {
        map.set(dateKey, [])
      }
      map.get(dateKey)!.push(todo)
    }
  })
  return map
})

function getTodosForDate(date: Date) {
  const dateKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  return todosByDate.value.get(dateKey) || []
}

function getVisibleTodosForDate(date: Date) {
  return getTodosForDate(date).slice(0, MAX_VISIBLE_EVENTS)
}

function getHiddenTodosCount(date: Date) {
  return Math.max(0, getTodosForDate(date).length - MAX_VISIBLE_EVENTS)
}

function isSameDay(d1: Date, d2: Date) {
  return d1.getFullYear() === d2.getFullYear() &&
         d1.getMonth() === d2.getMonth() &&
         d1.getDate() === d2.getDate()
}

function hasOverdueActiveTodos(date: Date) {
  return getTodosForDate(date).some((todo) => todo.status !== 'DONE' && date < today.value && !isSameDay(date, today.value))
}

function formatDayLabel(date: Date) {
  return new Intl.DateTimeFormat(intlLocale.value, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  }).format(date)
}

function formatEventLabel(todo: TodoItem) {
  const statusKey = `status.${String(todo.status).toUpperCase()}`
  return `${todo.title}, ${t(statusKey)}`
}

const currentMonthTodos = computed(() => {
  return props.todos.filter((todo) => {
    if (!todo.dueDate) {
      return false
    }

    const dueDate = new Date(todo.dueDate)
    return dueDate.getFullYear() === currentYear.value && dueDate.getMonth() === currentMonth.value
  })
})

const hasEventsInCurrentMonth = computed(() => {
  return currentMonthTodos.value.length > 0
})

const currentMonthEventCount = computed(() => currentMonthTodos.value.length)

function handleEventKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    event.stopPropagation()
    ;(event.currentTarget as HTMLElement | null)?.click()
  }
}

function handleStatusKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    event.stopPropagation()
    ;(event.currentTarget as HTMLElement | null)?.click()
  }
}

</script>

<template>
  <div class="calendar-board">
    <div class="calendar-header">
      <div class="calendar-nav">
        <button type="button" class="btn btn-outline btn-sm calendar-nav-button" :title="t('calendar.today')" :aria-label="t('calendar.today')" @click="resetToToday">{{ t('calendar.today') }}</button>
        <button type="button" class="btn btn-outline btn-sm calendar-nav-button calendar-nav-button--icon" :title="t('calendar.prevMonth')" :aria-label="t('calendar.prevMonth')" @click="prevMonth">&lt;</button>
        <button type="button" class="btn btn-outline btn-sm calendar-nav-button calendar-nav-button--icon" :title="t('calendar.nextMonth')" :aria-label="t('calendar.nextMonth')" @click="nextMonth">&gt;</button>
        <h2 class="calendar-title" aria-live="polite">{{ currentMonthName }} {{ currentYear }}</h2>
      </div>
      <div v-if="hasEventsInCurrentMonth" class="calendar-summary-badge" role="status">
        {{ t('calendar.eventsCount', { count: currentMonthEventCount }) }}
      </div>
      <div v-else class="empty-month-badge" role="status">
        {{ t('calendar.emptyMonth') }}
      </div>
    </div>

    <div v-if="!hasEventsInCurrentMonth" class="calendar-empty-state" role="status">
      <strong>{{ t('calendar.emptyMonth') }}</strong>
      <span>{{ t('status.empty') }}</span>
    </div>
    
    <div class="calendar-grid-wrapper">
      <div class="calendar-grid" role="grid" :aria-label="`${currentMonthName} ${currentYear}`">
        <div class="calendar-grid-header" role="row">
          <div class="calendar-weekday" role="columnheader" :aria-label="t('calendar.mon')">{{ t('calendar.mon') }}</div>
          <div class="calendar-weekday" role="columnheader" :aria-label="t('calendar.tue')">{{ t('calendar.tue') }}</div>
          <div class="calendar-weekday" role="columnheader" :aria-label="t('calendar.wed')">{{ t('calendar.wed') }}</div>
          <div class="calendar-weekday" role="columnheader" :aria-label="t('calendar.thu')">{{ t('calendar.thu') }}</div>
          <div class="calendar-weekday" role="columnheader" :aria-label="t('calendar.fri')">{{ t('calendar.fri') }}</div>
          <div class="calendar-weekday" role="columnheader" :aria-label="t('calendar.sat')">{{ t('calendar.sat') }}</div>
          <div class="calendar-weekday" role="columnheader" :aria-label="t('calendar.sun')">{{ t('calendar.sun') }}</div>
        </div>
        
        <div class="calendar-grid-body" role="rowgroup">
          <div 
            v-for="(day) in calendarDays" 
            :key="day.date.toISOString()"
            class="calendar-day"
            :class="{ 
              'not-current-month': !day.isCurrentMonth, 
              'is-today': day.isToday,
              'has-events': getTodosForDate(day.date).length > 0,
              'is-empty': getTodosForDate(day.date).length === 0,
              'has-overflow': getHiddenTodosCount(day.date) > 0,
              'has-overdue': hasOverdueActiveTodos(day.date)
            }"
            role="gridcell"
            :aria-current="day.isToday ? 'date' : undefined"
            :tabindex="day.isToday ? 0 : -1"
          >
            <div class="calendar-day-header">
              <span class="day-number" :aria-label="formatDayLabel(day.date)">{{ day.dayNumber }}</span>
              <span v-if="getTodosForDate(day.date).length > 0" class="calendar-day-count">{{ getTodosForDate(day.date).length }}</span>
            </div>
            <div class="calendar-day-content">
              <div 
                v-for="todo in getVisibleTodosForDate(day.date)" 
                :key="todo.id" 
                class="calendar-event"
                :class="{ 'is-done': todo.status === 'DONE', 'is-pending': todo.status !== 'DONE' }"
                role="button"
                tabindex="0"
                :aria-label="formatEventLabel(todo)"
                :title="todo.title"
                @click="$emit('startEdit', todo)"
                @keydown="handleEventKeydown($event)"
              >
                <button
                  type="button"
                  class="status-toggle" 
                  :class="[
                    todo.status === 'DONE' ? 'status-done' : 'status-pending',
                    `priority-${todo.priority ?? 3}`
                  ]"
                  :aria-label="todo.status === 'DONE' ? t('status.markAsPending') : t('status.markAsDone')"
                  :title="todo.status === 'DONE' ? t('status.markAsPending') : t('status.markAsDone')"
                  @click.stop="$emit('toggleStatus', todo)"
                  @keydown="handleStatusKeydown($event)"
                >
                  <svg v-if="todo.status === 'DONE'" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true"><path d="M12.207 4.793a1 1 0 010 1.414l-5 5a1 1 0 01-1.414 0l-2-2a1 1 0 011.414-1.414L6.5 9.086l4.293-4.293a1 1 0 011.414 0z"/></svg>
                </button>
                <span class="event-title">{{ todo.title }}</span>
              </div>
              <div 
                v-if="getHiddenTodosCount(day.date) > 0"
                class="calendar-more-indicator"
              >
                {{ t('calendar.more', { count: getHiddenTodosCount(day.date) }) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.calendar-board {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 680px;
  padding: 24px;
  background: var(--color-surface-base);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-panel);
  overflow: hidden;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 12px;
}

.calendar-nav {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.calendar-title {
  margin: 0 8px;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-strong);
}

.calendar-nav-button {
  min-height: 36px;
}

.calendar-nav-button--icon {
  min-width: 36px;
  padding-inline: 0;
  justify-content: center;
}

.calendar-summary-badge,
.calendar-today-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-size: 0.85rem;
  font-weight: 600;
}

.calendar-summary-badge {
  background: var(--color-surface-active);
  color: var(--color-text-muted);
}

.calendar-today-badge {
  background: var(--color-primary-shadow);
  color: var(--color-primary-dark);
}

.calendar-grid {
  display: flex;
  flex-direction: column;
  flex: 1;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--color-surface-base);
}

.calendar-grid-wrapper {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.calendar-grid-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: var(--color-surface-hover);
  border-bottom: 1px solid var(--color-border);
}

.calendar-weekday {
  padding: 12px;
  text-align: center;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-right: 1px solid var(--color-border);
}
.calendar-weekday:last-child {
  border-right: none;
}

.calendar-grid-body {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  flex: 1;
  min-height: 0;
}

.calendar-day {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 120px;
  background: var(--color-surface-base);
  transition: background 0.2s ease;
  border-right: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}

.calendar-day:nth-child(7n) {
  border-right: none;
}

.calendar-day:nth-last-child(-n + 7) {
  border-bottom: none;
}

.calendar-day:hover {
  background: var(--color-surface-hover);
}

.calendar-day.not-current-month {
  background: var(--color-surface-card-hover);
  opacity: 0.6;
}

.calendar-day.is-today {
  background: var(--color-primary-shadow);
}

.calendar-day-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.day-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  font-size: 0.95rem;
  font-weight: 500;
  color: var(--color-text-strong);
}

.calendar-day.is-today .day-number {
  background: var(--color-primary);
  color: #fff;
  font-weight: 700;
}

.calendar-day.not-current-month .day-number {
  color: var(--color-text-muted);
}

.calendar-day-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: var(--radius-full);
  background: var(--color-surface-active);
  color: var(--color-text-muted);
  font-size: 0.72rem;
  font-weight: 700;
}

.calendar-day-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.calendar-day-content::-webkit-scrollbar {
  width: 4px;
}
.calendar-day-content::-webkit-scrollbar-track {
  background: transparent;
}
.calendar-day-content::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 4px;
}

.calendar-event {
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  border: 1px solid transparent;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.calendar-event:hover {
  transform: translateY(-1px);
  filter: brightness(0.95);
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.calendar-event.is-pending {
  background: var(--color-surface-active);
  color: var(--color-text-strong);
  border-color: var(--color-border);
}
.calendar-event.is-pending:hover {
  background: var(--color-border);
}

.calendar-event.is-done {
  background: var(--color-surface-hover);
  color: var(--color-text-muted);
  text-decoration: line-through;
  opacity: 0.7;
}

.calendar-event.is-overdue {
  background: var(--color-danger-bg);
  color: var(--color-text-danger-strong);
  border-color: var(--color-danger-border);
}

.calendar-event-time {
  font-size: 0.7rem;
  opacity: 0.7;
  font-weight: 600;
}

.calendar-more-indicator {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  padding: 2px 4px;
  text-align: center;
  background: var(--color-surface-hover);
  border-radius: var(--radius-sm);
  cursor: pointer;
  margin-top: 2px;
}

.calendar-more-indicator:hover {
  background: var(--color-surface-active);
  color: var(--color-text-strong);
}

.calendar-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: var(--color-text-muted);
  padding: 40px;
  text-align: center;
  border-radius: var(--radius-lg);
  background: var(--color-surface-hover);
  border: 1px dashed var(--color-border);
}

@media (max-width: 768px) {
  .calendar-board {
    min-height: auto;
    padding: 16px;
  }
  .calendar-header {
    flex-direction: column;
    align-items: stretch;
  }
  .calendar-weekday {
    font-size: 0.7rem;
    padding: 8px 4px;
  }
  .calendar-grid-body {
    grid-template-columns: repeat(7, minmax(84px, 1fr));
    min-width: 588px;
  }
  .calendar-day {
    min-height: 80px;
  }
  .calendar-day {
    padding: 4px;
  }
  .day-number {
    width: 24px;
    height: 24px;
    font-size: 0.85rem;
  }
  .calendar-event {
    padding: 2px 4px;
    font-size: 0.7rem;
  }
  .calendar-event-time {
    display: none;
  }
}
</style>
