import { describe, expect, it, vi, beforeEach } from 'vitest'
import { ref } from 'vue'
import { mountWithI18nAndRouter } from '../../test/test-utils'
import ChangePassword from './ChangePassword.vue'
import { fetchApi } from '../../api'
import { useAuth } from '../../composables/useAuth'

vi.mock('../../api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api')>()
  return {
    ...actual,
    fetchApi: vi.fn(),
  }
})

const setSessionMock = vi.fn()

vi.mock('../../composables/useAuth', () => ({
  useAuth: () => ({
    token: ref('token'),
    user: ref({ id: 1, username: 'alice', passwordChangeRequired: true }),
    setSession: setSessionMock
  })
}))

describe('ChangePassword.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders form fields', async () => {
    const { wrapper } = await mountWithI18nAndRouter(ChangePassword)
    expect(wrapper.find('input[type="password"]#currentPassword').exists()).toBe(true)
    expect(wrapper.find('input[type="password"]#newPassword').exists()).toBe(true)
    expect(wrapper.find('input[type="password"]#confirmPassword').exists()).toBe(true)
  })

  it('shows error if new passwords do not match', async () => {
    const { wrapper } = await mountWithI18nAndRouter(ChangePassword)
    
    await wrapper.find('#currentPassword').setValue('oldpass')
    await wrapper.find('#newPassword').setValue('newpass')
    await wrapper.find('#confirmPassword').setValue('mismatch')
    await wrapper.find('form').trigger('submit.prevent')
    
    expect(wrapper.text()).toContain('New passwords do not match.')
    expect(fetchApi).not.toHaveBeenCalled()
  })

  it('submits successfully and updates session', async () => {
    vi.mocked(fetchApi).mockResolvedValueOnce({
      success: true,
      message: 'OK',
      data: {
        id: 1,
        username: 'alice',
        passwordChangeRequired: false
      },
      timestamp: new Date().toISOString()
    })

    const { wrapper } = await mountWithI18nAndRouter(ChangePassword)
    
    await wrapper.find('#currentPassword').setValue('oldpass')
    await wrapper.find('#newPassword').setValue('newpass')
    await wrapper.find('#confirmPassword').setValue('newpass')
    await wrapper.find('form').trigger('submit.prevent')
    
    await Promise.resolve()
    await Promise.resolve()

    expect(fetchApi).toHaveBeenCalledWith('/api/auth/change-password', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({
        currentPassword: 'oldpass',
        newPassword: 'newpass',
        confirmPassword: 'newpass'
      })
    }))

    expect(setSessionMock).toHaveBeenCalledWith('token', {
      id: 1,
      username: 'alice',
      passwordChangeRequired: false
    })
    
    expect(wrapper.text()).toContain('Password changed successfully')
  })

  it('shows fallback message for unexpected errors', async () => {
    vi.mocked(fetchApi).mockRejectedValueOnce(new Error('Network down'))

    const { wrapper } = await mountWithI18nAndRouter(ChangePassword)

    await wrapper.find('#currentPassword').setValue('oldpass')
    await wrapper.find('#newPassword').setValue('newpass')
    await wrapper.find('#confirmPassword').setValue('newpass')
    await wrapper.find('form').trigger('submit.prevent')

    await Promise.resolve()
    await Promise.resolve()

    expect(wrapper.text()).toContain('Network down')
  })
})
