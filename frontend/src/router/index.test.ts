import { describe, expect, it, vi } from 'vitest'

const mockUser = vi.hoisted(() => ({
  value: null as null | { passwordChangeRequired?: boolean },
}))

vi.mock('../composables/useAuth', () => ({
  useAuth: () => ({
    user: mockUser,
  }),
}))

describe('router password change guard', () => {
  it('redirects forced-change users to the change-password route', async () => {
    mockUser.value = { passwordChangeRequired: true }

    const { createAppRouter } = await import('./index')
    const { createMemoryHistory } = await import('vue-router')
    const router = createAppRouter(createMemoryHistory())

    await router.push('/tasks')
    await router.isReady()

    expect(router.currentRoute.value.fullPath).toBe('/change-password')
  })

  it('redirects non-forced users away from the change-password route to tasks', async () => {
    mockUser.value = { passwordChangeRequired: false }

    const { createAppRouter } = await import('./index')
    const { createMemoryHistory } = await import('vue-router')
    const router = createAppRouter(createMemoryHistory())

    await router.push('/change-password')
    await router.isReady()

    expect(router.currentRoute.value.fullPath).toBe('/tasks')
  })
})
