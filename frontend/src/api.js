// cliente http unico para falar com a api do back-end. centraliza a base url e
// o tratamento de erro para os componentes nao repetirem fetch na mao.

const BASE_URL = 'http://localhost:8080/api'

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

  listComments: (postId) => request(`/posts/${postId}/comments`),
  addComment: (postId, data) =>
    request(`/posts/${postId}/comments`, { method: 'POST', body: JSON.stringify(data) }),
  deleteComment: (id) => request(`/comments/${id}`, { method: 'DELETE' }),

  listAuthors: () => request('/authors'),
  createAuthor: (data) => request('/authors', { method: 'POST', body: JSON.stringify(data) }),
  updateAuthor: (id, data) => request(`/authors/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteAuthor: (id) => request(`/authors/${id}`, { method: 'DELETE' }),
}
