import './styles/todo.css'
import './styles/tokens.css'
import './styles/ui.css'
import { createApp } from 'vue'
import App from './App.vue'
import i18n from './i18n'

createApp(App).use(i18n).mount('#app')
