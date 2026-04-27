import { createI18n, type I18n } from 'vue-i18n'
import { mount, type MountingOptions, type VueWrapper } from '@vue/test-utils'
import type { ComponentPublicInstance, DefineComponent } from 'vue'
import { createMemoryHistory, type Router } from 'vue-router'
import en from '../locales/en'
import zhCN from '../locales/zh-CN'
import { createAppRouter } from '../router'

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

export async function mountWithI18nAndRouter(
  component: DefineComponent<any, any, any, any, any, any, any, any>,
  {
    route = '/tasks',
    options = {},
  }: {
    route?: string
    options?: MountingOptions<ComponentPublicInstance>
  } = {},
): Promise<{ wrapper: VueWrapper<ComponentPublicInstance>; router: Router }> {
  const i18n = createTestI18n()
  const router = createAppRouter(createMemoryHistory())

  await router.push(route)
  await router.isReady()

  const wrapper = mount(component, {
    ...options,
    global: {
      ...(options.global ?? {}),
      plugins: [...(options.global?.plugins ?? []), i18n, router],
    },
  })

  return { wrapper, router }
}
