import { createApp } from 'vue'
import { createPinia } from 'pinia' // 引入 Pinia
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia) // 注册 Pinia
app.use(router)
app.use(ElementPlus)
app.mount('#app')