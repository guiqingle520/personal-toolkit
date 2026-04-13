import { ref } from 'vue'

export interface User {
  id: number
  username: string
  email?: string
}

const TOKEN_STORAGE_KEY = 'personal-toolkit-auth-token'
const USER_STORAGE_KEY = 'personal-toolkit-auth-user'

function readStoredUser(): User | null {
  const rawUser = globalThis.localStorage?.getItem(USER_STORAGE_KEY)
  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser) as User
  } catch {
    globalThis.localStorage?.removeItem(USER_STORAGE_KEY)
    return null
  }
}

const token = ref<string | null>(globalThis.localStorage?.getItem(TOKEN_STORAGE_KEY) ?? null)
const user = ref<User | null>(readStoredUser())

export function useAuth() {
  const setSession = (nextToken: string, nextUser: User | null) => {
    token.value = nextToken
    user.value = nextUser
    globalThis.localStorage?.setItem(TOKEN_STORAGE_KEY, nextToken)
    if (nextUser) {
      globalThis.localStorage?.setItem(USER_STORAGE_KEY, JSON.stringify(nextUser))
      return
    }
    globalThis.localStorage?.removeItem(USER_STORAGE_KEY)
  }

  const clearToken = () => {
    token.value = null
    user.value = null
    globalThis.localStorage?.removeItem(TOKEN_STORAGE_KEY)
    globalThis.localStorage?.removeItem(USER_STORAGE_KEY)
  }

  return {
    token,
    user,
    setSession,
    clearToken,
  }
}
