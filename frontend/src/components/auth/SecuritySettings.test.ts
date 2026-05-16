import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mountWithI18nAndRouter } from '../../test/test-utils'
import SecuritySettings from './SecuritySettings.vue'
import { fetchApi } from '../../api'

vi.mock('../../api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api')>()
  return {
    ...actual,
    fetchApi: vi.fn(),
  }
})

describe('SecuritySettings.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads policy on mount', async () => {
    vi.mocked(fetchApi).mockResolvedValueOnce({
      success: true,
      message: 'OK',
      data: {
        accessTokenTtlSeconds: 3600,
        effectiveAccessTokenTtlSeconds: 3600,
        passwordExpiryEnabled: true,
        passwordExpiryDays: 30
      },
      timestamp: new Date().toISOString()
    })

    const { wrapper } = await mountWithI18nAndRouter(SecuritySettings)
    await Promise.resolve()
    await Promise.resolve()
    await Promise.resolve()

    expect(fetchApi).toHaveBeenCalledWith('/api/auth/security-policy', { method: 'GET' })

    const inputTtl = wrapper.find('input#accessTokenTtl').element as HTMLInputElement
    expect(inputTtl.value).toBe('3600')

    const inputExpiry = wrapper.find('input#passwordExpiryDays').element as HTMLInputElement
    expect(inputExpiry.value).toBe('30')
  })

  it('handles 403 unauthorized error', async () => {
    vi.mocked(fetchApi).mockRejectedValueOnce(new Error(JSON.stringify({ status: 403, message: 'Forbidden' })))

    const { wrapper } = await mountWithI18nAndRouter(SecuritySettings)
    await Promise.resolve()
    await Promise.resolve()

    expect(wrapper.text()).toContain('You do not have permission to access security settings.')
    expect(wrapper.find('input#accessTokenTtl').exists()).toBe(false)
  })

  it('saves policy and shows success', async () => {
    vi.mocked(fetchApi).mockResolvedValueOnce({
      success: true,
      message: 'OK',
      data: {
        accessTokenTtlSeconds: 3600,
        effectiveAccessTokenTtlSeconds: 3600,
        passwordExpiryEnabled: false,
        passwordExpiryDays: 90
      },
      timestamp: new Date().toISOString()
    })

    vi.mocked(fetchApi).mockResolvedValueOnce({
      success: true,
      message: 'OK',
      data: {
        accessTokenTtlSeconds: 7200,
        effectiveAccessTokenTtlSeconds: 7200,
        passwordExpiryEnabled: true,
        passwordExpiryDays: 14
      },
      timestamp: new Date().toISOString()
    })

    const { wrapper } = await mountWithI18nAndRouter(SecuritySettings)
    await Promise.resolve()
    await Promise.resolve()
    await Promise.resolve()

    await wrapper.find('#accessTokenTtl').setValue('7200')
    await wrapper.find('#passwordExpiryEnabled').setValue(true)
    await wrapper.find('#passwordExpiryDays').setValue('14')
    await wrapper.find('form').trigger('submit.prevent')

    await Promise.resolve()
    await Promise.resolve()
    await Promise.resolve()

    expect(fetchApi).toHaveBeenCalledWith('/api/auth/security-policy', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify({
        accessTokenTtlSeconds: 7200,
        passwordExpiryEnabled: true,
        passwordExpiryDays: 14
      })
    }))

    expect(wrapper.text()).toContain('Security policy updated successfully.')
  })
})
