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
                :class="{ 'is-done': todo.status === 'DONE' }"
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
  padding: 18px;
  background: linear-gradient(180deg, rgba(255,255,255,0.04), rgba(255,255,255,0.02));
  border-radius: var(--radius-xl);
  border: 1px solid rgba(255,255,255,0.08);
  box-shadow: 0 14px 30px rgba(0,0,0,0.16), inset 0 1px 0 rgba(255,255,255,0.04);
  backdrop-filter: blur(12px);
  overflow: hidden;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
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
  margin: 0 4px;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-strong);
  letter-spacing: -0.02em;
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
.empty-month-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  color: var(--color-text-muted);
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  padding: 6px 10px;
  border-radius: var(--radius-full);
}

.calendar-summary-badge {
  color: var(--color-primary);
  background: rgba(56, 189, 248, 0.12);
  border-color: rgba(56, 189, 248, 0.24);
}

.calendar-empty-state {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  margin-bottom: 18px;
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  border: 1px dashed rgba(255,255,255,0.12);
  background: rgba(255,255,255,0.03);
  color: var(--color-text-muted);
}

.calendar-empty-state strong {
  color: var(--color-text-strong);
}

/* Allow horizontal scroll on smaller screens */
.calendar-grid-wrapper {
  flex: 1;
  overflow: auto;
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: var(--radius-lg);
  background: rgba(255,255,255,0.02);
}

.calendar-grid {
  display: flex;
  flex-direction: column;
  min-width: 700px; /* Force minimum width for mobile responsiveness */
  height: 100%;
}

.calendar-grid-header {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  background: rgba(255,255,255,0.04);
  border-bottom: 1px solid rgba(255,255,255,0.08);
  flex-shrink: 0;
}

.calendar-weekday {
  padding: 10px 8px;
  text-align: center;
  font-weight: 600;
  font-size: 0.75rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  border-right: 1px solid rgba(255,255,255,0.06);
}
.calendar-weekday:last-child {
  border-right: none;
}

.calendar-grid-body {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  grid-template-rows: repeat(6, minmax(96px, 1fr));
  flex: 1;
}

.calendar-day {
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255,255,255,0.06);
  border-bottom: 1px solid rgba(255,255,255,0.06);
  padding: 6px;
  background: rgba(255,255,255,0.015);
  transition: background-color 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.calendar-day:nth-child(7n) {
  border-right: none;
}
.calendar-day:nth-last-child(-n+7) {
  border-bottom: none;
}

.calendar-day.not-current-month {
  background: rgba(255,255,255,0.008);
}

.calendar-day.not-current-month .calendar-event {
  opacity: 0.55;
}

.calendar-day.is-today {
  background: rgba(56, 189, 248, 0.06);
  box-shadow: inset 0 0 0 1px rgba(56, 189, 248, 0.32), 0 0 0 1px rgba(56, 189, 248, 0.12);
  z-index: 1;
}

.calendar-day.has-overdue {
  box-shadow: inset 0 0 0 1px rgba(239, 68, 68, 0.18);
}

.calendar-day.is-empty .calendar-day-content {
  opacity: 0.72;
}

.calendar-day.is-today .day-number {
  background: var(--color-primary-gradient);
  color: var(--color-text-inverse);
  border-radius: 50%;
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  box-shadow: var(--shadow-primary);
}

.calendar-day:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
}

.calendar-day-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 2px 2px 8px;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.calendar-day-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 700;
  background: rgba(255,255,255,0.06);
  color: var(--color-text-muted);
}

.calendar-day-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  overflow: hidden;
}

.calendar-event {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 28px;
  padding: 5px 8px;
  background: rgba(255,255,255,0.045);
  border-radius: var(--radius-md);
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid rgba(255,255,255,0.06);
  border-left: 3px solid var(--color-primary);
  user-select: none;
}

.calendar-event:hover, .calendar-event:focus-visible {
  background: rgba(255,255,255,0.08);
  border-color: rgba(56, 189, 248, 0.24);
  transform: translateX(2px);
  outline: none;
}

.calendar-event.is-done {
  opacity: 0.6;
  border-left-color: var(--color-success);
  text-decoration: line-through;
}

.event-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.status-toggle {
  width: 16px;
  height: 16px;
  border-radius: 3px;
  border: 1px solid currentColor;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  padding: 0;
  background: transparent;
  cursor: pointer;
}

.status-toggle:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 1px;
}

.status-done {
  background: currentColor;
}

.calendar-more-indicator {
  margin-top: auto;
  font-size: 0.72rem;
  color: var(--color-text-muted);
  text-align: center;
  padding: 4px 0 0;
  font-weight: 600;
}

.priority-4, .priority-5 { color: var(--color-danger); }
.priority-3 { color: #f59e0b; }
.priority-2 { color: var(--color-primary); }
.priority-1, .priority-0 { color: var(--color-text-muted); }

@media (max-width: 640px) {
  .calendar-board {
    min-height: 560px;
    padding: 12px;
  }

  .calendar-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .calendar-nav {
    width: 100%;
  }

  .calendar-title {
    width: 100%;
    margin: 0;
  }

  .calendar-grid {
    min-width: 640px;
  }

  .calendar-day {
    padding: 4px;
  }

  .calendar-event {
    min-height: 26px;
    padding: 4px 6px;
  }
}
</style>
