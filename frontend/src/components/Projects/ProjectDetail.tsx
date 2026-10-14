import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { proyectosApi, SesionExpiradaError } from '../../api/proyectos'
import { FileList } from '../Files/FileList'
import { EditProjectModal } from './EditProjectModal'
import './projects.css'

// Definimos la interfaz según lo que devuelve el backend
interface ProyectoBackend {
  id: number
  nombre: string
  descripcion: string
  activo: boolean
  creadoPor: { id: number; username: string }
  miRol: 'OWNER' | 'COLLABORATOR' | 'VIEWER'
}

export function ProjectDetail() {
  const { projectId } = useParams<{ projectId: string }>()
  const navigate = useNavigate()

  const [project, setProject] = useState<ProyectoBackend | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showEditModal, setShowEditModal] = useState(false)

  // Helper para obtener el texto del rol (corrige el nested ternary)
  const getRoleLabel = (role: 'OWNER' | 'COLLABORATOR' | 'VIEWER') => {
    if (role === 'OWNER') return 'Propietario'
    if (role === 'COLLABORATOR') return 'Colaborador'
    return 'Solo lectura'
  }

  // Cargar el proyecto real desde el backend
  useEffect(() => {
    if (!projectId) return

    setIsLoading(true)
    setError(null)

    proyectosApi
      .obtenerPorId(projectId)
      .then((data: ProyectoBackend) => {
        setProject(data)
      })
      .catch((err) => {
        if (err instanceof SesionExpiradaError) {
          navigate('/login', { state: { from: `/proyectos/${projectId}` } })
        } else if (err.message.includes('403') || err.message.includes('404')) {
          setError('No tienes permisos para ver este proyecto o no existe.')
        } else {
          setError('Error al cargar el proyecto. Inténtalo de nuevo.')
        }
      })
      .finally(() => {
        setIsLoading(false)
      })
  }, [projectId, navigate])

  function handleSaveEdit(id: string, name: string, description: string) {
    if (!project) return

    // Llamada real a la API para modificar
    proyectosApi
      .modificar(project.id.toString(), name, description)
      .then(() => {
        setProject({ ...project, nombre: name, descripcion: description })
        setShowEditModal(false)
      })
      .catch((err) => {
        alert(err instanceof Error ? err.message : 'Error al modificar el proyecto')
      })
  }

  function handleArchive() {
    if (!project || !window.confirm('¿Querés archivar este proyecto?')) return

    proyectosApi
      .archivar(project.id.toString())
      .then(() => {
        setProject({ ...project, activo: false })
      })
      .catch((err) => {
        alert(err instanceof Error ? err.message : 'Error al archivar el proyecto')
      })
  }

  // Estado de carga
  if (isLoading) {
    return (
      <main className="workspace">
        <section className="project-not-found">
          <span aria-hidden="true">⌛</span>
          <h1>Cargando proyecto...</h1>
        </section>
      </main>
    )
  }

  // Estado de error (ej: 403 Forbidden o 404 Not Found)
  if (error || !project) {
    return (
      <main className="workspace">
        <section className="project-not-found">
          <span aria-hidden="true">⛔</span>
          <h1>Acceso denegado o proyecto no encontrado</h1>
          <p>{error || 'No tienes los permisos necesarios para ver este proyecto.'}</p>
          <button className="primary-action" type="button" onClick={() => navigate('/projects')}>
            Volver a mis proyectos
          </button>
        </section>
      </main>
    )
  }

  // Determinar si el usuario es propietario (se eliminó la variable canModify que no se usaba)
  const isOwner = project.miRol === 'OWNER'

  return (
    <main className="workspace">
      <header className="workspace-bar">
        <button className="brand" type="button" onClick={() => navigate('/projects')}>
          <span className="brand__mark" aria-hidden="true">⌁</span>
          <span>Safe &amp; Sound</span>
        </button>
        <div className="workspace-bar__actions">
          <button className="workspace-bar__profile" type="button" onClick={() => navigate('/profile')}>Perfil</button>
          <span className="workspace-bar__status">
            <i /> {project.activo ? 'Proyecto activo' : 'Proyecto archivado'}
          </span>
        </div>
      </header>

      <section className="project-detail">
        <button className="back-link" type="button" onClick={() => navigate('/projects')}>
          ← Volver a proyectos
        </button>

        <header className="project-detail__hero">
          <div className="project-detail__art" aria-hidden="true">
            {project.nombre.charAt(0).toUpperCase()}
          </div>
          <div className="project-detail__content">
            <p className="eyebrow">Proyecto musical</p>
            <h1>{project.nombre}</h1>
            <p>{project.descripcion}</p>
            <div className="detail-badges">
              <span className={`status-pill status-pill--${project.activo ? 'active' : 'archived'}`}>
                {project.activo ? 'Activo' : 'Archivado'}
              </span>
              <span className="role-chip">
                <span aria-hidden="true">◉</span>{' '}
                {getRoleLabel(project.miRol)}
              </span>
            </div>
          </div>

          {/* Solo mostrar acciones si es OWNER y está activo */}
          {isOwner && project.activo && (
            <div className="detail-actions">
              <button className="quiet-action" type="button" onClick={() => setShowEditModal(true)}>
                Editar proyecto
              </button>
              <button className="archive-action" type="button" onClick={handleArchive}>
                Archivar
              </button>
            </div>
          )}
        </header>

        {/* FileList ya tiene su propia lógica interna para ocultar botones según userRole */}
        <FileList projectId={project.id.toString()} userRole={project.miRol} />
      </section>

      {showEditModal && (
        <EditProjectModal
          project={{ id: project.id.toString(), name: project.nombre, description: project.descripcion }}
          onClose={() => setShowEditModal(false)}
          onSave={handleSaveEdit}
        />
      )}
    </main>
  )
}