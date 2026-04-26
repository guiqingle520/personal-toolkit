<script setup lang="ts">
import { computed, useSlots } from 'vue'

const slots = useSlots()

const hasHeader = computed(() => Boolean(slots.header))
const hasMenu = computed(() => Boolean(slots.menu))
const hasSidebar = computed(() => Boolean(slots.sidebar))

const bodyClass = computed(() => ({
  'workbench-body--no-menu': !hasMenu.value,
  'workbench-body--no-sidebar': !hasSidebar.value,
}))
</script>

<template>
  <div class="workbench-layout">
    <div v-if="hasHeader" class="workbench-top">
      <slot name="header" />
    </div>

    <div class="workbench-body" :class="bodyClass">
      <aside v-if="hasMenu" class="workbench-menu">
        <slot name="menu" />
      </aside>

      <main class="workbench-main">
        <slot />
      </main>

      <aside v-if="hasSidebar" class="workbench-sidebar">
        <slot name="sidebar" />
      </aside>
    </div>
  </div>
</template>
