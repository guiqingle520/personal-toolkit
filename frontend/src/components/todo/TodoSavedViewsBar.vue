<script setup lang="ts">
import type { TodoSavedView } from './types'

defineProps<{
  savedViews: TodoSavedView[]
}>()

defineEmits<{
  (e: 'apply', savedView: TodoSavedView): void
  (e: 'set-default', id: number): void
  (e: 'rename', savedView: TodoSavedView): void
  (e: 'delete', id: number): void
}>()
</script>

<template>
  <section v-if="savedViews.length" class="saved-views-panel">
    <div class="saved-views-title-row">
      <h3>{{ $t('savedViews.title') }}</h3>
    </div>

    <div class="saved-views-list">
      <div v-for="savedView in savedViews" :key="savedView.id" class="saved-view-item">
        <button type="button" class="btn btn-sm btn-outline saved-view-apply" @click="$emit('apply', savedView)">
          {{ savedView.name }}
        </button>
        <span v-if="savedView.isDefault" class="saved-view-default">{{ $t('savedViews.defaultBadge') }}</span>
        <div class="saved-view-actions">
          <button type="button" class="btn btn-sm btn-ghost" @click="$emit('set-default', savedView.id)">{{ $t('savedViews.setDefault') }}</button>
          <button type="button" class="btn btn-sm btn-ghost" @click="$emit('rename', savedView)">{{ $t('savedViews.rename') }}</button>
          <button type="button" class="btn btn-sm btn-danger-outline" @click="$emit('delete', savedView.id)">{{ $t('savedViews.delete') }}</button>
        </div>
      </div>
    </div>
  </section>
</template>
