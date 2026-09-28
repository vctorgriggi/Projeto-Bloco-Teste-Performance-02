// cliente http unico para falar com a api do back-end. centraliza a base url e
// o tratamento de erro para os componentes nao repetirem fetch na mao.
//
// a base url continua sendo uma so, mesmo agora que o sistema tem dois servicos:
// comentarios e reacoes vivem no microsservico de engajamento, mas o navegador nao
// fala com ele direto -- o monolito e a porta de entrada e alcanca o engajamento por
// dentro. do lado do front, portanto, nada de descoberta de servico, segunda origem
// ou segundo cors.
//
// desde a quinta entrega a base e relativa (/api): a mesma construcao do front serve
// em qualquer endereco. em desenvolvimento, o vite repassa /api ao monolito (veja
// vite.config.js); no conteiner, e o nginx que repassa. o navegador so ve uma origem.

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })

  if (response.status === 204) {
    return null
  }

  const body = await response.json().catch(() => null)

  if (!response.ok) {
    // a api devolve um ApiError com a mensagem; repassamos para a ui exibir
    const message = body?.message || 'Algo deu errado ao falar com o servidor'
    throw new Error(message)
  }

  return body
}

export const api = {
  listPosts: () => request('/posts'),
  getPost: (id) => request(`/posts/${id}`),
  postHistory: (id) => request(`/posts/${id}/history`),
  createPost: (data) => request('/posts', { method: 'POST', body: JSON.stringify(data) }),
  updatePost: (id, data) => request(`/posts/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  publishPost: (id) => request(`/posts/${id}/publish`, { method: 'POST' }),
  deletePost: (id) => request(`/posts/${id}`, { method: 'DELETE' }),

  // comentarios e reacoes sao servidos pelo microsservico de engajamento, atraves do
  // monolito. quando ele esta fora do ar, as leituras respondem 503 e a mensagem de
  // erro chega aqui como qualquer outra.
  listComments: (postId) => request(`/posts/${postId}/comments`),
  // enviar um comentario nao espera o engajamento: o monolito poe um comando na fila e
  // responde 202 com o envio pendente ({ submissionId, status: 'PENDING', ... }). o
  // comentario aparece na listagem quando o engajamento processar a mensagem, com o
  // mesmo submissionId. 503 aqui significa que o broker, e nao o engajamento, esta fora.
  addComment: (postId, data) =>
    request(`/posts/${postId}/comments`, { method: 'POST', body: JSON.stringify(data) }),
  deleteComment: (id) => request(`/comments/${id}`, { method: 'DELETE' }),

  reactionSummary: (postId, reader) =>
    request(`/posts/${postId}/reactions${reader ? `?reader=${encodeURIComponent(reader)}` : ''}`),
  react: (postId, data) =>
    request(`/posts/${postId}/reactions`, { method: 'POST', body: JSON.stringify(data) }),
  undoReaction: (postId, type, reader) =>
    request(`/posts/${postId}/reactions/${type}?reader=${encodeURIComponent(reader)}`, {
      method: 'DELETE',
    }),

  // diagnostico da integracao: responde sempre 200, com available dizendo se o
  // engajamento esta de pe agora e brokerAvailable dizendo o mesmo da fila
  engagementStatus: () => request('/engagement/status'),
  // os totais de conversa e reacoes de todos os posts, numa chamada so. vem da copia
  // que o monolito mantem a partir dos eventos do engajamento, e nao do microsservico:
  // responde mesmo com ele fora do ar
  engagementCounters: () => request('/engagement/counters'),

  listAuthors: () => request('/authors'),
  createAuthor: (data) => request('/authors', { method: 'POST', body: JSON.stringify(data) }),
  updateAuthor: (id, data) => request(`/authors/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteAuthor: (id) => request(`/authors/${id}`, { method: 'DELETE' }),
}
