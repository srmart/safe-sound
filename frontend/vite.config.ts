import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    headers: {
      'X-Content-Type-Options': 'nosniff',
      'X-Frame-Options': 'DENY',
      'Referrer-Policy': 'no-referrer',
      'Cross-Origin-Opener-Policy': 'same-origin',
      'X-Permitted-Cross-Domain-Policies': 'none'
    },
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
