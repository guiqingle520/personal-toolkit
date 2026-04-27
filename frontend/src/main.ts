import './styles/todo.css'
import './styles/tokens.css'
import './styles/ui.css'
import { createApp } from 'vue'
import App from './App.vue'
import i18n, { syncDocumentLocale } from './i18n'
import router from './router'
import { initializeTheme } from './theme'

syncDocumentLocale(i18n.global.locale)
initializeTheme()
createApp(App).use(router).use(i18n).mount('#app')
