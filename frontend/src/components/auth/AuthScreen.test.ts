import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuthScreen from './AuthScreen.vue'
import { mountWithI18n } from '../../test/test-utils'
import { setTheme } from '../../theme'

async function flushUi() {
  await Promise.resolve()
  await Promise.resolve()
  await new Promise((resolve) => setTimeout(resolve, 0))
  await Promise.resolve()
}

describe('AuthScreen', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input)
      const payload = url.endsWith('/api/auth/login-policy')
        ? {
            success: true,
            message: 'ok',
            data: {
              captchaEnabled: true,
              adaptiveCaptcha: false,
              adaptiveTriggerThreshold: 2,
            },
            timestamp: '2026-04-15T00:00:00Z',
          }
        : {
            success: true,
            message: 'ok',
            data: {
              captchaId: 'captcha-id',
              image: 'data:image/svg+xml;base64,abc',
            },
            timestamp: '2026-04-15T00:00:00Z',
          }

      return new Response(JSON.stringify(payload), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    }))
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
  })

  it('shows username-or-email label in login mode', async () => {
    const wrapper = mountWithI18n(AuthScreen)
    await flushUi()

    expect(wrapper.find('label[for="username"]').text()).toBe('Username or Email')
    expect(wrapper.find('label[for="captcha"]').exists()).toBe(true)
  })

  it('shows captcha after backend requires it in adaptive mode', async () => {
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input)
      if (url.endsWith('/api/auth/login-policy')) {
        return new Response(JSON.stringify({
          success: true,
          message: 'ok',
          data: {
            captchaEnabled: true,
            adaptiveCaptcha: true,
            adaptiveTriggerThreshold: 2,
          },
          timestamp: '2026-04-15T00:00:00Z',
        }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      if (url.endsWith('/api/auth/login')) {
        return new Response(JSON.stringify({
          success: false,
          code: 'CAPTCHA_REQUIRED',
          message: 'Captcha required before login',
          status: 400,
          path: '/api/auth/login',
          timestamp: '2026-04-15T00:00:00Z',
        }), {
          status: 400,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      return new Response(JSON.stringify({
        success: true,
        message: 'ok',
        data: {
          captchaId: 'captcha-id',
          image: 'data:image/svg+xml;base64,abc',
        },
        timestamp: '2026-04-15T00:00:00Z',
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    }))

    const wrapper = mountWithI18n(AuthScreen)
    await flushUi()

    expect(wrapper.find('label[for="captcha"]').exists()).toBe(false)

    await wrapper.find('#username').setValue('alice')
    await wrapper.find('#password').setValue('password123')
    await wrapper.find('form').trigger('submit.prevent')
    await flushUi()

    expect(wrapper.find('label[for="captcha"]').exists()).toBe(true)
  })

  it('shows separate username and email labels in register mode', async () => {
    const wrapper = mountWithI18n(AuthScreen)

    await wrapper.find('.auth-toggle a').trigger('click')

    const labels = wrapper.findAll('label').map((label) => label.text())
    expect(labels).toContain('Username')
    expect(labels).toContain('Email')
  })

  it('keeps the auth screen mounted under a light theme', async () => {
    setTheme('light')

    const wrapper = mountWithI18n(AuthScreen)
    await flushUi()

    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
    expect(wrapper.find('.auth-card').exists()).toBe(true)
  })
})
