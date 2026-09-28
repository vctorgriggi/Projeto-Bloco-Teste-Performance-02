import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// porta 5173 (a mesma liberada no cors do back-end).
//
// o proxy faz o papel que o nginx faz no conteiner: /api vai para o monolito. com isso
// o front usa uma base relativa e roda igual nos dois lugares, sem uma url de api
// diferente por ambiente embutida no build.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  // testes de componente (vitest), num dom simulado. a api entra dublada em cada
  // teste: o que se testa aqui e o que a tela faz com cada resposta, e nao o back-end.
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
  },
})
