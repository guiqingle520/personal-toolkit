import { createRouter, createWebHistory, type RouterHistory } from 'vue-router'

import TodoList from '../components/TodoList.vue'
import TodoStatisticsView from '../components/todo/TodoStatisticsView.vue'

export function createAppRouter(history: RouterHistory = createWebHistory()) {
  return createRouter({
    history,
    routes: [
      {
        path: '/',
        redirect: '/tasks',
      },
      {
        path: '/tasks',
        name: 'tasks',
        component: TodoList,
      },
      {
        path: '/statistics',
        name: 'statistics',
        component: TodoStatisticsView,
      },
    ],
  })
}

const router = createAppRouter()

export default router
