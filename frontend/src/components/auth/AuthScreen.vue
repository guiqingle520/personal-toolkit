<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuth } from '../../composables/useAuth'
import { fetchApi } from '../../api'

const { t } = useI18n()
const { setSession } = useAuth()
const isLogin = ref(true)
const username = ref('')
const email = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')
const validationErrors = ref<Record<string, string[]>>({})

async function submit() {
  loading.value = true
  errorMessage.value = ''
  validationErrors.value = {}

  try {
    const endpoint = isLogin.value ? '/api/auth/login' : '/api/auth/register'
    const body = isLogin.value
      ? { username: username.value, password: password.value }
      : { username: username.value, email: email.value, password: password.value }

    const res = await fetchApi<{ token: string; user?: { id: number; username: string; email?: string } }>(endpoint, {
      method: 'POST',
      body: JSON.stringify(body)
    }, t)

    if (res.data?.token) {
      setSession(res.data.token, res.data.user ?? null)
    }
  } catch (error) {
    if (error instanceof Error) {
      try {
        const parsed = JSON.parse(error.message)
        errorMessage.value = parsed.message || t('feedback.genericError')
        validationErrors.value = parsed.validation || {}
      } catch {
        errorMessage.value = error.message
        validationErrors.value = {}
      }
    } else {
      errorMessage.value = t('feedback.unexpectedError')
    }
  } finally {
    loading.value = false
  }
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
            <a href="#" @click.prevent="isLogin = false; errorMessage = ''; validationErrors = {}">{{ $t('auth.registerLink') }}</a>
          </span>
          <span v-else>
            {{ $t('auth.hasAccount') }} 
            <a href="#" @click.prevent="isLogin = true; errorMessage = ''; validationErrors = {}">{{ $t('auth.loginLink') }}</a>
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
  min-height: 80vh;
}
.auth-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 32px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.auth-title {
  margin-top: 0;
  margin-bottom: 24px;
  text-align: center;
  color: var(--text-primary);
  font-size: 1.5rem;
}
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.form-group label {
  font-size: 0.9rem;
  color: var(--text-secondary);
}
.auth-submit {
  margin-top: 8px;
  width: 100%;
  padding: 12px;
  font-size: 1rem;
}
.auth-toggle {
  margin-top: 24px;
  text-align: center;
  font-size: 0.9rem;
  color: var(--text-secondary);
}
.auth-toggle a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}
.auth-toggle a:hover {
  text-decoration: underline;
}
.error-banner {
  margin-top: 8px;
  padding: 12px;
  background: rgba(255, 68, 68, 0.1);
  border-left: 4px solid var(--danger-color, #ff4444);
  color: var(--text-primary);
  font-size: 0.85rem;
  border-radius: 4px;
}
.validation-list {
  margin: 8px 0 0 0;
  padding-left: 20px;
}
</style>
