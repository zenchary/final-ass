import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  // Thêm phần server proxy này
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      // Proxy để load ảnh sản phẩm từ backend
      '/photos': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Proxy để load avatar
      '/avatars': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})