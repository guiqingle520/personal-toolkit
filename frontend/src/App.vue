<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterView } from 'vue-router'

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
              <span class="app-account-trigger-caret" :class="{ 'is-open': accountMenuOpen }">▼</span>
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
                  <RouterLink to="/security-settings" class="btn btn-outline" @click="closeAccountMenu">
                    {{ $t('account.securitySettings') }}
                  </RouterLink>
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
          <RouterView />
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
main {
  width: 100%;
  margin: 0 auto;
  padding: 0 24px 36px;
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
  flex: 1;
}

.app-authenticated-shell {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 100%;
  flex: 1;
}

.app-workbench-host {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  flex: 1;
  padding-top: 28px;
}

.app-account-bar {
  display: flex;
  justify-content: flex-end;
}

@media (min-width: 1025px) {
  .app-account-bar {
    position: sticky;
    top: 0;
    z-index: 30;
    background: rgba(var(--color-app-bg-rgb, 252, 252, 252), 0.78);
    backdrop-filter: blur(18px);
    -webkit-backdrop-filter: blur(18px);
    margin: 0 -24px;
    padding: 18px 24px 14px;
    border-bottom: 1px solid var(--color-border-subtle);
  }
}

.app-account-menu {
  position: relative;
}

.app-account-trigger {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 236px;
  padding: 10px 14px;
  border-radius: var(--radius-xl);
  background: color-mix(in srgb, var(--color-surface-base) 86%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-border) 75%, rgba(var(--color-primary-rgb), 0.16));
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: all var(--transition-fast);
}

.app-account-trigger:hover {
  border-color: rgba(var(--color-primary-rgb), 0.5);
  background: color-mix(in srgb, var(--color-surface-base) 78%, rgba(var(--color-primary-rgb), 0.06));
}

.app-account-trigger[aria-expanded='true'] {
  border-color: rgba(var(--color-primary-rgb), 0.55);
  background: color-mix(in srgb, var(--color-surface-base) 74%, rgba(var(--color-primary-rgb), 0.08));
}

.app-account-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--color-primary) 88%, #ffffff 12%);
  color: var(--color-text-inverse);
  font-size: 0.98rem;
  font-weight: 700;
  position: relative;
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
  font-weight: 600;
}

.app-account-trigger-meta {
  color: var(--color-text-muted);
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.app-account-trigger-caret {
  color: var(--color-text-muted);
  font-size: 0.8rem;
  transition: transform var(--transition-fast), color var(--transition-fast);
}

.app-account-trigger-caret.is-open {
  transform: rotate(180deg);
  color: var(--color-text-strong);
}

.app-account-dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  width: min(320px, calc(100vw - 24px));
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  border-radius: var(--radius-xl);
  background: color-mix(in srgb, var(--color-surface-base) 84%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-border) 68%, rgba(var(--color-primary-rgb), 0.18));
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  z-index: 30;
  transform-origin: top right;
}

.app-account-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.app-account-section-title {
  color: var(--color-text-muted);
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.16em;
}

.app-account-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border-radius: var(--radius-md);
  background: rgba(var(--color-primary-rgb), 0.04);
  border: 1px solid color-mix(in srgb, var(--color-border-subtle) 82%, rgba(var(--color-primary-rgb), 0.16));
}

.app-account-card-label,
.app-account-card-meta {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.app-account-card-name {
  color: var(--color-text-strong);
  font-size: 0.95rem;
  font-weight: 600;
}

.app-account-logout {
  align-self: flex-start;
}

.app-theme-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.app-theme-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.app-theme-orb {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-primary);
}

.app-theme-orb.is-dark {
  background: #0f172a;
}

.app-theme-orb.is-light {
  background: #f8fafc;
  border: 1px solid var(--color-border);
}

.app-theme-state {
  color: var(--color-text-strong);
  font-size: 0.9rem;
  font-weight: 500;
}

.app-theme-controls {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px;
  border-radius: var(--radius-md);
  background: rgba(var(--color-primary-rgb), 0.04);
  border: 1px solid color-mix(in srgb, var(--color-border-subtle) 86%, rgba(var(--color-primary-rgb), 0.12));
}

.app-theme-option {
  min-width: 80px;
  min-height: 32px;
  border-radius: var(--radius-sm);
  border-color: transparent;
  color: var(--color-text-normal);
}

.app-theme-option.is-active {
  background: color-mix(in srgb, var(--color-surface-base) 90%, rgba(var(--color-primary-rgb), 0.04));
  color: var(--color-primary);
  border-color: rgba(var(--color-primary-rgb), 0.4);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  font-weight: 600;
}

.app-theme-option:not(.is-active):not(:disabled):hover {
  color: var(--color-text-strong);
  background: var(--color-surface-base);
}

.account-menu-fade-enter-active,
.account-menu-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.account-menu-fade-enter-from,
.account-menu-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}

@media (max-width: 640px) {
  main {
    padding: 0 12px 20px;
  }

  .app-account-bar {
    justify-content: stretch;
    padding-top: 12px;
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

  .app-theme-controls {
    width: 100%;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .app-theme-option {
    min-width: 0;
  }
}
</style>
