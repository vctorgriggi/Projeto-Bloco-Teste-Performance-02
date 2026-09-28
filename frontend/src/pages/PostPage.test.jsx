import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import PostPage from './PostPage.jsx'
import { api } from '../api.js'

vi.mock('../api.js', () => ({
  api: {
    getPost: vi.fn(),
    listComments: vi.fn(),
    addComment: vi.fn(),
    reactionSummary: vi.fn(),
  },
}))

const post = { id: 1, title: 'Um post', content: 'texto do post', authorName: 'Ana', status: 'PUBLISHED' }
const envio = { submissionId: 'envio-1', postId: 1, authorName: 'Carla', content: 'na fila ainda', status: 'PENDING' }

function renderizar() {
  return render(
    <MemoryRouter initialEntries={['/posts/1']}>
      <Routes>
        <Route path="/posts/:id" element={<PostPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

// a barra de reacoes tambem pede o nome do leitor, entao a busca fica restrita ao
// formulario de comentario
function formulario() {
  return within(screen.getByRole('button', { name: 'comentar' }).closest('form'))
}

function comentar(nome, texto) {
  fireEvent.change(formulario().getByPlaceholderText('seu nome'), { target: { value: nome } })
  fireEvent.change(formulario().getByPlaceholderText('deixe um recado...'), { target: { value: texto } })
  fireEvent.click(formulario().getByRole('button', { name: 'comentar' }))
}

// a pagina do post depois da quarta entrega: o comentario e aceito (202) e so depois
// gravado pelo engajamento, e a tela mostra esse intervalo em vez de esconde-lo
describe('PostPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    api.getPost.mockResolvedValue(post)
    api.reactionSummary.mockResolvedValue({ postId: 1, total: 0, counts: {}, mine: [] })
  })

  afterEach(() => vi.useRealTimers())

  it('o recado enviado aparece "na fila" e vira comentario quando o engajamento grava', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    api.listComments.mockResolvedValueOnce([])
    api.addComment.mockResolvedValue(envio)
    renderizar()
    await screen.findByText('Um post')

    comentar('Carla', 'na fila ainda')

    expect(await screen.findByText('na fila')).toBeInTheDocument()
    expect(screen.getByText('na fila ainda')).toBeInTheDocument()

    // o engajamento processou a mensagem: a listagem traz o comentario com o mesmo id
    api.listComments.mockResolvedValue([
      { id: 9, postId: 1, authorName: 'Carla', content: 'na fila ainda', submissionId: 'envio-1' },
    ])
    await act(async () => {
      await vi.advanceTimersByTimeAsync(3500)
    })

    await waitFor(() => expect(screen.queryByText('na fila')).not.toBeInTheDocument())
    expect(screen.getByText('na fila ainda')).toBeInTheDocument()
  })

  // com o engajamento fora, a conversa nao carrega -- mas o formulario continua, porque
  // o recado agora tem onde esperar (a fila). na terceira entrega ele saia da tela
  it('com o engajamento fora do ar, o post continua legivel e o formulario continua', async () => {
    api.listComments.mockRejectedValue(new Error('O servico de engajamento esta indisponivel'))

    renderizar()

    expect(await screen.findByText('texto do post')).toBeInTheDocument()
    expect(await screen.findByText(/O servico de engajamento esta indisponivel/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'comentar' })).toBeInTheDocument()
  })

  // com o broker fora, o envio falha e o texto fica onde o leitor escreveu
  it('se o envio falhar, o texto continua no formulario', async () => {
    api.listComments.mockResolvedValue([])
    api.addComment.mockRejectedValue(new Error('A fila de mensagens esta indisponivel'))
    renderizar()
    await screen.findByText('Um post')

    comentar('Carla', 'nao pode se perder')

    expect(await screen.findByText('A fila de mensagens esta indisponivel')).toBeInTheDocument()
    expect(formulario().getByPlaceholderText('deixe um recado...')).toHaveValue('nao pode se perder')
    expect(screen.queryByText('na fila')).not.toBeInTheDocument()
  })
})
