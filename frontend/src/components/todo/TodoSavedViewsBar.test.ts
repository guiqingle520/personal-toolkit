import { describe, expect, it } from 'vitest'

import TodoSavedViewsBar from './TodoSavedViewsBar.vue'
import { mountWithI18n } from '../../test/test-utils'

describe('TodoSavedViewsBar', () => {
  it('renders and emits saved view actions', async () => {
    const wrapper = mountWithI18n(TodoSavedViewsBar, {
      props: {
        savedViews: [
          {
            id: 1,
            name: 'Ops Focus',
            isDefault: true,
            filters: { status: 'PENDING' },
            createTime: '2026-04-22T00:00:00',
            updateTime: '2026-04-22T00:00:00',
          },
        ],
      },
    })

    expect(wrapper.text()).toContain('Ops Focus')

    const buttons = wrapper.findAll('button')
    await buttons[0].trigger('click')
    await buttons[1].trigger('click')
    await buttons[2].trigger('click')
    await buttons[3].trigger('click')

    expect(wrapper.emitted('apply')?.[0]?.[0]?.id).toBe(1)
    expect(wrapper.emitted('set-default')?.[0]).toEqual([1])
    expect(wrapper.emitted('rename')?.[0]?.[0]?.name).toBe('Ops Focus')
    expect(wrapper.emitted('delete')?.[0]).toEqual([1])
  })
})
