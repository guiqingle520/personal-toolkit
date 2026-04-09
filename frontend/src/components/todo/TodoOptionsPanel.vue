<script setup lang="ts">
defineProps<{
  options: { categories: string[], tags: string[] }
  show: boolean
}>()
</script>

<template>
  <div v-if="show" class="options-panel">
    <h3>{{ $t('options.knownCategories') }}</h3>
    <div class="options-list">
      <span v-for="c in options.categories" :key="c" class="badge badge-category">{{ c }}</span>
      <span v-if="!options.categories.length">{{ $t('options.noCategories') }}</span>
    </div>
    <h3>{{ $t('options.knownTags') }}</h3>
    <div class="options-list">
      <span v-for="t in options.tags" :key="t" class="badge badge-tag">#{{ t }}</span>
      <span v-if="!options.tags.length">{{ $t('options.noTags') }}</span>
    </div>
  </div>
</template>
