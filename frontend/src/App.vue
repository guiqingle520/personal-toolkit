<template>
  <main>
    <AuthScreen v-if="!token" />
    <TodoList v-else />
  </main>
</template>

<script setup lang="ts">
import TodoList from './components/TodoList.vue'
import AuthScreen from './components/auth/AuthScreen.vue'
import { useAuth } from './composables/useAuth'

const { token } = useAuth()
</script>

<style scoped>
main {
  max-width: 860px;
  margin: 0 auto;
  padding: 32px 16px;
}
</style>
