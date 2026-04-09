import { describe, expect, it } from 'vitest'
import {
  formatDateForInput,
  formatPriorityLabel,
  formatRecurrenceLabelKey,
  parseTags,
  priorityBadgeClass,
  toDateTimeValue,
} from './todoView'

describe('todoView helpers', () => {
  it('converts date input to backend datetime', () => {
    expect(toDateTimeValue('2026-04-06')).toBe('2026-04-06T00:00:00')
    expect(toDateTimeValue('')).toBeNull()
  })

  it('formats datetime for date input', () => {
    expect(formatDateForInput('2026-04-06T00:00:00')).toBe('2026-04-06')
    expect(formatDateForInput(undefined)).toBe('')
  })

  it('parses comma separated tags', () => {
    expect(parseTags(' urgent, backend , ,ui')).toEqual(['urgent', 'backend', 'ui'])
    expect(parseTags(undefined)).toEqual([])
  })

  it('maps numeric priorities to labels', () => {
    expect(formatPriorityLabel(5)).toBe('priority.critical')
    expect(formatPriorityLabel(4)).toBe('priority.high')
    expect(formatPriorityLabel(3)).toBe('priority.medium')
    expect(formatPriorityLabel(2)).toBe('priority.low')
    expect(formatPriorityLabel(1)).toBe('priority.backlog')
  })

  it('maps numeric priorities to badge classes', () => {
    expect(priorityBadgeClass(5)).toBe('badge-high')
    expect(priorityBadgeClass(3)).toBe('badge-medium')
    expect(priorityBadgeClass(1)).toBe('badge-low')
    expect(priorityBadgeClass(undefined)).toBe('badge-neutral')
  })

  it('verifies datetime helper returns correctly for recurrence fields', () => {
    expect(toDateTimeValue('2026-10-10')).toBe('2026-10-10T00:00:00')
  })

  it('maps recurrence values to locale keys regardless of input case', () => {
    expect(formatRecurrenceLabelKey('DAILY')).toBe('recurrence.daily')
    expect(formatRecurrenceLabelKey('weekly')).toBe('recurrence.weekly')
    expect(formatRecurrenceLabelKey(undefined)).toBe('recurrence.none')
  })
})
