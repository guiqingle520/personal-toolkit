import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import App from './App.vue'
import { mountWithI18nAndRouter } from './test/test-utils'

const clearTokenMock = vi.fn()

vi.mock('./composables/useAuth', () => ({
  useAuth: () => ({
    token: ref('token'),
    user: ref({ id: 1, username: 'alice', email: 'alice@example.com' }),
    setSession: vi.fn(),
    clearToken: clearTokenMock,
  }),
}))

describe('App account menu', () => {
  beforeEach(() => {
    clearTokenMock.mockReset()
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
      matches: false,
      media: '(prefers-color-scheme: dark)',
      onchange: null,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })))
    vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({ success: true, message: 'ok', data: null }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })))
  })

  it('renders authenticated todo workbench inside a dedicated host', async () => {
    const { wrapper } = await mountWithI18nAndRouter(App, { route: '/tasks' })

    expect(wrapper.find('.app-authenticated-shell').exists()).toBe(true)
    expect(wrapper.find('.app-account-bar').exists()).toBe(true)
    expect(wrapper.find('.app-workbench-host').exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'TodoList' }).exists()).toBe(true)
  })

  it('renders TodoList inside the workbench host for /tasks', async () => {
    const { wrapper } = await mountWithI18nAndRouter(App, { route: '/tasks' })

    const workbenchHost = wrapper.find('.app-workbench-host')
    expect(workbenchHost.exists()).toBe(true)
    expect(workbenchHost.findComponent({ name: 'TodoList' }).exists()).toBe(true)
  })

  it('renders statistics route inside the workbench host', async () => {
    const { wrapper } = await mountWithI18nAndRouter(App, { route: '/statistics' })

    const workbenchHost = wrapper.find('.app-workbench-host')
    expect(workbenchHost.exists()).toBe(true)
    expect(workbenchHost.findComponent({ name: 'TodoStatisticsView' }).exists()).toBe(true)
  })

  it('opens account menu and switches theme from theme settings', async () => {
    const { wrapper } = await mountWithI18nAndRouter(App, { route: '/tasks' })

    await wrapper.find('.app-account-trigger').trigger('click')

    expect(wrapper.text()).toContain('Account Management')
    expect(wrapper.text()).toContain('Theme Settings')

    const themeButtons = wrapper.findAll('.app-theme-option')
    await themeButtons[2].trigger('click')

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
    expect(localStorage.getItem('personal-toolkit-theme')).toBe('dark')
    expect(wrapper.text()).toContain('Current: Dark')
  })

  it('logs out from the account menu', async () => {
    const { wrapper } = await mountWithI18nAndRouter(App, { route: '/tasks' })

    await wrapper.find('.app-account-trigger').trigger('click')
    await wrapper.find('.app-account-logout').trigger('click')
    await Promise.resolve()
    await Promise.resolve()

    expect(clearTokenMock).toHaveBeenCalledTimes(1)
  })
})
