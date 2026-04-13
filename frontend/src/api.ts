import { useAuth } from './composables/useAuth'

export interface ApiResponse<T = void> {
  success: boolean
  message: string
  data?: T
  timestamp: string
}

export interface ApiError {
  message: string
  validation?: Record<string, string[]>
}

export async function fetchApi<T>(url: string, options?: RequestInit, t?: (key: string, args?: any) => string): Promise<ApiResponse<T>> {
  const { token, clearToken } = useAuth()
  
  const headers: Record<string, string> = {
    ...((options?.headers as Record<string, string>) || {})
  }
  
  if (!headers['Content-Type'] && !(options?.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }
  
  if (token.value) {
    headers['Authorization'] = `Bearer ${token.value}`
  }

  const res = await fetch(url, {
    ...options,
    headers,
  })
  
  // if unauthorized, clear token
  if (res.status === 401) {
    clearToken()
    // It will reactively unmount TodoList and mount AuthScreen
  }

  let data
  try {
    data = await res.json()
  } catch {
    data = null
  }
  
  if (!res.ok) {
    const errorMsg = data?.message || (t ? t('feedback.httpError', { status: res.status }) : `HTTP Error ${res.status}`)
    throw new Error(JSON.stringify({
      message: errorMsg,
      validation: data?.validation
    }))
  }
  
  return data as ApiResponse<T>
}
