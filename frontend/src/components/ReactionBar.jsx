import { useCallback, useEffect, useState } from 'react'
import { api } from '../api.js'
import { BulbDoodle, CupDoodle, HeartDoodle } from './Doodles.jsx'

// componente novo da terceira entrega: a barra de reacoes de um post. e a interface
// da capacidade que nasceu dentro do microsservico de engajamento.
//
// os tipos aparecem aqui porque e a camada de apresentacao que decide como cada um se
// chama e se desenha na tela; quem decide quais tipos existem e o microsservico, e ele
// devolve todos no resumo, inclusive os zerados. se um tipo novo aparecer la e nao
// estiver mapeado abaixo, ele e ignorado na tela em vez de quebrar a pagina.
const REACTIONS = [
  { type: 'CORACAO', label: 'amei', Icon: HeartDoodle },
  { type: 'CAFE', label: 'li com calma', Icon: CupDoodle },
  { type: 'IDEIA', label: 'me fez pensar', Icon: BulbDoodle },
]

// nao ha login no blog. o leitor se identifica por um nome, guardado no navegador,
// e e esse nome que o servico usa para saber que ele ja reagiu.
const READER_KEY = 'kissaten:leitor'

function leitorGuardado() {
  return localStorage.getItem(READER_KEY) || ''
}

export default function ReactionBar({ postId }) {
  // dois estados para o nome: o que esta sendo digitado e o que ja valeu como
  // consulta. sem essa separacao, cada tecla digitada viraria uma requisicao de
  // resumo ao microsservico.
  const [readerInput, setReaderInput] = useState(leitorGuardado)
  const [reader, setReader] = useState(leitorGuardado)

  const [summary, setSummary] = useState(null)
  const [error, setError] = useState(null)
  const [pendingType, setPendingType] = useState(null)

  // confirma o nome digitado depois de uma pausa curta, e guarda no navegador
  useEffect(() => {
    const espera = setTimeout(() => {
      const nome = readerInput.trim()
      setReader(nome)
      localStorage.setItem(READER_KEY, nome)
    }, 500)
    return () => clearTimeout(espera)
  }, [readerInput])

  const loadSummary = useCallback(async () => {
    try {
      setSummary(await api.reactionSummary(postId, reader))
      setError(null)
    } catch (e) {
      // o resumo e o unico ponto onde a queda do engajamento aparece aqui; a barra
      // mostra o aviso em vez de fingir que o post nao tem reacao nenhuma
      setSummary(null)
      setError(e.message)
    }
  }, [postId, reader])

  useEffect(() => {
    loadSummary()
  }, [loadSummary])

  // o mesmo clique reage e desfaz: se o tipo ja esta em "mine", a acao e remover.
  // le o nome do campo, e nao do estado confirmado, para funcionar tambem no clique
  // que vem logo depois de digitar.
  async function toggle(type) {
    const leitor = readerInput.trim()
    if (!leitor) {
      setError('escreva seu nome ali embaixo para poder reagir')
      return
    }

    setPendingType(type)
    setError(null)
    try {
      const jaReagiu = summary?.mine?.includes(type)
      setSummary(
        jaReagiu
          ? await api.undoReaction(postId, type, leitor)
          : await api.react(postId, { readerName: leitor, type }),
      )
    } catch (e) {
      setError(e.message)
      loadSummary() // reconcilia com o servidor se o estado local ficou defasado
    } finally {
      setPendingType(null)
    }
  }

  return (
    <section className="reactions">
      <div className="reactions-head">
        <h3>o que ficou</h3>
        {summary && <span className="reactions-total">{summary.total} reação(ões)</span>}
      </div>

      {error && <p className="error-note">{error}</p>}

      <div className="reaction-buttons">
        {REACTIONS.map(({ type, label, Icon }) => {
          const marcada = summary?.mine?.includes(type) ?? false
          return (
            <button
              key={type}
              type="button"
              className={`reaction-btn${marcada ? ' reaction-btn-on' : ''}`}
              onClick={() => toggle(type)}
              disabled={pendingType === type || summary === null}
              aria-pressed={marcada}
              title={marcada ? `desfazer "${label}"` : label}
            >
              <Icon className="reaction-icon" />
              <span className="reaction-label">{label}</span>
              <span className="reaction-count">{summary?.counts?.[type] ?? 0}</span>
            </button>
          )
        })}
      </div>

      <label className="reader-field">
        quem está lendo?
        <input
          value={readerInput}
          placeholder="seu nome"
          maxLength={60}
          onChange={(e) => setReaderInput(e.target.value)}
        />
      </label>
    </section>
  )
}
