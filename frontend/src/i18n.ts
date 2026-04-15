import { createI18n } from 'vue-i18n'
import en, { type MessageSchema } from './locales/en'
import zhCN from './locales/zh-CN'

export const SUPPORTED_LOCALES = ['en', 'zh-CN'] as const
export type AppLocale = typeof SUPPORTED_LOCALES[number]

const LOCALE_STORAGE_KEY = 'personal-toolkit-locale'

function isSupportedLocale(locale: string): locale is AppLocale {
  return (SUPPORTED_LOCALES as readonly string[]).includes(locale)
}

function resolveInitialLocale(): AppLocale {
  const savedLocale = globalThis.localStorage?.getItem(LOCALE_STORAGE_KEY)
  if (savedLocale && isSupportedLocale(savedLocale)) {
    return savedLocale
  }

  const browserLocale = globalThis.navigator?.language
  if (browserLocale && isSupportedLocale(browserLocale)) {
    return browserLocale
  }

  if (browserLocale?.toLowerCase().startsWith('zh')) {
    return 'zh-CN'
  }

  return 'en'
}

const i18n = createI18n<[MessageSchema], AppLocale>({
  legacy: false,
  locale: resolveInitialLocale(),
  fallbackLocale: 'en',
  messages: {
    en,
    'zh-CN': zhCN
  }
})

export function persistLocale(locale: AppLocale): void {
  globalThis.localStorage?.setItem(LOCALE_STORAGE_KEY, locale)
}

export function syncDocumentLocale(locale: AppLocale): void {
  if (typeof document === 'undefined') {
    return
  }

  document.documentElement.lang = locale
}

export default i18n
