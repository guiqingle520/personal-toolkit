<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

type CalendarCell = {
  date: Date
  day: number
  inCurrentMonth: boolean
  key: string
}

const props = defineProps<{
  modelValue?: string | null
  disabled?: boolean
  placeholder?: string
  class?: string
  min?: string
  max?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string): void
}>()

const { locale } = useI18n()

const isOpen = ref(false)
const viewDate = ref(new Date())
const selectedDateStr = ref<string | null>(props.modelValue || null)
const focusedDate = ref<Date | null>(null)

const containerRef = ref<HTMLElement | null>(null)
const triggerRef = ref<HTMLElement | null>(null)
const popoverRef = ref<HTMLElement | null>(null)

const alignRight = ref(false)
const showAbove = ref(false)

const labels = computed(() => {
  if (locale.value === 'zh-CN') {
    return {
      clear: '清空日期',
      open: '打开日期选择器',
      previousMonth: '上个月',
      nextMonth: '下个月',
      calendar: '日期选择器',
    }
  }

  return {
    clear: 'Clear date',
    open: 'Open date picker',
    previousMonth: 'Previous month',
    nextMonth: 'Next month',
    calendar: 'Date picker',
  }
})

function padDate(value: number): string {
  return String(value).padStart(2, '0')
}

function toYmd(date: Date): string {
  return `${date.getFullYear()}-${padDate(date.getMonth() + 1)}-${padDate(date.getDate())}`
}

function parseYmd(value: string | null | undefined): Date | null {
  if (!value) {
    return null
  }

  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) {
    return null
  }

  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])

  if (month < 1 || month > 12 || day < 1 || day > 31) {
    return null
  }

  const candidate = new Date(year, month - 1, day)
  if (
    candidate.getFullYear() !== year
    || candidate.getMonth() !== month - 1
    || candidate.getDate() !== day
  ) {
    return null
  }

  return candidate
}

function addDays(date: Date, delta: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + delta)
}

function addMonths(date: Date, delta: number): Date {
  const year = date.getFullYear()
  const month = date.getMonth() + delta
  const day = date.getDate()

  const candidate = new Date(year, month, 1)
  const maxDay = new Date(candidate.getFullYear(), candidate.getMonth() + 1, 0).getDate()
  return new Date(candidate.getFullYear(), candidate.getMonth(), Math.min(day, maxDay))
}

function sameDate(a: Date | null, b: Date | null): boolean {
  if (!a || !b) {
    return false
  }

  return a.getFullYear() === b.getFullYear()
    && a.getMonth() === b.getMonth()
    && a.getDate() === b.getDate()
}

const monthYearLabel = computed(() => {
  return new Intl.DateTimeFormat(locale.value, {
    year: 'numeric',
    month: 'long',
  }).format(viewDate.value)
})

const weekDayLabels = computed(() => {
  const formatter = new Intl.DateTimeFormat(locale.value, { weekday: 'short' })
  const baseSunday = new Date(2021, 7, 1)

  return Array.from({ length: 7 }, (_, index) => formatter.format(addDays(baseSunday, index)))
})

