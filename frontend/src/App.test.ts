import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import App from './App.vue'
import { mountWithI18n } from './test/test-utils'

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

  it('opens account menu and switches theme from theme settings', async () => {
    const wrapper = mountWithI18n(App)

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
    const wrapper = mountWithI18n(App)

    await wrapper.find('.app-account-trigger').trigger('click')
    await wrapper.find('.app-account-logout').trigger('click')
    await Promise.resolve()
    await Promise.resolve()

    expect(clearTokenMock).toHaveBeenCalledTimes(1)
  })
})
