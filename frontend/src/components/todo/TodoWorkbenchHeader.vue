<script setup lang="ts">
import { computed, useSlots } from 'vue'

const props = defineProps<{
  title: string
  subtitle?: string
}>()

const slots = useSlots()

const hasSummary = computed(() => Boolean(slots.summary || props.subtitle))
const hasActions = computed(() => Boolean(slots.actions))
</script>

<template>
  <header class="workbench-header">
    <div class="workbench-header-copy">
      <h1 class="workbench-header-title">{{ title }}</h1>

      <div v-if="hasSummary" class="workbench-header-summary">
        <slot name="summary">
          {{ subtitle }}
        </slot>
      </div>
    </div>

    <div v-if="hasActions" class="workbench-header-actions">
      <slot name="actions" />
    </div>
  </header>
</template>
