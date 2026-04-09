<template>
  <main>
    <TodoList />
  </main>
</template>

<script setup lang="ts">
import TodoList from './components/TodoList.vue'
</script>

<style scoped>
main {
  max-width: 860px;
  margin: 0 auto;
  padding: 32px 16px;
}
</style>
