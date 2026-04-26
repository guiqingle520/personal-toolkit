<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

import TodoList from './components/TodoList.vue'
import AuthScreen from './components/auth/AuthScreen.vue'
import { useAuth } from './composables/useAuth'
import { fetchApi } from './api'
import { THEME_OPTIONS, type AppTheme, useTheme } from './theme'

const { token, user, clearToken } = useAuth()
const { t } = useI18n()
const { theme, resolvedTheme, setTheme } = useTheme()

const accountMenuOpen = ref(false)
const accountMenuRef = ref<HTMLElement | null>(null)

const themeOptions = computed(() => THEME_OPTIONS.map((value) => ({
  value,
  label: t(`theme.${value}`),
})))

const themeToneClass = computed(() => `is-${resolvedTheme.value}`)
const accountDisplayName = computed(() => user.value?.username || user.value?.email || t('account.buttonLabel'))
const accountSecondaryText = computed(() => user.value?.email || t('account.signedInAs', { value: accountDisplayName.value }))
const accountInitial = computed(() => accountDisplayName.value.trim().charAt(0).toUpperCase())

function handleThemeChange(nextTheme: AppTheme) {
  setTheme(nextTheme)
}

function toggleAccountMenu() {
  accountMenuOpen.value = !accountMenuOpen.value
}

function closeAccountMenu() {
  accountMenuOpen.value = false
}

function handleDocumentClick(event: MouseEvent) {
  const target = event.target
  if (!(target instanceof Node)) {
    return
  }

  if (accountMenuRef.value?.contains(target)) {
    return
  }

  closeAccountMenu()
}

function handleDocumentKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeAccountMenu()
  }
}

async function handleLogout() {
  closeAccountMenu()
  try {
    await fetchApi('/api/auth/logout', { method: 'POST' })
  } catch (error) {
    console.warn('Failed to notify logout endpoint before clearing local session.', error)
  } finally {
    clearToken()
  }
}

watch(token, (nextToken) => {
  if (!nextToken) {
    closeAccountMenu()
  }
})

onMounted(() => {
  if (typeof document === 'undefined') {
    return
  }

  document.addEventListener('mousedown', handleDocumentClick)
  document.addEventListener('keydown', handleDocumentKeydown)
})

onBeforeUnmount(() => {
  if (typeof document === 'undefined') {
    return
  }

  document.removeEventListener('mousedown', handleDocumentClick)
  document.removeEventListener('keydown', handleDocumentKeydown)
})
</script>

<template>
  <main>
    <div class="app-shell">
      <AuthScreen v-if="!token" />

      <div v-else class="app-authenticated-shell">
        <div class="app-account-bar">
          <div ref="accountMenuRef" class="app-account-menu">
            <button
              type="button"
              class="btn btn-outline app-account-trigger"
              :class="themeToneClass"
              :aria-expanded="accountMenuOpen"
              :aria-label="$t('account.menuLabel')"
              @click="toggleAccountMenu"
            >
              <span class="app-account-avatar">{{ accountInitial }}</span>
              <span class="app-account-trigger-copy">
                <span class="app-account-trigger-label">{{ accountDisplayName }}</span>
                <span class="app-account-trigger-meta">{{ $t('account.buttonLabel') }}</span>
              </span>
              <span class="app-account-trigger-caret" :class="{ 'is-open': accountMenuOpen }">⌄</span>
            </button>

            <Transition name="account-menu-fade">
              <div v-if="accountMenuOpen" class="app-account-dropdown" :class="themeToneClass">
                <section class="app-account-section">
                  <div class="app-account-section-title">{{ $t('account.management') }}</div>
                  <div class="app-account-card">
                    <span class="app-account-card-label">{{ $t('account.signedInAs', { value: accountDisplayName }) }}</span>
                    <strong class="app-account-card-name">{{ accountDisplayName }}</strong>
                    <span class="app-account-card-meta">{{ accountSecondaryText }}</span>
                  </div>
                  <button type="button" class="btn btn-danger-outline app-account-logout" @click="handleLogout">
                    {{ $t('auth.logout') }}
                  </button>
                </section>

                <section class="app-account-section">
                  <div class="app-account-section-title">{{ $t('account.themeSettings') }}</div>
                  <div class="app-theme-meta">
                    <span class="app-theme-label">
                      <span class="app-theme-orb" :class="themeToneClass"></span>
                      {{ $t('theme.label') }}
                    </span>
                    <span class="app-theme-state">{{ $t('theme.current', { theme: $t(`theme.${resolvedTheme}`) }) }}</span>
                  </div>

                  <div class="app-theme-controls" role="group" :aria-label="$t('theme.label')">
                    <button
                      v-for="option in themeOptions"
                      :key="option.value"
                      type="button"
                      class="btn btn-outline app-theme-option"
                      :class="{ 'is-active': theme === option.value }"
                      :aria-pressed="theme === option.value"
                      @click="handleThemeChange(option.value)"
                    >
                      {{ option.label }}
                    </button>
                  </div>
                </section>
              </div>
            </Transition>
          </div>
        </div>

        <div class="app-workbench-host">
          <TodoList />
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
main {
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  padding: 32px 16px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 100%;
}

.app-authenticated-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 100%;
}

