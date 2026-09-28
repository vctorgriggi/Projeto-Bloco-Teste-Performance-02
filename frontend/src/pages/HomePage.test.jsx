import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import HomePage from './HomePage.jsx'
import { api } from '../api.js'

vi.mock('../api.js', () => ({
  api: { listPosts: vi.fn(), engagementCounters: vi.fn() },
}))

const posts = [
  { id: 1, title: 'Comecando com Spring Boot', content: 'texto', authorName: 'Ana', status: 'PUBLISHED' },
  { id: 2, title: 'Organizando o codigo', content: 'texto', authorName: 'Ana', status: 'DRAFT' },
]

function renderizar() {
  return render(
    <MemoryRouter>
      <HomePage />
    </MemoryRouter>,
  )
}

// a estante com os totais de conversa de cada post, vindos da copia local que o
// monolito mantem pelos eventos do engajamento (quarta entrega)
describe('HomePage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('mostra os totais de cada post, no singular e no plural', async () => {
    api.listPosts.mockResolvedValue(posts)
    api.engagementCounters.mockResolvedValue([
      { postId: 1, comments: 3, reactions: 1 },
      { postId: 2, comments: 1, reactions: 0 },
    ])

    renderizar()

    expect(await screen.findByText('3 recados · 1 reação')).toBeInTheDocument()
    expect(await screen.findByText('1 recado · 0 reações')).toBeInTheDocument()
  })

  it('post sem conversa nenhuma nao mostra linha de totais', async () => {
    api.listPosts.mockResolvedValue(posts)
    api.engagementCounters.mockResolvedValue([{ postId: 1, comments: 0, reactions: 0 }])

    renderizar()

    await screen.findByText('Comecando com Spring Boot')
    expect(screen.queryByText(/recados?/)).not.toBeInTheDocument()
  })

  // os totais sao enfeite: se ate eles falharem, a estante aparece sem os numeros, e
  // nao com um erro no lugar dos posts
  it('falha nos contadores nao derruba a estante', async () => {
    api.listPosts.mockResolvedValue(posts)
    api.engagementCounters.mockRejectedValue(new Error('fora do ar'))

    renderizar()

    expect(await screen.findByText('Comecando com Spring Boot')).toBeInTheDocument()
    expect(screen.getByText('Organizando o codigo')).toBeInTheDocument()
    expect(screen.queryByText('fora do ar')).not.toBeInTheDocument()
  })
})
