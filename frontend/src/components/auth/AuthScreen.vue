<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuth } from '../../composables/useAuth'
import { fetchApi, toApiError } from '../../api'

const { t } = useI18n()
const { setSession } = useAuth()
const isLogin = ref(true)
const username = ref('')
const email = ref('')
const password = ref('')
const captchaId = ref('')
const captchaCode = ref('')
const captchaImage = ref('')
const captchaLoadError = ref('')
const loading = ref(false)
const errorMessage = ref('')
const validationErrors = ref<Record<string, string[]>>({})
const captchaEnabled = ref(false)
const adaptiveCaptcha = ref(false)
const adaptiveTriggerThreshold = ref(1)
const showCaptcha = ref(false)

async function loadLoginPolicy() {
  try {
    const res = await fetchApi<{ captchaEnabled: boolean; adaptiveCaptcha: boolean; adaptiveTriggerThreshold: number }>('/api/auth/login-policy', { method: 'GET' }, t)
    captchaEnabled.value = res.data?.captchaEnabled ?? false
    adaptiveCaptcha.value = res.data?.adaptiveCaptcha ?? false
    adaptiveTriggerThreshold.value = res.data?.adaptiveTriggerThreshold ?? 1
    showCaptcha.value = captchaEnabled.value && !adaptiveCaptcha.value

    if (showCaptcha.value) {
      captchaLoadError.value = ''
      await loadCaptcha()
      return
    }

    captchaId.value = ''
    captchaImage.value = ''
    captchaCode.value = ''
    captchaLoadError.value = ''
  } catch (loadPolicyError) {
    void loadPolicyError
    captchaEnabled.value = false
    adaptiveCaptcha.value = false
    adaptiveTriggerThreshold.value = 1
    showCaptcha.value = false
    captchaId.value = ''
    captchaImage.value = ''
    captchaCode.value = ''
    captchaLoadError.value = ''
  }
}

async function loadCaptcha() {
  if (!captchaEnabled.value) {
    return
  }

  try {
    const res = await fetchApi<{ captchaId: string; image: string }>('/api/auth/captcha', { method: 'GET' }, t)
    captchaId.value = res.data?.captchaId ?? ''
    captchaImage.value = res.data?.image ?? ''
    captchaCode.value = ''
    captchaLoadError.value = captchaImage.value ? '' : t('auth.captchaLoadFailed')
  } catch (loadCaptchaError) {
    void loadCaptchaError
    captchaId.value = ''
    captchaImage.value = ''
    captchaLoadError.value = t('auth.captchaLoadFailed')
  }
}

async function submit() {
  loading.value = true
  errorMessage.value = ''
  validationErrors.value = {}

  try {
    const endpoint = isLogin.value ? '/api/auth/login' : '/api/auth/register'
    const body = isLogin.value
      ? { username: username.value, password: password.value, captchaId: captchaId.value, captchaCode: captchaCode.value }
      : { username: username.value, email: email.value, password: password.value }

    const res = await fetchApi<{ token: string; user?: { id: number; username: string; email?: string; passwordChangeRequired?: boolean } }>(endpoint, {
      method: 'POST',
      body: JSON.stringify(body)
    }, t)

    if (res.data?.token) {
      setSession(res.data.token, res.data.user ?? null)
    }
  } catch (error) {
    if (error instanceof Error) {
      const apiError = toApiError(error, t('feedback.genericError'))
      if (apiError.code === 'CAPTCHA_REQUIRED' && captchaEnabled.value) {
        showCaptcha.value = true
        await loadCaptcha()
      } else if (isLogin.value && showCaptcha.value) {
        await loadCaptcha()
      }
      errorMessage.value = apiError.message || t('feedback.genericError')
      validationErrors.value = apiError.validation || {}
    } else {
      if (isLogin.value && showCaptcha.value) {
        await loadCaptcha()
      }
      errorMessage.value = t('feedback.unexpectedError')
    }
  } finally {
    loading.value = false
  }
}

if (isLogin.value) {
  void loadLoginPolicy()
}
</script>

