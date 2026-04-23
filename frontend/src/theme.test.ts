import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import {
  cleanupThemeListeners,
  initializeTheme,
  setTheme,
  THEME_STORAGE_KEY,
  useTheme,
} from './theme'

type MatchMediaListener = (event: MediaQueryListEvent) => void

function stubMatchMedia(matches: boolean) {
  let currentMatches = matches
  const listeners = new Set<MatchMediaListener>()

  vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
    matches: currentMatches,
    media: '(prefers-color-scheme: dark)',
    onchange: null,
    addEventListener: (_: string, listener: MatchMediaListener) => listeners.add(listener),
    removeEventListener: (_: string, listener: MatchMediaListener) => listeners.delete(listener),
    addListener: (listener: MatchMediaListener) => listeners.add(listener),
    removeListener: (listener: MatchMediaListener) => listeners.delete(listener),
    dispatchEvent: () => true,
  })))

  return {
    setMatches(nextMatches: boolean) {
      currentMatches = nextMatches
      listeners.forEach((listener) => listener({ matches: nextMatches } as MediaQueryListEvent))
    },
  }
}

describe('theme', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
  })

  afterEach(() => {
    cleanupThemeListeners()
    vi.unstubAllGlobals()
  })

  it('applies explicit light theme and persists it', () => {
    stubMatchMedia(true)

    initializeTheme()
    setTheme('light')

    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light')
    expect(useTheme().resolvedTheme.value).toBe('light')
  })

  it('follows system theme when system mode is selected', () => {
    const matchMediaControl = stubMatchMedia(true)

    initializeTheme()
    setTheme('system')

    expect(document.documentElement.hasAttribute('data-theme')).toBe(false)
    expect(useTheme().resolvedTheme.value).toBe('dark')

    matchMediaControl.setMatches(false)

    expect(useTheme().resolvedTheme.value).toBe('light')
    expect(document.documentElement.hasAttribute('data-theme')).toBe(false)
  })
})
