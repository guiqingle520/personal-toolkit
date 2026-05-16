<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { fetchApi, toApiError } from '../../api'

const { t } = useI18n()
const router = useRouter()

const loading = ref(true)
const saving = ref(false)
const error = ref('')
const success = ref('')
const unauthorized = ref(false)

interface SecurityPolicy {
  accessTokenTtlSeconds: number
  effectiveAccessTokenTtlSeconds: number
  passwordExpiryEnabled: boolean
  passwordExpiryDays: number
}

const policy = ref<SecurityPolicy>({
  accessTokenTtlSeconds: 3600,
  effectiveAccessTokenTtlSeconds: 3600,
  passwordExpiryEnabled: false,
  passwordExpiryDays: 90
})

async function loadPolicy() {
  try {
    loading.value = true
    error.value = ''
    unauthorized.value = false
    const res = await fetchApi<SecurityPolicy>('/api/auth/security-policy', { method: 'GET' })
    if (res.data) {
      policy.value = res.data
    }
  } catch (caughtError: unknown) {
    const apiError = toApiError(caughtError, t('securitySettings.error'))

    if (apiError.status === 403) {
      unauthorized.value = true
    } else {
      error.value = apiError.message
    }
  } finally {
    loading.value = false
  }
}

async function savePolicy() {
  try {
    saving.value = true
    error.value = ''
    success.value = ''
    const res = await fetchApi<SecurityPolicy>('/api/auth/security-policy', {
      method: 'PUT',
      body: JSON.stringify({
        accessTokenTtlSeconds: policy.value.accessTokenTtlSeconds,
        passwordExpiryEnabled: policy.value.passwordExpiryEnabled,
        passwordExpiryDays: policy.value.passwordExpiryDays
      })
    })
    if (res.data) {
      policy.value = res.data
      success.value = t('securitySettings.success')
      setTimeout(() => { success.value = '' }, 3000)
    }
  } catch (caughtError: unknown) {
    const apiError = toApiError(caughtError, t('securitySettings.error'))

    if (apiError.status === 403) {
      unauthorized.value = true
    } else {
      error.value = apiError.message
    }
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadPolicy()
})
</script>

<template>
  <div class="security-settings-view">
    <div class="panel-header">
      <h2 class="panel-title">{{ $t('securitySettings.title') }}</h2>
    </div>

    <div v-if="loading" class="panel-body loading-state">
      {{ $t('app.syncing') }}
    </div>

    <div v-else-if="unauthorized" class="panel-body unauthorized-state">
      <div class="error-banner">{{ $t('securitySettings.unauthorized') }}</div>
      <div style="margin-top: 1rem;">
        <button class="btn btn-outline" @click="router.push('/tasks')">{{ $t('form.cancel') }}</button>
      </div>
    </div>

    <div v-else class="panel-body">
      <div v-if="error" class="error-banner">{{ error }}</div>
      <div v-if="success" class="success-banner">{{ success }}</div>

      <form class="settings-form" @submit.prevent="savePolicy">
        <div class="form-group">
          <label for="accessTokenTtl">{{ $t('securitySettings.accessTokenTtl') }}</label>
          <input id="accessTokenTtl" type="number" v-model.number="policy.accessTokenTtlSeconds" class="form-control" required />
          <small class="form-help">{{ $t('securitySettings.ttlNotice') }}</small>
        </div>

        <div class="form-group">
          <label for="effectiveTtl">{{ $t('securitySettings.effectiveTtl') }}</label>
          <input id="effectiveTtl" type="number" :value="policy.effectiveAccessTokenTtlSeconds" class="form-control" disabled readonly />
        </div>

        <div class="form-group checkbox-group">
          <input id="passwordExpiryEnabled" type="checkbox" v-model="policy.passwordExpiryEnabled" />
          <label for="passwordExpiryEnabled">{{ $t('securitySettings.passwordExpiryEnabled') }}</label>
        </div>

        <div class="form-group" v-if="policy.passwordExpiryEnabled">
          <label for="passwordExpiryDays">{{ $t('securitySettings.passwordExpiryDays') }}</label>
          <input id="passwordExpiryDays" type="number" v-model.number="policy.passwordExpiryDays" class="form-control" required min="1" />
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="saving">
            {{ saving ? $t('app.syncing') : $t('securitySettings.save') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.security-settings-view {
  max-width: 600px;
  margin: 0 auto;
  background: var(--color-surface-base);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
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
  padding: 20px;
}

.loading-state,
.unauthorized-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--color-text-muted);
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

.checkbox-group {
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.checkbox-group label {
  margin-bottom: 0;
  font-weight: normal;
}

label {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--color-text-strong);
}

.form-control {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-surface-base);
  color: var(--color-text-normal);
  font-size: 0.95rem;
}

.form-control:disabled {
  background: var(--color-surface-hover);
  color: var(--color-text-muted);
  cursor: not-allowed;
}

.form-help {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.form-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}
</style>