const displayValue = computed(() => {
  const parsed = parseYmd(selectedDateStr.value)
  if (!parsed) {
    return ''
  }

  return new Intl.DateTimeFormat(locale.value, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(parsed)
})

const displayPlaceholder = computed(() => {
  if (props.placeholder) {
    return props.placeholder
  }

  return locale.value === 'zh-CN' ? '选择日期' : 'Select date'
})

const calendarCells = computed<CalendarCell[]>(() => {
  const year = viewDate.value.getFullYear()
  const month = viewDate.value.getMonth()
  const firstDayIndex = new Date(year, month, 1).getDay()
  const firstCellDate = new Date(year, month, 1 - firstDayIndex)

  const cells: CalendarCell[] = []
  for (let i = 0; i < 42; i++) {
    const date = new Date(firstCellDate.getFullYear(), firstCellDate.getMonth(), firstCellDate.getDate() + i)
    cells.push({
      date,
      day: date.getDate(),
      inCurrentMonth: date.getMonth() === month,
      key: toYmd(date),
    })
  }

  return cells
})

function isToday(date: Date): boolean {
  return sameDate(date, new Date())
}

function isSelected(date: Date): boolean {
  const selected = parseYmd(selectedDateStr.value)
  return sameDate(date, selected)
}

function isWithinBounds(date: Date): boolean {
  const minDate = parseYmd(props.min)
  const maxDate = parseYmd(props.max)

  if (minDate && date < minDate) {
    return false
  }

  if (maxDate && date > maxDate) {
    return false
  }

  return true
}

function clampDateValue(value: string | null | undefined): string {
  const parsed = parseYmd(value)
  if (!parsed) {
    return value || ''
  }

  const minDate = parseYmd(props.min)
  if (minDate && parsed < minDate) {
    return toYmd(minDate)
  }

  const maxDate = parseYmd(props.max)
  if (maxDate && parsed > maxDate) {
    return toYmd(maxDate)
  }

  return toYmd(parsed)
}

function isFocused(date: Date): boolean {
  return sameDate(date, focusedDate.value)
}

function focusCalendarDate(date: Date | null): void {
  if (!date) {
    return
  }

  nextTick(() => {
    const key = toYmd(date)
    const button = popoverRef.value?.querySelector<HTMLButtonElement>(`button[data-date="${key}"]`)
    button?.focus()
  })
}

function updatePopoverPlacement(): void {
  const containerRect = containerRef.value?.getBoundingClientRect()
  const popoverRect = popoverRef.value?.getBoundingClientRect()
  if (!containerRect || !popoverRect) {
    return
  }

  alignRight.value = containerRect.left + popoverRect.width > window.innerWidth - 12
  const wouldClipLeft = containerRect.right - popoverRect.width < 12
  if (alignRight.value && wouldClipLeft) {
    alignRight.value = false
  }

  showAbove.value = containerRect.bottom + popoverRect.height > window.innerHeight - 12
    && containerRect.top > popoverRect.height + 12
}

function openPopover(): void {
  if (props.disabled) {
    return
  }

  isOpen.value = true

  const selected = parseYmd(selectedDateStr.value)
  const anchorDate = selected ?? new Date()
  viewDate.value = new Date(anchorDate.getFullYear(), anchorDate.getMonth(), 1)
  focusedDate.value = anchorDate

  nextTick(() => {
    updatePopoverPlacement()
    focusCalendarDate(anchorDate)
  })
}

function closePopover(restoreFocus = true): void {
  isOpen.value = false
  if (restoreFocus) {
    nextTick(() => {
      triggerRef.value?.focus()
    })
  }
}

function toggleOpen(): void {
  if (props.disabled) {
    return
  }

  if (isOpen.value) {
    closePopover(false)
  } else {
    openPopover()
  }
}

function emitDateChange(dateValue: string): void {
  selectedDateStr.value = dateValue || null
  emit('update:modelValue', dateValue)
  emit('change', dateValue)
}

function selectDate(date: Date): void {
  if (props.disabled) {
    return
  }

  if (!isWithinBounds(date)) {
    return
  }

  const dateValue = toYmd(date)
  emitDateChange(dateValue)
  closePopover()
}

function clearDate(event: Event): void {
  event.stopPropagation()
  if (props.disabled) {
    return
  }

  emitDateChange('')
  closePopover(false)
}

function prevMonth(event: Event): void {
  event.preventDefault()
  viewDate.value = new Date(viewDate.value.getFullYear(), viewDate.value.getMonth() - 1, 1)
  updatePopoverPlacement()
}

function nextMonth(event: Event): void {
  event.preventDefault()
  viewDate.value = new Date(viewDate.value.getFullYear(), viewDate.value.getMonth() + 1, 1)
  updatePopoverPlacement()
}

function handleTriggerKeydown(event: KeyboardEvent): void {
  if (props.disabled) {
    return
  }

  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    toggleOpen()
    return
  }

  if (event.key === 'ArrowDown' && !isOpen.value) {
    event.preventDefault()
    openPopover()
  }
}

function handleCalendarKeydown(event: KeyboardEvent): void {
  if (!isOpen.value || !focusedDate.value) {
    return
  }

  let nextFocused: Date | null = null

  switch (event.key) {
    case 'ArrowLeft':
      nextFocused = addDays(focusedDate.value, -1)
      break
    case 'ArrowRight':
      nextFocused = addDays(focusedDate.value, 1)
      break
    case 'ArrowUp':
      nextFocused = addDays(focusedDate.value, -7)
      break
    case 'ArrowDown':
      nextFocused = addDays(focusedDate.value, 7)
      break
    case 'Home':
      nextFocused = new Date(viewDate.value.getFullYear(), viewDate.value.getMonth(), 1)
      break
    case 'End':
      nextFocused = new Date(viewDate.value.getFullYear(), viewDate.value.getMonth() + 1, 0)
      break
    case 'PageUp':
      nextFocused = addMonths(focusedDate.value, -1)
      break
    case 'PageDown':
      nextFocused = addMonths(focusedDate.value, 1)
      break
    case 'Enter':
    case ' ':
      event.preventDefault()
      selectDate(focusedDate.value)
      return
    case 'Escape':
      event.preventDefault()
      closePopover()
      return
    default:
      return
  }

  event.preventDefault()

  if (!nextFocused) {
    return
  }

  focusedDate.value = nextFocused
  viewDate.value = new Date(nextFocused.getFullYear(), nextFocused.getMonth(), 1)
  focusCalendarDate(nextFocused)
}

