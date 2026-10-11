import { useCallback, useEffect, useState } from 'react'
import type { CSSProperties } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Project, ProjectRole } from '../../types'
import { proyectosApi, SesionExpiradaError } from '../../api/proyectos'
import { clearTokens } from '../../api/tokenStorage'
import { CreateProjectModal } from './CreateProjectModal'
import './projects.css'

interface ProyectoListadoResponse {
  id: number
  nombre: string
  descripcion: string | null
  activo: boolean
  rol: ProjectRole
}

type FiltroEstado = 'ALL' | 'ACTIVE' | 'ARCHIVED'

export function ProjectList() {
  const [projects, setProjects] = useState<Project[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [nombre, setNombre] = useState('')
  const [filtroEstado, setFiltroEstado] = useState<FiltroEstado>('ALL')
  const navigate = useNavigate()

  const fetchProjects = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const activo = filtroEstado === 'ALL' ? undefined : filtroEstado === 'ACTIVE'
      const data: ProyectoListadoResponse[] = await proyectosApi.listar(nombre, activo)
      setProjects(data.map((project) => ({
        id: project.id.toString(),
        name: project.nombre,
        description: project.descripcion || 'Sin descripción todavía.',
        status: project.activo ? 'ACTIVE' : 'ARCHIVED',
        myRole: project.rol,
      })))
    } catch (err) {
      if (err instanceof SesionExpiradaError) {
        clearTokens()
        navigate('/login', { replace: true })
        return
      }
      setError(err instanceof Error ? err.message : 'No se pudieron cargar los proyectos.')
    } finally {
      setLoading(false)
    }
  }, [nombre, filtroEstado, navigate])

  useEffect(() => { void fetchProjects() }, [fetchProjects])

  async function handleArchive(projectId: string) {
    if (!window.confirm('¿Querés archivar este proyecto?')) return
    try {
      await proyectosApi.archivar(projectId)
      await fetchProjects()
    } catch (err) {
      window.alert(err instanceof Error ? err.message : 'No se pudo archivar el proyecto.')
    }
  }

  async function handleCreateProject(name: string, description: string) {
    try {
      await proyectosApi.crear(name, description)
      setShowCreateModal(false)
      await fetchProjects()
    } catch (err) {
      window.alert(err instanceof Error ? err.message : 'No se pudo crear el proyecto.')
    }
  }

  function openProject(project: Project) {
    navigate(`/projects/${project.id}`, { state: { project } })
  }

  return (
    <main className="workspace">
      <header className="workspace-bar">
        <button className="brand" type="button" onClick={() => navigate('/projects')}>
          <span className="brand__mark" aria-hidden="true">⌁</span>
          <span>Safe &amp; Sound</span>
        </button>
        <div className="workspace-bar__actions">
          <button className="workspace-bar__profile" type="button" onClick={() => navigate('/profile')}>Perfil</button>
          <span className="workspace-bar__status"><i /> Sesión protegida</span>
        </div>
      </header>

      <section className="projects-page">
        <div className="projects-hero">
          <div>
            <p className="eyebrow">Tu espacio creativo</p>
            <h1>Proyectos que suenan a vos.</h1>
            <p className="projects-hero__copy">Organizá ideas, colaboraciones y cada versión de tu música en un solo lugar.</p>
          </div>
          <button className="primary-action" type="button" onClick={() => setShowCreateModal(true)}>
            <span aria-hidden="true">＋</span> Nuevo proyecto
          </button>
        </div>

        <section className="project-toolbar" aria-label="Buscar y filtrar proyectos">
          <label className="search-field" htmlFor="buscar-proyecto">
            <span aria-hidden="true">⌕</span>
            <input id="buscar-proyecto" type="search" value={nombre} onChange={(event) => setNombre(event.target.value)} placeholder="Buscar proyectos" />
          </label>
          <label className="select-field" htmlFor="filtro-estado">
            <span>Estado</span>
            <select id="filtro-estado" value={filtroEstado} onChange={(event) => setFiltroEstado(event.target.value as FiltroEstado)}>
              <option value="ALL">Todos</option>
              <option value="ACTIVE">Activos</option>
              <option value="ARCHIVED">Archivados</option>
            </select>
          </label>
          <button className="quiet-action" type="button" onClick={() => { setNombre(''); setFiltroEstado('ALL') }}>Limpiar</button>
        </section>

        {loading && <div className="project-state"><span className="loading-orb" /> Cargando tus proyectos…</div>}
        {!loading && error && (
          <div className="project-state project-state--error">
            <strong>No se pudieron cargar los proyectos</strong>
            <span>{error}</span>
            <button className="quiet-action" type="button" onClick={() => void fetchProjects()}>Reintentar</button>
          </div>
        )}

        {!loading && !error && (
          <section className="project-grid" aria-label="Proyectos">
            {projects.length === 0 ? (
              <div className="project-empty">
                <span aria-hidden="true">♫</span>
                <h2>{nombre || filtroEstado !== 'ALL' ? 'No encontramos coincidencias' : 'Tu próximo proyecto empieza acá'}</h2>
                <p>{nombre || filtroEstado !== 'ALL' ? 'Probá con otro nombre o estado.' : 'Creá un proyecto para comenzar a colaborar.'}</p>
                {!nombre && filtroEstado === 'ALL' && <button className="primary-action" type="button" onClick={() => setShowCreateModal(true)}>Crear proyecto</button>}
              </div>
            ) : projects.map((project, index) => (
              <article className={`project-card ${project.status === 'ARCHIVED' ? 'project-card--archived' : ''}`} key={project.id} onClick={() => openProject(project)} tabIndex={0} onKeyDown={(event) => { if (event.key === 'Enter') openProject(project) }}>
                <div className="project-card__topline">
                  <span className="project-art" style={{ '--card-hue': `${(index * 58 + 270) % 360}` } as CSSProperties}>{project.name.charAt(0).toUpperCase()}</span>
                  <span className={`status-pill status-pill--${project.status.toLowerCase()}`}>{project.status === 'ACTIVE' ? 'Activo' : 'Archivado'}</span>
                </div>
                <div>
                  <h2>{project.name}</h2>
                  <p>{project.description}</p>
                </div>
                <footer className="project-card__footer">
                  <span className="role-chip"><span aria-hidden="true">◉</span> {roleLabel(project.myRole)}</span>
                  {project.myRole === 'OWNER' && project.status === 'ACTIVE' && (
                    <button className="archive-action" type="button" onClick={(event) => { event.stopPropagation(); void handleArchive(project.id) }}>Archivar</button>
                  )}
                </footer>
              </article>
            ))}
          </section>
        )}
      </section>

      {showCreateModal && <CreateProjectModal onClose={() => setShowCreateModal(false)} onCreate={handleCreateProject} />}
    </main>
  )
}

function roleLabel(role: ProjectRole) {
  return role === 'OWNER' ? 'Propietario' : role === 'COLLABORATOR' ? 'Colaborador' : 'Solo lectura'
}
