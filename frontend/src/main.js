import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/styles.css'
import '@fontsource/zhi-mang-xing/chinese-simplified-400.css'
import '@fontsource/ma-shan-zheng/chinese-simplified-400.css'
import reveal from './directives/reveal'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.directive('reveal', reveal)
app.mount('#app')