function handleOutsidePointer(event: MouseEvent): void {
  if (isOpen.value && containerRef.value && !containerRef.value.contains(event.target as Node)) {
    closePopover(false)
  }
}

function handleGlobalEscape(event: KeyboardEvent): void {
  if (isOpen.value && event.key === 'Escape') {
    closePopover()
  }
}

function handleNativeInput(event: Event): void {
  const target = event.target as HTMLInputElement
  const nextValue = clampDateValue(target.value)
  target.value = nextValue
  selectedDateStr.value = nextValue || null
  emit('update:modelValue', nextValue)
}

function handleNativeChange(event: Event): void {
  const target = event.target as HTMLInputElement
  const nextValue = clampDateValue(target.value)
  target.value = nextValue
  emit('change', nextValue)
}

watch(() => props.modelValue, (newVal) => {
  const normalizedValue = clampDateValue(newVal)
  selectedDateStr.value = normalizedValue || null

  const parsed = parseYmd(normalizedValue)
  if (parsed) {
    viewDate.value = new Date(parsed.getFullYear(), parsed.getMonth(), 1)
  }
}, { immediate: true })

watch(() => [props.min, props.max], () => {
  const normalizedValue = clampDateValue(selectedDateStr.value)
  if (normalizedValue !== (selectedDateStr.value || '')) {
    emitDateChange(normalizedValue)
  }
})

watch(locale, () => {
  if (isOpen.value) {
    nextTick(updatePopoverPlacement)
  }
})

onMounted(() => {
  document.addEventListener('click', handleOutsidePointer)
  document.addEventListener('keydown', handleGlobalEscape)
  window.addEventListener('resize', updatePopoverPlacement)
})

onUnmounted(() => {
  document.removeEventListener('click', handleOutsidePointer)
  document.removeEventListener('keydown', handleGlobalEscape)
  window.removeEventListener('resize', updatePopoverPlacement)
})
</script>

<template>
  <div ref="containerRef" class="localized-date-input-wrapper">
    <input
      type="date"
      class="native-sync-input"
      :value="props.modelValue || ''"
      :min="props.min"
      :max="props.max"
      :disabled="props.disabled"
      @input="handleNativeInput"
      @change="handleNativeChange"
      tabindex="-1"
      aria-hidden="true"
    />

    <div
      ref="triggerRef"
      class="date-input-display"
      :class="[props.class, { 'is-disabled': props.disabled, 'is-open': isOpen }]"
      role="button"
      tabindex="0"
      :aria-haspopup="'dialog'"
      :aria-expanded="isOpen"
      :aria-label="labels.open"
      @click="toggleOpen"
      @keydown="handleTriggerKeydown"
    >
      <span v-if="displayValue" class="date-text">{{ displayValue }}</span>
      <span v-else class="date-placeholder">{{ displayPlaceholder }}</span>

      <div class="date-actions">
        <button
          v-if="selectedDateStr && !props.disabled"
          class="clear-btn"
          type="button"
          :title="labels.clear"
          :aria-label="labels.clear"
          @click="clearDate"
        >
          ×
        </button>
        <svg class="calendar-icon" viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
          <path fill="currentColor" d="M19 4h-1V2h-2v2H8V2H6v2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10zm0-12H5V6h14v2z" />
        </svg>
      </div>
    </div>

    <div
      v-if="isOpen"
      ref="popoverRef"
      class="calendar-popover"
      :class="{ 'align-right': alignRight, 'is-above': showAbove }"
      role="dialog"
      :aria-label="labels.calendar"
      @keydown="handleCalendarKeydown"
    >
      <div class="calendar-header">
        <button class="nav-btn" type="button" :aria-label="labels.previousMonth" @click.stop="prevMonth">&lt;</button>
        <span class="month-year-display">{{ monthYearLabel }}</span>
        <button class="nav-btn" type="button" :aria-label="labels.nextMonth" @click.stop="nextMonth">&gt;</button>
      </div>

      <div class="calendar-grid" role="grid">
        <div v-for="(dayLabel, index) in weekDayLabels" :key="`weekday-${index}`" class="weekday-header" role="columnheader">
          {{ dayLabel }}
        </div>

        <button
          v-for="cell in calendarCells"
          :key="cell.key"
          type="button"
          class="day-cell"
          :class="{
            'is-outside': !cell.inCurrentMonth,
            'is-disabled': !isWithinBounds(cell.date),
            'is-today': isToday(cell.date),
            'is-selected': isSelected(cell.date),
            'is-focused': isFocused(cell.date),
          }"
          :data-date="cell.key"
          :aria-selected="isSelected(cell.date)"
          :disabled="!isWithinBounds(cell.date)"
          :aria-current="isToday(cell.date) ? 'date' : undefined"
          @click.stop="selectDate(cell.date)"
          @focus="focusedDate = cell.date"
        >
          {{ cell.day }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.localized-date-input-wrapper {
  position: relative;
  display: inline-block;
  vertical-align: middle;
}

.native-sync-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}

