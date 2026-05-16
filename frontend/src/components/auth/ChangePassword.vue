<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { fetchApi, toApiError } from '../../api'
import { useAuth, type User } from '../../composables/useAuth'

const { t } = useI18n()
const router = useRouter()
const { setSession, token, user } = useAuth()

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

const saving = ref(false)
const error = ref('')
const success = ref('')

interface ChangePasswordResponse {
  user?: User
}

async function changePassword() {
  if (newPassword.value !== confirmPassword.value) {
    error.value = t('changePassword.mismatch')
    return
  }

  try {
    saving.value = true
    error.value = ''
    success.value = ''
    
    const res = await fetchApi<ChangePasswordResponse>('/api/auth/change-password', {
      method: 'POST',
      body: JSON.stringify({
        currentPassword: currentPassword.value,
        newPassword: newPassword.value,
        confirmPassword: confirmPassword.value
      })
    })

    success.value = t('changePassword.success')
    
    if (token.value && user.value) {
      setSession(token.value, {
        ...user.value,
        ...res.data?.user,
        passwordChangeRequired: false
      })
    }

    setTimeout(() => {
      void router.push('/tasks')
    }, 1500)
  } catch (caughtError: unknown) {
    error.value = toApiError(caughtError, t('changePassword.error')).message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="change-password-wrapper">
    <div class="change-password-box">
      <div class="panel-header">
        <h2 class="panel-title">{{ $t('changePassword.title') }}</h2>
      </div>
      
      <div class="panel-body">
        <div v-if="error" class="error-banner">{{ error }}</div>
        <div v-if="success" class="success-banner">{{ success }}</div>

        <form class="settings-form" @submit.prevent="changePassword">
          <div class="form-group">
            <label for="currentPassword">{{ $t('changePassword.currentPassword') }}</label>
            <input id="currentPassword" type="password" v-model="currentPassword" class="form-control" required />
          </div>

          <div class="form-group">
            <label for="newPassword">{{ $t('changePassword.newPassword') }}</label>
            <input id="newPassword" type="password" v-model="newPassword" class="form-control" required />
          </div>

          <div class="form-group">
            <label for="confirmPassword">{{ $t('changePassword.confirmPassword') }}</label>
            <input id="confirmPassword" type="password" v-model="confirmPassword" class="form-control" required />
          </div>

          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="saving || success !== ''">
              {{ saving ? $t('app.syncing') : $t('changePassword.submit') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.change-password-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 50vh;
}

.change-password-box {
  width: 100%;
  max-width: 450px;
  background: var(--color-surface-base);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-subtle);
  background: var(--color-surface-hover);
}

.panel-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

.panel-body {
  padding: 24px 20px;
}

.error-banner {
  background: var(--color-danger-bg, #fee2e2);
  color: var(--color-danger-text, #ef4444);
  padding: 12px;
  border-radius: var(--radius-md);
  margin-bottom: 20px;
  font-size: 0.9rem;
}

.success-banner {
  background: var(--color-success-bg, #dcfce7);
  color: var(--color-success-text, #22c55e);
  padding: 12px;
  border-radius: var(--radius-md);
  margin-bottom: 20px;
  font-size: 0.9rem;
}

.settings-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

label {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--color-text-strong);
}

.form-control {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-surface-base);
  color: var(--color-text-normal);
  font-size: 0.95rem;
}

.form-actions {
  margin-top: 10px;
}

.form-actions .btn {
  width: 100%;
  padding: 10px;
  font-weight: 600;
}
</style>
