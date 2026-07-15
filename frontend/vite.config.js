import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// porta 5173 (a mesma liberada no cors do back-end)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
})