.date-input-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 32px;
  padding: 0 8px;
  box-sizing: border-box;
  cursor: pointer;
  background-color: var(--color-surface-input, rgba(255, 255, 255, 0.03));
  border: 1px solid var(--color-border, rgba(255, 255, 255, 0.1));
  border-radius: var(--radius-sm, 4px);
  color: var(--color-text-bright, #e4e4e7);
  transition: border-color var(--transition-fast, 0.2s), box-shadow var(--transition-fast, 0.2s);
}

.date-input-display:hover {
  border-color: var(--color-primary, #38bdf8);
}

.date-input-display:focus-visible,
.date-input-display.is-open {
  border-color: var(--color-primary, #38bdf8);
  box-shadow: var(--focus-ring, 0 0 0 2px rgba(56, 189, 248, 0.18));
}

.date-input-display.is-disabled {
  opacity: var(--control-disabled-opacity, 0.5);
  cursor: not-allowed;
  pointer-events: none;
}

.date-text,
.date-placeholder {
  flex: 1;
  font-size: 0.9em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.date-placeholder {
  color: var(--color-text-muted, #71717a);
}

.date-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 4px;
}

.clear-btn {
  border: 0;
  background: transparent;
  color: var(--color-text-muted, #71717a);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  padding: 0 4px;
}

.clear-btn:hover {
  color: var(--color-text-bright, #e4e4e7);
}

.calendar-icon {
  color: var(--color-text-muted, #71717a);
}

.calendar-popover {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  z-index: 1000;
  width: 296px;
  padding: 16px;
  box-sizing: border-box;
  background-color: #0f172a;
  border: 1px solid var(--color-border, rgba(255, 255, 255, 0.1));
  border-radius: var(--radius-md, 8px);
  box-shadow: var(--shadow-panel, 0 24px 64px rgba(0, 0, 0, 0.4));
}

.calendar-popover.align-right {
  left: auto;
  right: 0;
}

.calendar-popover.is-above {
  top: auto;
  bottom: calc(100% + 8px);
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.month-year-display {
  font-weight: 600;
  font-size: 0.95em;
  color: var(--color-text-bright, #e4e4e7);
}

.nav-btn {
  width: 32px;
  height: 32px;
  border: 1px solid var(--color-border-subtle, rgba(255, 255, 255, 0.05));
  border-radius: var(--radius-sm, 4px);
  background: var(--color-surface-base, rgba(255, 255, 255, 0.02));
  color: var(--color-text-normal, #a1a1aa);
  cursor: pointer;
  transition: all var(--transition-fast, 0.2s);
}

.nav-btn:hover,
.nav-btn:focus-visible {
  background-color: var(--color-surface-hover, rgba(255, 255, 255, 0.05));
  border-color: var(--color-border, rgba(255, 255, 255, 0.1));
  color: var(--color-text-bright, #e4e4e7);
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.weekday-header {
  text-align: center;
  font-size: 0.75em;
  font-weight: 600;
  color: var(--color-text-muted, #71717a);
  margin-bottom: 8px;
}

.day-cell {
  height: 32px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm, 4px);
  background: transparent;
  color: var(--color-text-normal, #a1a1aa);
  cursor: pointer;
  font-size: 0.85em;
  transition: all var(--transition-fast, 0.2s);
}

.day-cell:hover {
  background-color: var(--color-surface-hover, rgba(255, 255, 255, 0.05));
  color: var(--color-text-bright, #e4e4e7);
}

.day-cell.is-outside {
  color: rgba(161, 161, 170, 0.45);
}

.day-cell.is-disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.day-cell.is-disabled:hover {
  background: transparent;
  color: var(--color-text-normal, #a1a1aa);
}

.day-cell.is-today {
  border-color: var(--color-border, rgba(255, 255, 255, 0.1));
  color: var(--color-primary, #38bdf8);
  font-weight: 600;
}

.day-cell.is-selected {
  background: var(--color-primary-gradient, linear-gradient(135deg, #3b82f6 0%, #2563eb 100%));
  color: var(--color-text-inverse, #fff);
  border-color: transparent;
  font-weight: 600;
  box-shadow: var(--shadow-primary, 0 4px 12px rgba(37, 99, 235, 0.2));
}

.day-cell.is-focused {
  box-shadow: var(--focus-ring, 0 0 0 2px rgba(56, 189, 248, 0.18));
}

.day-cell.is-selected:hover {
  box-shadow: var(--shadow-primary-hover, 0 6px 16px rgba(37, 99, 235, 0.3));
}
</style>
