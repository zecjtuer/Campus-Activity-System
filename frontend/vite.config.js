import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    host: '0.0.0.0',   // 监听所有网卡, 允许局域网其他设备访问
    open: true,
    proxy: {
      // 开发环境代理: 前端 /api → 后端 http://localhost:8080, 避免 CORS
      // 注意: proxy 运行在宿主机本机, target 保持 localhost 即可, 不要改成局域网IP
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
