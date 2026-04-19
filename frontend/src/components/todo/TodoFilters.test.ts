import { describe, expect, it } from 'vitest'
import TodoFilters from './TodoFilters.vue'
import { mountWithI18n } from '../../test/test-utils'
import type { TodoFiltersModel } from './types'

function createFilters(overrides: Partial<TodoFiltersModel> = {}): TodoFiltersModel {
    return {
      page: 2,
      size: 10,
      status: '',
      priority: '',
      category: '',
      keyword: '',
      tag: '',
      recurrenceType: '',
      timePreset: '',
      dueDateFrom: '',
      dueDateTo: '',
      remindDateFrom: '',
      remindDateTo: '',
      sortBy: 'createTime',
      sortDir: 'DESC',
      ...overrides,
  }
}

describe('TodoFilters', () => {
  it('emits update only for text input until enter is pressed', async () => {
    const wrapper = mountWithI18n(TodoFilters, {
      props: {
        filters: createFilters(),
        categoryListId: 'category-options',
        tagListId: 'tag-options',
      },
    })

    const inputs = wrapper.findAll('input')
    const searchInput = inputs[0]
    await searchInput.setValue('deploy')

    const updateEvents = wrapper.emitted('update:filters')
    expect(updateEvents).toHaveLength(1)
    expect(updateEvents?.[0]?.[0]).toMatchObject({ keyword: 'deploy', page: 2 })
    expect(wrapper.emitted('loadTodos')).toBeUndefined()

    await searchInput.trigger('keyup.enter')
    expect(wrapper.emitted('loadTodos')).toHaveLength(1)
  })

  it('emits update and load for select/date changes, and reset separately', async () => {
    const wrapper = mountWithI18n(TodoFilters, {
      props: {
        filters: createFilters(),
        categoryListId: 'category-options',
        tagListId: 'tag-options',
      },
    })

    const statusSelect = wrapper.findAll('select')[0]
    const prioritySelect = wrapper.findAll('select')[1]
    const recurrenceSelect = wrapper.findAll('select')[2]
    await statusSelect.setValue('DONE')
    await prioritySelect.setValue('4')
    await recurrenceSelect.setValue('DAILY')

    const dateInputs = wrapper.findAll('.localized-date-input-wrapper input')
    await dateInputs[0].setValue('2026-04-07')
    await dateInputs[2].setValue('2026-04-09')

    const updates = wrapper.emitted('update:filters')
    expect(updates).toHaveLength(5)
    expect(updates?.[0]?.[0]).toMatchObject({ status: 'DONE' })
    expect(updates?.[1]?.[0]).toMatchObject({ priority: '4' })
    expect(updates?.[2]?.[0]).toMatchObject({ recurrenceType: 'DAILY' })
    expect(updates?.[3]?.[0]).toMatchObject({ dueDateFrom: '2026-04-07' })
    expect(updates?.[4]?.[0]).toMatchObject({ remindDateFrom: '2026-04-09' })
    expect(wrapper.emitted('loadTodos')).toHaveLength(5)

    const presetButton = wrapper.findAll('.filter-preset-btn')[0]
    await presetButton.trigger('click')
    expect(wrapper.emitted('update:filters')?.[5]?.[0]).toMatchObject({ timePreset: 'DUE_TODAY' })

    const resetButton = wrapper.find('button.btn-ghost')
    await resetButton.trigger('click')
    expect(wrapper.emitted('resetFilters')).toHaveLength(1)
  })
})
