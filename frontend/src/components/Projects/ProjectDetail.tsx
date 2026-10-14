import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { proyectosApi, SesionExpiradaError } from '../../api/proyectos'
import { clearTokens } from '../../api/tokenStorage'
import type { Project } from '../../types'
import type { ProjectRole } from '../../types'
import { FileList } from '../Files/FileList'
import { EditProjectModal } from './EditProjectModal'
import './projects.css'

interface ProyectoListadoResponse {
  id: number
  rol: ProjectRole
}

interface ProyectoDetalleResponse {
  id: number
  nombre: string
  descripcion: string | null
  activo: boolean
}

function toProject(detail: ProyectoDetalleResponse, rol: ProjectRole): Project {
  return {
    id: String(detail.id),
    name: detail.nombre,
    description: detail.descripcion || 'Sin descripción todavía.',
    status: detail.activo ? 'ACTIVE' : 'ARCHIVED',
    myRole: rol,
  }
}

export function ProjectDetail() {
  const { projectId } = useParams<{ projectId: string }>()
  const navigate = useNavigate()
  const [project, setProject] = useState<Project | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showEditModal, setShowEditModal] = useState(false)

  useEffect(() => {
    let active = true

    async function loadProject() {
      if (!projectId) {
        if (active) setLoading(false)
        return
      }

      setLoading(true)
      setError(null)
      setProject(null)

      try {
        // Este endpoint solo incluye proyectos donde el usuario autenticado es miembro.
        const projects: ProyectoListadoResponse[] = await proyectosApi.listar()
        const membership = projects.find((item) => String(item.id) === projectId)

        if (!membership) return

        // El backend vuelve a validar la membresía para la consulta de detalle.
        const detail: ProyectoDetalleResponse = await proyectosApi.obtenerPorId(projectId)
        if (active) setProject(toProject(detail, membership.rol))
      } catch (err) {
        if (!active) return
        if (err instanceof SesionExpiradaError) {
          clearTokens()
          navigate('/login', { replace: true })
          return
        }
        setError(err instanceof Error ? err.message : 'No se pudo cargar el proyecto.')
      } finally {
        if (active) setLoading(false)
      }
    }

    void loadProject()
    return () => { active = false }
  }, [navigate, projectId])

  async function handleSaveEdit(_id: string, name: string, description: string) {
    if (!project) return

    try {
      const updated: ProyectoDetalleResponse = await proyectosApi.modificar(project.id, name, description)
      setProject(toProject(updated, project.myRole))
      setShowEditModal(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo modificar el proyecto.')
    }
  }

  async function handleArchive() {
    if (!project || !window.confirm('¿Querés archivar este proyecto?')) return

    try {
      await proyectosApi.archivar(project.id)
      navigate('/projects', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo archivar el proyecto.')
    }
  }

  if (loading) {
    return <main className="workspace"><div className="project-state"><span className="loading-orb" /> Cargando proyecto…</div></main>
  }

  if (error) {
    return <main className="workspace"><section className="project-not-found"><span aria-hidden="true">!</span><h1>No se pudo abrir el proyecto</h1><p>{error}</p><button className="primary-action" type="button" onClick={() => navigate('/projects')}>Ver mis proyectos</button></section></main>
  }

  if (!project) {
    return <main className="workspace"><section className="project-not-found"><span aria-hidden="true">♪</span><h1>Proyecto no encontrado</h1><p>No tenés acceso a este proyecto o ya no está disponible.</p><button className="primary-action" type="button" onClick={() => navigate('/projects')}>Ver mis proyectos</button></section></main>
  }

  return (
    <main className="workspace">
      <header className="workspace-bar">
        <button className="brand" type="button" onClick={() => navigate('/projects')}><span className="brand__mark" aria-hidden="true">⌁</span><span>Safe &amp; Sound</span></button>
        <div className="workspace-bar__actions">
          <button className="workspace-bar__profile" type="button" onClick={() => navigate('/profile')}>Perfil</button>
          <span className="workspace-bar__status"><i /> Proyecto privado</span>
        </div>
      </header>
      <section className="project-detail">
        <button className="back-link" type="button" onClick={() => navigate('/projects')}>← Volver a proyectos</button>
        <header className="project-detail__hero">
          <div className="project-detail__art" aria-hidden="true">{project.name.charAt(0).toUpperCase()}</div>
          <div className="project-detail__content">
            <p className="eyebrow">Proyecto musical</p>
            <h1>{project.name}</h1>
            <p>{project.description}</p>
            <div className="detail-badges">
              <span className={`status-pill status-pill--${project.status.toLowerCase()}`}>{project.status === 'ACTIVE' ? 'Activo' : 'Archivado'}</span>
              <span className="role-chip"><span aria-hidden="true">◉</span> {project.myRole === 'OWNER' ? 'Propietario' : project.myRole === 'COLLABORATOR' ? 'Colaborador' : 'Solo lectura'}</span>
            </div>
          </div>
          {project.myRole === 'OWNER' && project.status === 'ACTIVE' && (
            <div className="detail-actions">
              <button className="quiet-action" type="button" onClick={() => setShowEditModal(true)}>Editar proyecto</button>
              <button className="archive-action" type="button" onClick={() => void handleArchive()}>Archivar</button>
            </div>
          )}
        </header>
        <FileList projectId={project.id} userRole={project.myRole} />
      </section>
      {showEditModal && <EditProjectModal project={project} onClose={() => setShowEditModal(false)} onSave={(id, name, description) => void handleSaveEdit(id, name, description)} />}
    </main>
  )
}
