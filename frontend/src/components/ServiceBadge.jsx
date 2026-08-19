import { useEffect, useState } from 'react'
import { api } from '../api.js'

// selo de disponibilidade do microsservico de engajamento.
//
// existe por causa da arquitetura distribuida: um pedaco do sistema pode estar fora do
// ar enquanto o resto funciona, e isso e uma informacao que o leitor merece ter antes
// de escrever um comentario e receber um erro. o monolito responde esse diagnostico em
// /api/engagement/status, sempre com 200 -- o "caiu" vem no corpo.
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

  const titulo = status.available
    ? `${status.service} respondendo (${status.registeredInstances} instância(s) registrada(s) na descoberta)`
    : `${status.service} não respondeu na última verificação`

  return (
    <span className={`service-badge${status.available ? '' : ' service-badge-down'}`} title={titulo}>
      <span className="service-dot" aria-hidden="true" />
      {status.available ? 'conversa e reações no ar' : 'conversa e reações fora do ar'}
    </span>
  )
}
