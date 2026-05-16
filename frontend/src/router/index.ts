import { createRouter, createWebHistory, type RouterHistory, type RouteLocationNormalized } from 'vue-router'

import TodoList from '../components/TodoList.vue'
import TodoStatisticsView from '../components/todo/TodoStatisticsView.vue'
import ChangePassword from '../components/auth/ChangePassword.vue'
import SecuritySettings from '../components/auth/SecuritySettings.vue'
import { useAuth } from '../composables/useAuth'

export function createAppRouter(history: RouterHistory = createWebHistory()) {
  const router = createRouter({
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
      {
        path: '/change-password',
        name: 'changePassword',
        component: ChangePassword,
      },
      {
        path: '/security-settings',
        name: 'securitySettings',
        component: SecuritySettings,
      },
    ],
  })

  router.beforeEach((to: RouteLocationNormalized) => {
    const { user } = useAuth()
    
    // If user requires password change, restrict them to /change-password
    if (user.value?.passwordChangeRequired && to.path !== '/change-password') {
      return '/change-password'
    }

    // If user does not require password change, redirect away from /change-password
    if (!user.value?.passwordChangeRequired && to.path === '/change-password') {
      return '/tasks'
    }
  })

  return router
}

const router = createAppRouter()

export default router
