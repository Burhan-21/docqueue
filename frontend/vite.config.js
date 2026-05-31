import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
      },
    },
  },
  build: {
    chunkSizeWarningLimit: 600,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('recharts') || id.includes('d3-') || id.includes('victory-vendor')) {
              return 'vendor-charts'
            }
            if (id.includes('@stomp') || id.includes('sockjs')) {
              return 'vendor-stomp'
            }
            if (id.includes('@tanstack')) {
              return 'vendor-query'
            }
            if (id.includes('react-dom') || id.includes('react-router') || id.includes('react/')) {
              return 'vendor-react'
            }
            if (id.includes('axios') || id.includes('date-fns') || id.includes('lucide')) {
              return 'vendor-utils'
            }
          }
        },
      },
    },
  },
})
