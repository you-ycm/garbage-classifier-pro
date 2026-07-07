import { createApp } from 'vue'
import axios from 'axios'
import { createPinia } from 'pinia' // 引入 Pinia
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router' // 引入路由

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(createPinia()) // 使用 Pinia
app.use(ElementPlus)
app.use(router) // 使用路由
app.mount('#app')

// 设置 axios 默认配置
axios.defaults.baseURL = 'http://192.168.58.128:8080/api'
axios.defaults.timeout = 10000