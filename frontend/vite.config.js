import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 元件按需引入(模板中用到哪個 <el-xxx> 才打包哪個),不再整包註冊;
    // 樣式仍在 main.js 載入完整 CSS,避免 ElMessage 等以 JS 呼叫的元件漏掉樣式
    Components({
      resolvers: [ElementPlusResolver({ importStyle: false })],
      dts: false,
    }),
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/sitemap.xml': 'http://localhost:8080',
      '/robots.txt': 'http://localhost:8080',
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
