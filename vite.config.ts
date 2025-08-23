import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: './', // Use relative paths for Electron compatibility
  server: {
    port: 3000,
    host: true,
    open: false // Don't auto-open browser when using with Electron
  },
  preview: {
    port: 3000
  }
})
