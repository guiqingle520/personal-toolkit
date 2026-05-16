import { useAuth } from './composables/useAuth'

export interface ApiResponse<T = void> {
  success: boolean
  message: string
  data?: T
  timestamp: string
}

export interface ApiError {
  code?: string
  message: string
  status?: number
  validation?: Record<string, string[]>
}

type TranslationArgs = Record<string, unknown>

export class ApiRequestError extends Error {
  readonly apiError: ApiError

  constructor(apiError: ApiError) {
    super(apiError.message)
    this.name = 'ApiRequestError'
    this.apiError = apiError
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isValidationMap(value: unknown): value is Record<string, string[]> {
  if (!isRecord(value)) {
    return false
  }

  return Object.values(value).every((entry) => Array.isArray(entry) && entry.every((item) => typeof item === 'string'))
}

function normalizeApiError(value: unknown): ApiError | null {
  if (!isRecord(value) || typeof value.message !== 'string') {
    return null
  }

  return {
    code: typeof value.code === 'string' ? value.code : undefined,
    message: value.message,
    status: typeof value.status === 'number' ? value.status : undefined,
    validation: isValidationMap(value.validation) ? value.validation : undefined,
  }
}

function parseApiErrorMessage(message: string): ApiError | null {
  try {
    return normalizeApiError(JSON.parse(message))
  } catch (parseError) {
    void parseError
    return null
  }
}

export function toApiError(error: unknown, fallbackMessage: string): ApiError {
  if (error instanceof ApiRequestError) {
    return error.apiError
  }

  if (error instanceof Error) {
    const parsedFromMessage = parseApiErrorMessage(error.message)
    if (parsedFromMessage) {
      return parsedFromMessage
    }

    return {
      message: error.message || fallbackMessage,
    }
  }

  const normalized = normalizeApiError(error)
  if (normalized) {
    const parsedFromMessage = parseApiErrorMessage(normalized.message)
    if (parsedFromMessage) {
      return {
        ...parsedFromMessage,
        code: parsedFromMessage.code ?? normalized.code,
        validation: parsedFromMessage.validation ?? normalized.validation,
      }
    }

    return normalized
  }

  return {
    message: fallbackMessage,
  }
}

export async function fetchApi<T>(url: string, options?: RequestInit, t?: (key: string, args?: TranslationArgs) => string): Promise<ApiResponse<T>> {
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
  } catch (parseError) {
    void parseError
    data = null
  }
  
  if (!res.ok) {
    const errorMsg = data?.message || (t ? t('feedback.httpError', { status: res.status }) : `HTTP Error ${res.status}`)
    throw new ApiRequestError({
      code: data?.code,
      message: errorMsg,
      status: data?.status ?? res.status,
      validation: data?.validation
    })
  }
  
  return data as ApiResponse<T>
}