.app-workbench-host {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.app-account-bar {
  display: flex;
  justify-content: flex-end;
}

.app-account-menu {
  position: relative;
}

.app-account-trigger {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 220px;
  padding: 10px 12px;
  border-radius: 22px;
  background: var(--color-surface-elevated);
  border: 1px solid var(--color-border-subtle);
  box-shadow: var(--theme-bar-shadow);
  backdrop-filter: blur(14px);
  overflow: hidden;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast), background-color var(--transition-fast);
}

.app-account-trigger:hover {
  border-color: color-mix(in srgb, var(--primary-color) 24%, var(--color-border-subtle));
  box-shadow: 0 20px 44px color-mix(in srgb, var(--primary-color) 14%, transparent);
  transform: translateY(-1px);
}

.app-account-trigger[aria-expanded='true'] {
  border-color: color-mix(in srgb, var(--primary-color) 36%, var(--color-border-subtle));
  box-shadow: 0 22px 52px color-mix(in srgb, var(--primary-color) 18%, transparent);
}

.app-account-trigger::before,
.app-account-dropdown::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--theme-bar-glow);
  opacity: 0.9;
  pointer-events: none;
}

.app-account-trigger > *,
.app-account-dropdown > * {
  position: relative;
  z-index: 1;
}

.app-account-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--color-primary-gradient);
  color: var(--color-text-inverse);
  font-size: 0.98rem;
  font-weight: 700;
  box-shadow: var(--shadow-primary);
  position: relative;
}

.app-account-avatar::after {
  content: '';
  position: absolute;
  inset: 2px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.26);
  opacity: 0.8;
}

.app-account-trigger-copy {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.app-account-trigger-label {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-strong);
  font-size: 0.92rem;
  font-weight: 700;
}

.app-account-trigger-meta {
  color: var(--color-text-muted);
  font-size: 0.75rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.app-account-trigger-caret {
  color: var(--color-text-muted);
  font-size: 1rem;
  transition: transform var(--transition-fast), color var(--transition-fast);
}

.app-account-trigger-caret.is-open {
  transform: rotate(180deg);
  color: var(--color-text-strong);
}

.app-account-dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 10px);
  width: min(360px, calc(100vw - 24px));
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 14px;
  border-radius: 22px;
  background: var(--color-surface-elevated);
  border: 1px solid var(--color-border-subtle);
  box-shadow: var(--theme-bar-shadow);
  backdrop-filter: blur(14px);
  z-index: 30;
  overflow: hidden;
  transform-origin: top right;
}

.app-account-dropdown::after {
  content: '';
  position: absolute;
  top: -8px;
  right: 22px;
  width: 16px;
  height: 16px;
  background: color-mix(in srgb, var(--color-surface-elevated) 96%, transparent);
  border-left: 1px solid var(--color-border-subtle);
  border-top: 1px solid var(--color-border-subtle);
  transform: rotate(45deg);
}

.app-account-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border-radius: var(--radius-xl);
  background: color-mix(in srgb, var(--color-surface-base) 92%, transparent);
  border: 1px solid var(--color-border-subtle);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.app-account-section-title {
  color: var(--color-text-muted);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.app-account-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px;
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--color-surface-hover) 88%, transparent);
  border: 1px solid var(--color-border-subtle);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.app-account-card-label,
.app-account-card-meta {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.app-account-card-name {
  color: var(--color-text-strong);
  font-size: 1rem;
  line-height: 1.3;
}

.app-account-logout {
  align-self: flex-start;
  min-width: 124px;
}

.app-theme-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.app-theme-label {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.app-theme-orb {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: var(--primary-color);
  box-shadow: 0 0 0 5px color-mix(in srgb, var(--primary-color) 18%, transparent);
}

.app-theme-orb.is-dark {
  background: #38bdf8;
}

.app-theme-orb.is-light {
  background: #f59e0b;
}

.app-theme-state {
  color: var(--color-text-bright);
  font-size: 0.92rem;
  font-weight: 600;
}

.app-theme-controls {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-surface-base) 88%, transparent);
  border: 1px solid var(--color-border-subtle);
}

.app-theme-option {
  min-width: 92px;
  min-height: 38px;
  border-radius: 999px;
  border-color: transparent;
  color: var(--color-text-normal);
  position: relative;
  overflow: hidden;
}

.app-theme-option.is-active {
  background: var(--color-primary-gradient);
  color: var(--color-text-inverse);
  box-shadow: var(--shadow-primary);
}

.app-theme-option.is-active::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: inherit;
  border: 1px solid rgba(255, 255, 255, 0.22);
}

.app-theme-option:not(.is-active):not(:disabled):hover {
  border-color: var(--color-border);
  background: color-mix(in srgb, var(--color-surface-hover) 94%, transparent);
}

.account-menu-fade-enter-active,
.account-menu-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.account-menu-fade-enter-from,
.account-menu-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.98);
}

@media (max-width: 640px) {
  main {
    padding: 20px 12px;
  }

  .app-account-bar {
    justify-content: stretch;
  }

  .app-account-trigger,
  .app-account-dropdown {
    width: 100%;
  }

  .app-account-dropdown {
    right: auto;
    left: 0;
    top: calc(100% + 8px);
  }

  .app-account-dropdown::after {
    left: 22px;
    right: auto;
  }

  .app-theme-controls {
    width: 100%;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .app-theme-option {
    min-width: 0;
    padding-inline: 10px;
  }

  .app-account-section {
    padding: 12px;
  }
}
</style>
