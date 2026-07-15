import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { LeafDoodle } from '../components/Doodles.jsx'

// vitrine de posts. busca a lista na api ao montar e mostra cada um como um
// cartaozinho de papel.
export default function HomePage() {
  const [posts, setPosts] = useState([])
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api
      .listPosts()
      .then(setPosts)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
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
          </li>
        ))}
      </ul>
    </section>
  )
}
