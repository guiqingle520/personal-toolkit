import { createI18n, type I18n } from 'vue-i18n'
import { mount, type MountingOptions, type VueWrapper } from '@vue/test-utils'
import type { ComponentPublicInstance, DefineComponent } from 'vue'
import en from '../locales/en'
import zhCN from '../locales/zh-CN'

function createTestI18n(): I18n {
  return createI18n({
    legacy: false,
    locale: 'en',
    fallbackLocale: 'en',
    messages: {
      en,
      'zh-CN': zhCN,
    },
  })
}

export function mountWithI18n(
  component: DefineComponent<any, any, any, any, any, any, any, any>,
  options: MountingOptions<ComponentPublicInstance> = {},
): VueWrapper<ComponentPublicInstance> {
  const i18n = createTestI18n()

  return mount(component, {
    ...options,
    global: {
      ...(options.global ?? {}),
      plugins: [...(options.global?.plugins ?? []), i18n],
    },
  })
}
