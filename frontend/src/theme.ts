import { computed, ref } from 'vue'

export const THEME_STORAGE_KEY = 'personal-toolkit-theme'
export const THEME_OPTIONS = ['system', 'light', 'dark'] as const

export type AppTheme = typeof THEME_OPTIONS[number]
export type ResolvedTheme = 'light' | 'dark'

const selectedTheme = ref<AppTheme>(resolveInitialTheme())
const resolvedTheme = ref<ResolvedTheme>('dark')

let mediaQueryList: MediaQueryList | null = null
let removeMediaQueryListener: (() => void) | null = null

function isSupportedTheme(value: string): value is AppTheme {
  return (THEME_OPTIONS as readonly string[]).includes(value)
}

function resolveInitialTheme(): AppTheme {
  const savedTheme = globalThis.localStorage?.getItem(THEME_STORAGE_KEY)
  if (savedTheme && isSupportedTheme(savedTheme)) {
    return savedTheme
  }

  return 'system'
}

function getSystemTheme(): ResolvedTheme {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return 'dark'
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function nextResolvedTheme(theme: AppTheme): ResolvedTheme {
  return theme === 'system' ? getSystemTheme() : theme
}

function applyThemeToDocument(theme: AppTheme) {
  if (typeof document === 'undefined') {
    return
  }

  if (theme === 'system') {
    document.documentElement.removeAttribute('data-theme')
  } else {
    document.documentElement.setAttribute('data-theme', theme)
  }
}

function syncResolvedTheme(theme: AppTheme) {
  resolvedTheme.value = nextResolvedTheme(theme)
}

function handleSystemThemeChange() {
  if (selectedTheme.value !== 'system') {
    return
  }

  syncResolvedTheme('system')
}

function bindSystemThemeListener() {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function' || mediaQueryList) {
    return
  }

  mediaQueryList = window.matchMedia('(prefers-color-scheme: dark)')
  const listener = () => handleSystemThemeChange()

  if (typeof mediaQueryList.addEventListener === 'function') {
    mediaQueryList.addEventListener('change', listener)
    removeMediaQueryListener = () => mediaQueryList?.removeEventListener('change', listener)
  } else if (typeof mediaQueryList.addListener === 'function') {
    mediaQueryList.addListener(listener)
    removeMediaQueryListener = () => mediaQueryList?.removeListener(listener)
  }
}

function persistTheme(theme: AppTheme) {
  globalThis.localStorage?.setItem(THEME_STORAGE_KEY, theme)
}

export function setTheme(theme: AppTheme) {
  selectedTheme.value = theme
  persistTheme(theme)
  applyThemeToDocument(theme)
  syncResolvedTheme(theme)
}

export function initializeTheme() {
  bindSystemThemeListener()
  applyThemeToDocument(selectedTheme.value)
  syncResolvedTheme(selectedTheme.value)
}

export function cleanupThemeListeners() {
  removeMediaQueryListener?.()
  removeMediaQueryListener = null
  mediaQueryList = null
}

export function useTheme() {
  return {
    theme: selectedTheme,
    resolvedTheme: computed(() => resolvedTheme.value),
    setTheme,
  }
}
