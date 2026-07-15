import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api.js'

// formulario para escrever um post novo. o autor e escolhido entre os ja
// cadastrados, ja que o post precisa pertencer a alguem.
export default function NewPostPage() {
  const navigate = useNavigate()
  const [authors, setAuthors] = useState([])
  const [form, setForm] = useState({ title: '', content: '', authorId: '' })
  const [error, setError] = useState(null)

  useEffect(() => {
    api
      .listAuthors()
      .then(setAuthors)
      .catch((e) => setError(e.message))
  }, [])

  async function handleSubmit(event) {
    event.preventDefault()
    try {
      const created = await api.createPost({
        title: form.title,
        content: form.content,
        authorId: Number(form.authorId),
      })
      navigate(`/posts/${created.id}`)
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <section className="form-page">
      <h2>escrever um post</h2>
      <p className="muted">ele nasce como rascunho. voce publica quando quiser.</p>

      {error && <p className="error-note">{error}</p>}

      {authors.length === 0 ? (
        <p className="muted">cadastre um autor antes de escrever.</p>
      ) : (
        <form onSubmit={handleSubmit} className="stack-form">
          <label>
            titulo
            <input
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              required
            />
          </label>

          <label>
            autor
            <select
              value={form.authorId}
              onChange={(e) => setForm({ ...form, authorId: e.target.value })}
              required
            >
              <option value="" disabled>escolha um autor</option>
              {authors.map((a) => (
                <option key={a.id} value={a.id}>{a.name}</option>
              ))}
            </select>
          </label>

          <label>
            texto
            <textarea
              rows={8}
              value={form.content}
              onChange={(e) => setForm({ ...form, content: e.target.value })}
              required
            />
          </label>

          <button type="submit" className="btn">guardar rascunho</button>
        </form>
      )}
    </section>
  )
}
