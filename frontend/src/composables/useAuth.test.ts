import { beforeEach, describe, expect, it } from 'vitest'
import { useAuth } from './useAuth'

describe('useAuth', () => {
  beforeEach(() => {
    localStorage.clear()
    const { clearToken } = useAuth()
    clearToken()
  })

  it('sets session and stores token and user in localStorage', () => {
    const { token, user, setSession } = useAuth()
    setSession('dummy-token', { id: 7, username: 'alice', email: 'alice@example.com' })

    expect(token.value).toBe('dummy-token')
    expect(user.value).toEqual({ id: 7, username: 'alice', email: 'alice@example.com' })
    expect(localStorage.getItem('personal-toolkit-auth-token')).toBe('dummy-token')
    expect(localStorage.getItem('personal-toolkit-auth-user')).toBe(JSON.stringify({ id: 7, username: 'alice', email: 'alice@example.com' }))
  })

  it('clears token from state and localStorage', () => {
    const { token, user, setSession, clearToken } = useAuth()
    setSession('dummy-token', { id: 7, username: 'alice', email: 'alice@example.com' })
    clearToken()

    expect(token.value).toBeNull()
    expect(user.value).toBeNull()
    expect(localStorage.getItem('personal-toolkit-auth-token')).toBeNull()
    expect(localStorage.getItem('personal-toolkit-auth-user')).toBeNull()
  })
})
