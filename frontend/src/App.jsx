import { NavLink, Route, Routes } from 'react-router-dom'
import HomePage from './pages/HomePage.jsx'
import PostPage from './pages/PostPage.jsx'
import NewPostPage from './pages/NewPostPage.jsx'
import AuthorsPage from './pages/AuthorsPage.jsx'
import { CoffeeDoodle, Squiggle } from './components/Doodles.jsx'
import ServiceBadge from './components/ServiceBadge.jsx'

// layout geral: um cabecalho enxuto com a "marca" da cafeteria, a navegacao e
// o rodape. o conteudo de cada rota entra no <main>.
export default function App() {
  return (
    <div className="page">
      <header className="site-header">
        <div className="brand">
          <CoffeeDoodle className="brand-icon" />
          <div>
            <h1 className="brand-name">kissaten</h1>
            <p className="brand-tagline">um cantinho de leitura, devagar</p>
          </div>
        </div>

        <nav className="site-nav">
          <NavLink to="/" end>posts</NavLink>
          <NavLink to="/escrever">escrever</NavLink>
          <NavLink to="/autores">autores</NavLink>
          {/* o sistema tem pecas que podem cair sozinhas; os selos avisam */}
          <ServiceBadge />
        </nav>
        <Squiggle className="header-squiggle" />
      </header>

      <main className="site-main">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/posts/:id" element={<PostPage />} />
          <Route path="/escrever" element={<NewPostPage />} />
          <Route path="/autores" element={<AuthorsPage />} />
        </Routes>
      </main>

      <footer className="site-footer">
        <Squiggle className="footer-squiggle" />
        <p>feito devagar, com cafe ~ projeto bloco, quarta entrega</p>
      </footer>
    </div>
  )
}
