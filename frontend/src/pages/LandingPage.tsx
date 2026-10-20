
import { Link } from 'react-router-dom'
import { getAccessToken } from '../api/tokenStorage'
import './LandingPage.css'

export default function LandingPage() {
    const isAuthenticated = Boolean(getAccessToken())

    return (
        <div className="landing-page">
            <header className="landing-header">
                <Link to="/" className="landing-brand">
                    <span className="landing-brand-icon">♫</span>
                    Safe & Sound
                </Link>

                <nav className="landing-nav">
                    <a href="#features">Funcionalidades</a>

                    {isAuthenticated ? (
                        <Link to="/projects" className="landing-nav-button">
                            Mis proyectos
                        </Link>
                    ) : (
                        <>
                            <Link to="/login">Iniciar sesión</Link>
                            <Link to="/register" className="landing-nav-button">
                                Registrarse
                            </Link>
                        </>
                    )}
                </nav>
            </header>

            <main>
                <section className="landing-hero">
                    <div className="landing-hero-content">
                        <span className="landing-eyebrow">
                            TU ESPACIO PARA CREAR
                        </span>

                        <h1>
                            La música suena mejor
                            <span> cuando la creamos juntos.</span>
                        </h1>

                        <p>
                            Organizá tus proyectos musicales, compartí
                            archivos y colaborá con tu equipo desde
                            un solo lugar.
                        </p>

                        <div className="landing-actions">
                            <Link
                                to={isAuthenticated ? '/projects' : '/register'}
                                className="landing-primary-button"
                            >
                                {isAuthenticated
                                    ? 'Ir a mis proyectos'
                                    : 'Comenzar ahora'}
                                <span aria-hidden="true">→</span>
                            </Link>

                            <a href="#features" className="landing-secondary-button">
                                Descubrir más
                            </a>
                        </div>
                    </div>

                    <div className="landing-visual" aria-hidden="true">
                        <div className="landing-record">
                            <div className="landing-record-ring">
                                <div className="landing-record-center">
                                    <span>♫</span>
                                </div>
                            </div>
                        </div>
                        <span className="landing-floating-note note-one">♪</span>
                        <span className="landing-floating-note note-two">♫</span>
                    </div>
                </section>

                <section id="features" className="landing-features">
                    <div className="landing-section-heading">
                        <span className="landing-eyebrow">
                            TODO EN UN MISMO LUGAR
                        </span>
                        <h2>Creá, compartí y colaborá.</h2>
                        <p>
                            Las herramientas que necesitás para llevar
                            tus ideas musicales más lejos.
                        </p>
                    </div>

                    <div className="landing-feature-grid">
                        <article className="landing-feature-card">
                            <span className="landing-feature-icon">◈</span>
                            <h3>Proyectos musicales</h3>
                            <p>
                                Mantené tus ideas y producciones organizadas
                                en espacios de trabajo.
                            </p>
                        </article>

                        <article className="landing-feature-card">
                            <span className="landing-feature-icon">♫</span>
                            <h3>Archivos compartidos</h3>
                            <p>
                                Centralizá los archivos de tus proyectos
                                y trabajá con tu equipo.
                            </p>
                        </article>

                        <article className="landing-feature-card">
                            <span className="landing-feature-icon">♧</span>
                            <h3>Colaboración segura</h3>
                            <p>
                                Administrá integrantes y permisos para
                                controlar quién accede a cada proyecto.
                            </p>
                        </article>
                    </div>
                </section>
            </main>

            <footer className="landing-footer">
                <span>♫ Safe & Sound</span>
                <span>Un espacio para crear música juntos.</span>
            </footer>
        </div>
    )
}

