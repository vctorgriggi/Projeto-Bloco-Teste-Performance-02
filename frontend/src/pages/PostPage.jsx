import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { api } from '../api.js'
import { Squiggle } from '../components/Doodles.jsx'

// rotulos amigaveis para os metadados que o back-end devolve do envers
const REVISION_LABELS = { INSERT: 'criado', UPDATE: 'editado', DELETE: 'removido' }
const STATUS_LABELS = { PUBLISHED: 'publicado', DRAFT: 'rascunho' }

function formatInstant(instant) {
  if (!instant) return ''
  return new Date(instant).toLocaleString('pt-BR')
}

// linha do tempo das revisoes do post, vinda de /api/posts/{id}/history
function PostHistory({ revisions }) {
  if (!revisions) return <p className="muted">carregando histórico...</p>
  if (revisions.length === 0) return <p className="muted">sem histórico ainda.</p>

  return (
    <section className="history">
      <h3>histórico ({revisions.length})</h3>
      <ol className="history-list">
        {revisions.map((rev) => (
          <li key={rev.revisionNumber} className="history-entry">
            <div className="history-head">
              <span className={`tag tag-${rev.status?.toLowerCase()}`}>
                {REVISION_LABELS[rev.revisionType] || rev.revisionType}
              </span>
              <span className="history-when">{formatInstant(rev.revisionInstant)}</span>
            </div>
            <p className="history-title">{rev.title}</p>
            <p className="muted">estado: {STATUS_LABELS[rev.status] || rev.status}</p>
          </li>
        ))}
      </ol>
    </section>
  )
}

// pagina de leitura de um post: o texto completo, editar/publicar/apagar o post
// e a conversa nos comentarios, com opcao de remover cada comentario.
export default function PostPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [post, setPost] = useState(null)
  const [comments, setComments] = useState([])
  const [error, setError] = useState(null)
  const [form, setForm] = useState({ authorName: '', content: '' })

  // modo edicao do proprio post (titulo e texto)
  const [editing, setEditing] = useState(false)
  const [editForm, setEditForm] = useState({ title: '', content: '' })

  // historico de revisoes do post, carregado sob demanda ao abrir a linha do tempo
  const [history, setHistory] = useState(null)
  const [showHistory, setShowHistory] = useState(false)

  function load() {
    Promise.all([api.getPost(id), api.listComments(id)])
      .then(([p, c]) => {
        setPost(p)
        setComments(c)
        setError(null)
      })
      .catch((e) => setError(e.message))
  }

  useEffect(load, [id])

  function startEditing() {
    setEditForm({ title: post.title, content: post.content })
    setEditing(true)
    setError(null)
  }

  // abre a linha do tempo ja mostrando o estado de carregando, e so entao busca
  // as revisoes; se falhar, fecha o painel e deixa o erro aparecer no aviso
  async function loadHistory() {
    setShowHistory(true)
    setHistory(null)
    setError(null)
    try {
      setHistory(await api.postHistory(id))
    } catch (e) {
      setShowHistory(false)
      setError(e.message)
    }
  }

  function toggleHistory() {
    if (showHistory) {
      setShowHistory(false)
    } else {
      loadHistory()
    }
  }

  async function handleSaveEdit(event) {
    event.preventDefault()
    try {
      // o put exige o authorId; reaproveitamos o autor que o post ja tem
      await api.updatePost(id, { ...editForm, authorId: post.authorId })
      setEditing(false)
      load()
      if (showHistory) loadHistory() // a edicao gerou uma revisao nova
    } catch (e) {
      setError(e.message)
    }
  }

  async function handlePublish() {
    try {
      await api.publishPost(id)
      load()
      if (showHistory) loadHistory()
    } catch (e) {
      setError(e.message)
    }
  }

  async function handleAddComment(event) {
    event.preventDefault()
    try {
      await api.addComment(id, form)
      setForm({ authorName: '', content: '' })
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  async function handleDeleteComment(commentId) {
    if (!confirm('apagar este comentario?')) return
    try {
      await api.deleteComment(commentId)
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  async function handleDeletePost() {
    if (!confirm('apagar este post?')) return
    try {
      await api.deletePost(id)
      navigate('/')
    } catch (e) {
      setError(e.message)
    }
  }

  // sem post ainda: ou falhou o carregamento inicial, ou esta abrindo
  if (!post) {
    return error
      ? <p className="error-note">{error}</p>
      : <p className="muted">abrindo a pagina...</p>
  }

  return (
    <article className="post-detail">
      <Link to="/" className="back-link">← voltar pra estante</Link>

      {/* erro de uma acao (publicar, editar, historico) sem derrubar a leitura */}
      {error && <p className="error-note">{error}</p>}

      <span className={`tag tag-${post.status.toLowerCase()}`}>
        {post.status === 'PUBLISHED' ? 'publicado' : 'rascunho'}
      </span>

      {editing ? (
        <form onSubmit={handleSaveEdit} className="stack-form edit-form">
          <label>
            titulo
            <input
              value={editForm.title}
              onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
              required
            />
          </label>
          <label>
            texto
            <textarea
              rows={6}
              value={editForm.content}
              onChange={(e) => setEditForm({ ...editForm, content: e.target.value })}
              required
            />
          </label>
          <div className="form-actions">
            <button type="submit" className="btn">salvar</button>
            <button type="button" className="btn btn-ghost" onClick={() => setEditing(false)}>cancelar</button>
          </div>
        </form>
      ) : (
        <>
          <h2>{post.title}</h2>
          <p className="post-meta">por {post.authorName}</p>
          <Squiggle className="inline-squiggle" />
          <p className="post-body">{post.content}</p>

          <div className="post-actions">
            {post.status !== 'PUBLISHED' && (
              <button onClick={handlePublish} className="btn">publicar</button>
            )}
            <button onClick={startEditing} className="btn btn-ghost">editar</button>
            <button onClick={handleDeletePost} className="btn btn-ghost">apagar</button>
            <button onClick={toggleHistory} className="btn btn-ghost">
              {showHistory ? 'ocultar histórico' : 'histórico'}
            </button>
          </div>

          {showHistory && <PostHistory revisions={history} />}
        </>
      )}

      <section className="comments">
        <h3>conversa ({comments.length})</h3>

        <ul className="comment-list">
          {comments.map((c) => (
            <li key={c.id} className="comment">
              <div className="comment-head">
                <p className="comment-author">{c.authorName}</p>
                <button className="btn-link btn-link-danger" onClick={() => handleDeleteComment(c.id)}>
                  apagar
                </button>
              </div>
              <p>{c.content}</p>
            </li>
          ))}
          {comments.length === 0 && <p className="muted">seja o primeiro a comentar.</p>}
        </ul>

        <form onSubmit={handleAddComment} className="comment-form">
          <input
            placeholder="seu nome"
            value={form.authorName}
            onChange={(e) => setForm({ ...form, authorName: e.target.value })}
            required
          />
          <textarea
            placeholder="deixe um recado..."
            value={form.content}
            onChange={(e) => setForm({ ...form, content: e.target.value })}
            required
          />
          <button type="submit" className="btn">comentar</button>
        </form>
      </section>
    </article>
  )
}
