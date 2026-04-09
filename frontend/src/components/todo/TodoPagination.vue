<script setup lang="ts">
import type { PageData, TodoItem } from './types'

defineProps<{
  pageData: PageData<TodoItem> | null
  loading: boolean
}>()
defineEmits<{
  (e: 'prevPage'): void
  (e: 'nextPage'): void
}>()
</script>

<template>
  <div class="pagination" v-if="pageData && pageData.totalPages > 1">
    <button class="btn btn-outline btn-sm" :disabled="pageData.first || loading" @click="$emit('prevPage')">{{ $t('pagination.prev') }}</button>
    <span class="page-info">{{ $t('pagination.pageInfo', { page: pageData.page + 1, total: pageData.totalPages }) }}</span>
    <button class="btn btn-outline btn-sm" :disabled="pageData.last || loading" @click="$emit('nextPage')">{{ $t('pagination.next') }}</button>
  </div>
</template>
