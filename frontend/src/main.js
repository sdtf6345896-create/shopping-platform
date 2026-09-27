import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElMessage, ElMessageBox, provideGlobalConfig } from 'element-plus'
import 'element-plus/dist/index.css'
import zhTw from 'element-plus/es/locale/lang/zh-tw'

import './style.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// Element Plus 元件在 vite.config.js 按需引入,不再 app.use(ElementPlus)。
// 全域語系改用 provideGlobalConfig;ElMessage/ElMessageBox 另外 install,
// 讓以 JS 呼叫的對話框也拿得到 app context(否則按鈕文字會變回英文 OK/Cancel)。
provideGlobalConfig({ locale: zhTw }, app, true)
app.use(ElMessage)
app.use(ElMessageBox)

app.mount('#app')
