import { describe, expect, it } from 'vitest'
import AuthScreen from './AuthScreen.vue'
import { mountWithI18n } from '../../test/test-utils'

describe('AuthScreen', () => {
  it('shows username-or-email label in login mode', () => {
    const wrapper = mountWithI18n(AuthScreen)

    expect(wrapper.find('label[for="username"]').text()).toBe('Username or Email')
  })

  it('shows separate username and email labels in register mode', async () => {
    const wrapper = mountWithI18n(AuthScreen)

    await wrapper.find('.auth-toggle a').trigger('click')

    const labels = wrapper.findAll('label').map((label) => label.text())
    expect(labels).toContain('Username')
    expect(labels).toContain('Email')
  })
})
