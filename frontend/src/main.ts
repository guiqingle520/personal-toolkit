import './styles/todo.css'
import './styles/tokens.css'
import './styles/ui.css'
import { createApp } from 'vue'
import App from './App.vue'
import i18n, { syncDocumentLocale } from './i18n'

syncDocumentLocale(i18n.global.locale)
createApp(App).use(i18n).mount('#app')
