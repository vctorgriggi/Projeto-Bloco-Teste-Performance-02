import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ServiceBadge from './ServiceBadge.jsx'
import { api } from '../api.js'

vi.mock('../api.js', () => ({ api: { engagementStatus: vi.fn() } }))

function status(parcial) {
  return {
    service: 'engagement-service',
    available: true,
    registeredInstances: 2,
    brokerAvailable: true,
    pendingEvents: 0,
    ...parcial,
  }
}

// dois selos, porque sao duas perguntas: a conversa (o microsservico) e a fila (o
// broker) podem cair uma sem a outra
describe('ServiceBadge', () => {
  beforeEach(() => vi.resetAllMocks())

  it('tudo no ar', async () => {
    api.engagementStatus.mockResolvedValue(status({}))

    render(<ServiceBadge />)

    expect(await screen.findByText('conversa no ar')).toBeInTheDocument()
    expect(screen.getByText('fila no ar')).toBeInTheDocument()
  })

  it('engajamento fora e broker de pe sao mostrados separados', async () => {
    api.engagementStatus.mockResolvedValue(status({ available: false }))

    render(<ServiceBadge />)

    expect(await screen.findByText('conversa fora do ar')).toBeInTheDocument()
    expect(screen.getByText('fila no ar')).toBeInTheDocument()
  })

  // com o broker fora, os eventos esperam no outbox, e o selo mostra quantos
  it('broker fora mostra os eventos guardados no outbox', async () => {
    api.engagementStatus.mockResolvedValue(status({ brokerAvailable: false, pendingEvents: 3 }))

    render(<ServiceBadge />)

    expect(await screen.findByText('fila fora do ar')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
  })

  it('se nem o monolito responder, nao ha selo', async () => {
    api.engagementStatus.mockRejectedValue(new Error('sem conexao'))

    const { container } = render(<ServiceBadge />)

    await new Promise((r) => setTimeout(r, 0))
    expect(container).toBeEmptyDOMElement()
  })
})
