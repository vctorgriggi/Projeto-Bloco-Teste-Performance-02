import { useEffect, useState } from 'react'
import { api } from '../api.js'

// selos de disponibilidade das duas pecas externas de que a conversa depende.
//
// existem por causa da arquitetura distribuida: um pedaco do sistema pode estar fora do
// ar enquanto o resto funciona, e isso e uma informacao que o leitor merece ter antes
// de agir. o monolito responde esse diagnostico em /api/engagement/status, sempre com
// 200 -- o "caiu" vem no corpo.
//
// desde a quarta entrega sao dois selos, porque sao duas perguntas diferentes:
// - "conversa": o microsservico de engajamento responde? sem ele, nao da para LER a
//   conversa nem reagir.
// - "fila": o broker de mensagens responde? sem ele, nao da para ENVIAR um comentario.
// com a conversa fora e a fila de pe, comentar ainda funciona: o recado espera na fila.
export default function ServiceBadge() {
  const [status, setStatus] = useState(null)

  useEffect(() => {
    let ativo = true

    async function consultar() {
      try {
        const atual = await api.engagementStatus()
        if (ativo) setStatus(atual)
      } catch {
        // se nem o monolito responde, nao ha selo a mostrar: a pagina inteira ja vai
        // exibir o erro de conexao
        if (ativo) setStatus(null)
      }
    }

    consultar()
    const intervalo = setInterval(consultar, 20000)
    return () => {
      ativo = false
      clearInterval(intervalo)
    }
  }, [])

  if (!status) return null

  const tituloConversa = status.available
    ? `${status.service} respondendo (${status.registeredInstances} instância(s) registrada(s) na descoberta)`
    : `${status.service} não respondeu na última verificação`

  // eventos esperando no outbox sao normais por alguns segundos; parados, sao o sinal
  // de que o broker caiu e os eventos estao guardados esperando por ele
  const pendentes = status.pendingEvents || 0
  const tituloFila = status.brokerAvailable
    ? `broker de mensagens respondendo${pendentes ? ` · ${pendentes} evento(s) saindo do outbox` : ''}`
    : `broker de mensagens fora do ar${pendentes ? ` · ${pendentes} evento(s) guardado(s) no outbox esperando por ele` : ''}`

  return (
    <span className="service-badges">
      <span className={`service-badge${status.available ? '' : ' service-badge-down'}`} title={tituloConversa}>
        <span className="service-dot" aria-hidden="true" />
        {status.available ? 'conversa no ar' : 'conversa fora do ar'}
      </span>
      <span className={`service-badge${status.brokerAvailable ? '' : ' service-badge-down'}`} title={tituloFila}>
        <span className="service-dot" aria-hidden="true" />
        {status.brokerAvailable ? 'fila no ar' : 'fila fora do ar'}
        {pendentes > 0 && <span className="service-count">{pendentes}</span>}
      </span>
    </span>
  )
}
