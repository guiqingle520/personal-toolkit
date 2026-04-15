import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuthScreen from './AuthScreen.vue'
import { mountWithI18n } from '../../test/test-utils'

describe('AuthScreen', () => {
  beforeEach(() => {
    const payload = JSON.stringify({
      success: true,
      message: 'ok',
      data: {
        captchaId: 'captcha-id',
        image: 'data:image/svg+xml;base64,abc',
      },
      timestamp: '2026-04-15T00:00:00Z',
    })

    vi.stubGlobal('fetch', vi.fn(async () => new Response(payload, {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })))
    localStorage.clear()
  })

  it('shows username-or-email label in login mode', () => {
    const wrapper = mountWithI18n(AuthScreen)

    expect(wrapper.find('label[for="username"]').text()).toBe('Username or Email')
    expect(wrapper.find('label[for="captcha"]').exists()).toBe(true)
  })

  it('shows separate username and email labels in register mode', async () => {
    const wrapper = mountWithI18n(AuthScreen)

    await wrapper.find('.auth-toggle a').trigger('click')

    const labels = wrapper.findAll('label').map((label) => label.text())
    expect(labels).toContain('Username')
    expect(labels).toContain('Email')
  })
})
