import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { LeafDoodle } from '../components/Doodles.jsx'

// "3 recados · 1 reação": o total de conversa de um post, em portugues de gente
function engagementLabel(counter) {
  if (!counter || (counter.comments === 0 && counter.reactions === 0)) return null
  const recados = `${counter.comments} ${counter.comments === 1 ? 'recado' : 'recados'}`
  const reacoes = `${counter.reactions} ${counter.reactions === 1 ? 'reação' : 'reações'}`
  return `${recados} · ${reacoes}`
}

// vitrine de posts. busca a lista na api ao montar e mostra cada um como um
// cartaozinho de papel, com os totais de conversa e reacoes de cada post.
//
// os totais vem de /engagement/counters, numa chamada so para a estante inteira. o
// monolito responde da copia que mantem a partir dos eventos do engajamento, entao a
// estante nao faz uma chamada por post e continua mostrando os numeros com o
// microsservico fora do ar. se ate essa chamada falhar, os cartoes aparecem sem os
// numeros -- eles sao enfeite, e nao podem derrubar a estante.
export default function HomePage() {
  const [posts, setPosts] = useState([])
  const [counters, setCounters] = useState({})
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api
      .listPosts()
      .then(setPosts)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))

    api
      .engagementCounters()
      .then((lista) => setCounters(Object.fromEntries(lista.map((c) => [c.postId, c]))))
      .catch(() => setCounters({}))
  }, [])

  if (loading) return <p className="muted">preparando o cafe...</p>
  if (error) return <p className="error-note">{error}</p>

  return (
    <section>
      <div className="section-head">
        <h2>na estante de hoje</h2>
        <LeafDoodle className="section-leaf" />
      </div>

      {posts.length === 0 && <p className="muted">ainda nao ha posts. que tal escrever o primeiro?</p>}

      <ul className="post-list">
        {posts.map((post) => (
          <li key={post.id} className="post-card">
            <span className={`tag tag-${post.status.toLowerCase()}`}>
              {post.status === 'PUBLISHED' ? 'publicado' : 'rascunho'}
            </span>
            <h3>
              <Link to={`/posts/${post.id}`}>{post.title}</Link>
            </h3>
            <p className="post-excerpt">{post.content}</p>
            <p className="post-meta">por {post.authorName}</p>
            {engagementLabel(counters[post.id]) && (
              <p className="post-engagement">{engagementLabel(counters[post.id])}</p>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}
