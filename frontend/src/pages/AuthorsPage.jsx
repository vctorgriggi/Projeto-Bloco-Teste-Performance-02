import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { LeafDoodle } from '../components/Doodles.jsx'

// lista os autores e permite cadastrar, editar e remover. o mesmo formulario
// serve para criar e editar: quando ha um editingId, ele entra em modo edicao.
export default function AuthorsPage() {
  const [authors, setAuthors] = useState([])
  const [form, setForm] = useState({ name: '', email: '', bio: '' })
  const [editingId, setEditingId] = useState(null)
  const [error, setError] = useState(null)

  function load() {
    api.listAuthors().then(setAuthors).catch((e) => setError(e.message))
  }

  useEffect(load, [])

  function resetForm() {
    setForm({ name: '', email: '', bio: '' })
    setEditingId(null)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    try {
      if (editingId) {
        await api.updateAuthor(editingId, form)
      } else {
        await api.createAuthor(form)
      }
      resetForm()
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  function handleEdit(author) {
    setForm({ name: author.name, email: author.email, bio: author.bio || '' })
    setEditingId(author.id)
    setError(null)
  }

  async function handleDelete(id) {
    if (!confirm('remover este autor?')) return
    setError(null)
    try {
      await api.deleteAuthor(id)
      if (editingId === id) resetForm()
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <section>
      <div className="section-head">
        <h2>quem escreve por aqui</h2>
        <LeafDoodle className="section-leaf" />
      </div>

      <ul className="author-list">
        {authors.map((a) => (
          <li key={a.id} className="author-card">
            <p className="author-name">{a.name}</p>
            <p className="author-bio">{a.bio}</p>
            <p className="author-email">{a.email}</p>
            <div className="card-actions">
              <button className="btn-link" onClick={() => handleEdit(a)}>editar</button>
              <button className="btn-link btn-link-danger" onClick={() => handleDelete(a.id)}>excluir</button>
            </div>
          </li>
        ))}
      </ul>

      <div className="form-page">
        <h3>{editingId ? 'editar autor' : 'entrar pra mesa'}</h3>
        {error && <p className="error-note">{error}</p>}
        <form onSubmit={handleSubmit} className="stack-form">
          <label>
            nome
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </label>
          <label>
            email
            <input
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              disabled={editingId !== null}
              required
            />
          </label>
          <label>
            bio
            <textarea value={form.bio} onChange={(e) => setForm({ ...form, bio: e.target.value })} />
          </label>
          <div className="form-actions">
            <button type="submit" className="btn">{editingId ? 'salvar' : 'cadastrar'}</button>
            {editingId && (
              <button type="button" className="btn btn-ghost" onClick={resetForm}>cancelar</button>
            )}
          </div>
        </form>
      </div>
    </section>
  )
}
