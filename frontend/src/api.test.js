import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from './api.js'

// o cliente http unico do front. tres contratos que os componentes assumem: a base e
// relativa (/api), o 204 vira null, e o erro traz a mensagem do ApiError do back-end.
describe('api', () => {
  afterEach(() => vi.unstubAllGlobals())

  function respondeCom(status, corpo) {
    const fetch = vi.fn().mockResolvedValue({
      status,
      ok: status >= 200 && status < 300,
      json: () => (corpo === undefined ? Promise.reject(new Error('sem corpo')) : Promise.resolve(corpo)),
    })
    vi.stubGlobal('fetch', fetch)
    return fetch
  }

  it('usa a base relativa, que o nginx e o vite repassam ao monolito', async () => {
    const fetch = respondeCom(200, [])

    await api.listPosts()

    expect(fetch).toHaveBeenCalledWith('/api/posts', expect.any(Object))
  })

  it('o envio de comentario devolve o envio pendente do 202', async () => {
    respondeCom(202, { submissionId: 'abc', status: 'PENDING' })

    const envio = await api.addComment(1, { authorName: 'Carla', content: 'oi' })

    expect(envio).toEqual({ submissionId: 'abc', status: 'PENDING' })
  })

  it('204 vira null', async () => {
    respondeCom(204)

    expect(await api.deletePost(1)).toBeNull()
  })

  // a mensagem escrita por quem recusou (o 503 do broker, o 409 da reacao repetida)
  // chega ate a tela
  it('erro traz a mensagem do ApiError', async () => {
    respondeCom(503, { status: 503, message: 'A fila de mensagens esta indisponivel' })

    await expect(api.addComment(1, {})).rejects.toThrow('A fila de mensagens esta indisponivel')
  })

  it('erro sem corpo vira uma mensagem generica', async () => {
    respondeCom(500)

    await expect(api.listPosts()).rejects.toThrow('Algo deu errado ao falar com o servidor')
  })
})
