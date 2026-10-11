import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/proyectos': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/perfil': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
