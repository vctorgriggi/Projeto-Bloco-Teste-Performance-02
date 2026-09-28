// teste de carga (k6) contra o blog implantado. roda de dentro do cluster:
//
//   scripts/k8s-carga.sh
//
// ou contra qualquer endereco, com o k6 instalado:  k6 run -e BASE=http://localhost:30080 scripts/carga.js
//
// o perfil mistura o que o leitor faz: ler a estante, abrir um post (conversa e
// reacoes, que vao ao microsservico por http) e, de vez em quando, comentar (que vai
// pela fila). serve para duas coisas:
//
// 1. ver o autoescalonamento acontecer: a carga passa a cpu do engajamento do alvo do
//    hpa, e as replicas sobem de 2 para 4 -- e descem quando ela acaba;
// 2. conferir que o sistema aguenta: os limites (thresholds) no fim reprovam o teste se
//    mais de 1% das requisicoes falhar ou se o p95 passar de 1s.
import http from 'k6/http'
import { check, sleep } from 'k6'

const BASE = __ENV.BASE || 'http://frontend:8080'
const API = `${BASE}/api`

export const options = {
  scenarios: {
    leitores: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 40 }, // chegando
        { duration: '2m', target: 40 },  // pico sustentado
        { duration: '30s', target: 0 },  // indo embora
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
    checks: ['rate>0.99'],
  },
}

export function setup() {
  const posts = http.get(`${API}/posts`).json()
  if (!posts.length) throw new Error('a estante esta vazia: rode scripts/e2e.sh ou crie um post antes')
  return { postIds: posts.map((p) => p.id) }
}

export default function ({ postIds }) {
  const postId = postIds[Math.floor(Math.random() * postIds.length)]
  const leitor = `leitor-${__VU}`

  check(http.get(`${API}/posts`), { 'estante 200': (r) => r.status === 200 })
  check(http.get(`${API}/engagement/counters`), { 'contadores 200': (r) => r.status === 200 })
  check(http.get(`${API}/posts/${postId}/comments`), { 'conversa 200': (r) => r.status === 200 })
  check(http.get(`${API}/posts/${postId}/reactions?reader=${leitor}`), { 'reacoes 200': (r) => r.status === 200 })

  // um em cada dez leitores comenta: vai pela fila, e o engajamento consome
  if (Math.random() < 0.1) {
    const envio = http.post(
      `${API}/posts/${postId}/comments`,
      JSON.stringify({ authorName: leitor, content: `recado de carga ${Date.now()}` }),
      { headers: { 'Content-Type': 'application/json' } },
    )
    check(envio, { 'comentario 202': (r) => r.status === 202 })
  }

  sleep(0.5)
}
