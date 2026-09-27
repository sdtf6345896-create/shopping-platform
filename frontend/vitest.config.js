import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    // 與 vite.config.js 相同的按需引入設定,元件測試才會渲染真的 Element Plus 元件
    Components({
      resolvers: [ElementPlusResolver({ importStyle: false })],
      dts: false,
    }),
  ],
  test: {
    environment: 'jsdom',
    // Element Plus 以 ESM 發佈,需要交給 Vite 轉換
    server: {
      deps: {
        inline: ['element-plus'],
      },
    },
  },
})