<template>
  <div class="auth-screen">
    <div class="auth-card">
      <h2 class="auth-title">{{ isLogin ? $t('auth.loginTitle') : $t('auth.registerTitle') }}</h2>
      
      <form @submit.prevent="submit" class="auth-form">
        <div class="form-group">
          <label for="username">{{ isLogin ? $t('auth.loginIdentifier') : $t('auth.username') }}</label>
          <input 
            id="username" 
            v-model="username" 
            type="text" 
            class="cyber-input" 
            required 
            autocomplete="username"
          />
        </div>
        
        <div class="form-group" v-if="!isLogin">
          <label for="email">{{ $t('auth.email') }}</label>
          <input 
            id="email" 
            v-model="email" 
            type="email" 
            class="cyber-input" 
            required 
            autocomplete="email"
          />
        </div>

        <div class="form-group">
          <label for="password">{{ $t('auth.password') }}</label>
          <input 
            id="password" 
            v-model="password" 
            type="password" 
            class="cyber-input" 
            required 
            autocomplete="current-password"
          />
        </div>

        <div class="form-group" v-if="isLogin && showCaptcha">
          <label for="captcha">{{ $t('auth.captcha') }}</label>
           <div class="captcha-row">
             <img
               v-if="captchaImage"
              :src="captchaImage"
              :alt="$t('auth.captcha')"
              class="captcha-image"
             />
             <span v-else class="captcha-placeholder">{{ captchaLoadError || $t('app.syncing') }}</span>
             <button type="button" class="btn btn-ghost btn-sm" @click="loadCaptcha">{{ $t('auth.refreshCaptcha') }}</button>
           </div>
          <input
            id="captcha"
            v-model="captchaCode"
            type="text"
            class="cyber-input"
            :placeholder="$t('auth.captchaPlaceholder')"
            required
            autocomplete="off"
          />
        </div>

        <div v-if="errorMessage" class="error-banner">
          <strong>{{ $t('status.error') }}</strong> {{ errorMessage }}
          <ul v-if="Object.keys(validationErrors).length > 0" class="validation-list">
            <li v-for="(errors, field) in validationErrors" :key="field">
              {{ field }}: {{ errors.join(', ') }}
            </li>
          </ul>
        </div>

        <button type="submit" class="btn btn-primary auth-submit" :disabled="loading">
          {{ loading ? $t('app.syncing') : (isLogin ? $t('auth.loginBtn') : $t('auth.registerBtn')) }}
        </button>
      </form>

      <div class="auth-toggle">
        <span v-if="isLogin">
          {{ $t('auth.noAccount') }} 
            <a href="#" @click.prevent="isLogin = false; errorMessage = ''; validationErrors = {}; showCaptcha = false; captchaId = ''; captchaImage = ''; captchaCode = ''">{{ $t('auth.registerLink') }}</a>
          </span>
          <span v-else>
            {{ $t('auth.hasAccount') }} 
            <a href="#" @click.prevent="isLogin = true; errorMessage = ''; validationErrors = {}; void loadLoginPolicy()">{{ $t('auth.loginLink') }}</a>
          </span>
        </div>
      </div>
  </div>
</template>

<style scoped>
.auth-screen {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 96px 16px 64px;
  background: var(--color-app-bg);
}
.auth-card {
  background: color-mix(in srgb, var(--color-surface-base) 92%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-border) 72%, rgba(var(--color-primary-rgb), 0.16));
  border-radius: var(--radius-lg);
  padding: 48px;
  width: 100%;
  max-width: 460px;
  box-shadow: 0 20px 56px rgba(0, 0, 0, 0.16);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}
.auth-title {
  margin-top: 0;
  margin-bottom: 40px;
  text-align: center;
  color: var(--color-text-strong);
  font-size: 2rem;
  font-family: var(--font-serif);
  font-weight: 500;
  letter-spacing: 0.04em;
}
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.form-group label {
  font-size: 0.78rem;
  font-weight: 500;
  color: var(--color-text-normal);
  text-transform: uppercase;
  letter-spacing: 0.12em;
}
.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.captcha-image {
  height: 40px;
  min-width: 120px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface-input);
}
.captcha-placeholder {
  min-width: 120px;
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  background: var(--color-surface-hover);
  font-size: 0.85rem;
}
.auth-submit {
  margin-top: 8px;
  width: 100%;
  padding: 10px;
  font-size: 1rem;
}
.auth-toggle {
  margin-top: 28px;
  text-align: center;
  font-size: 0.9rem;
  color: var(--color-text-normal);
}
.auth-toggle a {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 600;
}
.auth-toggle a:hover {
  text-decoration: none;
  color: var(--color-primary-dark);
}

@media (max-width: 640px) {
  .auth-screen {
    padding: 56px 12px 28px;
  }

  .auth-card {
    padding: 32px 24px;
  }

  .auth-title {
    font-size: 1.7rem;
    margin-bottom: 32px;
  }
}
</style>
