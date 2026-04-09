<script setup lang="ts">
defineProps<{
  loading: boolean
  isEmpty: boolean
}>()
</script>

<template>
  <div v-show="isEmpty" class="state-message" :class="loading ? 'loading' : 'empty'">
    <template v-if="loading">
      <div class="spinner"></div> {{ $t('status.initiating') }}
    </template>
    <template v-else>
      {{ $t('status.empty') }}
    </template>
  </div>
</template>
