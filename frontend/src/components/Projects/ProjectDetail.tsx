import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { proyectosApi, SesionExpiradaError } from '../../api/proyectos'
import { getAccessToken } from '../../api/tokenStorage' // <-- IMPORTACIÓN CORREGIDA
import { FileList } from '../Files/FileList'
import { EditProjectModal } from './EditProjectModal'
import './projects.css'

interface ProyectoBackend {
  id: number
  nombre: string
  descripcion: string
  activo: boolean
  creadoPor: { id: number; username: string }
}

export function ProjectDetail() {
  const { projectId } = useParams<{ projectId: string }>()
  const navigate = useNavigate()

  const [project, setProject] = useState<ProyectoBackend | null>(null)
  const [userRole, setUserRole] = useState<'OWNER' | 'COLLABORATOR' | 'VIEWER'>('VIEWER')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showEditModal, setShowEditModal] = useState(false)

  // Estados para el modal de reautenticación
  const [showReauthModal, setShowReauthModal] = useState(false)
  const [reauthPassword, setReauthPassword] = useState('')
  const [pendingAction, setPendingAction] = useState<(() => void) | null>(null)

  const getRoleLabel = (role: 'OWNER' | 'COLLABORATOR' | 'VIEWER') => {
    if (role === 'OWNER') return 'Propietario'
    if (role === 'COLLABORATOR') return 'Colaborador'
    return 'Solo lectura'
  }

  useEffect(() => {
    if (!projectId) return

    setIsLoading(true)
    setError(null)

    proyectosApi.obtenerPorId(projectId)
      .then((data: ProyectoBackend) => {
        setProject(data)
        return proyectosApi.listar()
      })
      .then((listaProyectos) => {
        const miProyecto = listaProyectos.find((p: any) => p.id.toString() === projectId)
        if (miProyecto && miProyecto.rol) {
          setUserRole(miProyecto.rol)
        } else {
          setUserRole('VIEWER')
        }
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

  // Función para abrir el modal y guardar la acción a ejecutar
  function solicitarReautenticacion(accion: () => void) {
    setPendingAction(() => accion)
    setShowReauthModal(true)
    setReauthPassword('')
  }

  // Función que valida la contraseña con el backend y ejecuta la acción si es correcta
  function confirmarReautenticacion() {
    if (!pendingAction) return

    // Usamos la función oficial del equipo para obtener el token
    const token = getAccessToken()

    fetch('http://localhost:8080/auth/reautenticar', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ password: reauthPassword })
    })
    .then(response => {
      if (response.ok) {
        pendingAction() // Ejecuta la acción original (archivar o modificar)
        setShowReauthModal(false)
      } else {
        alert('Contraseña incorrecta. Operación cancelada.')
      }
    })
    .catch(err => {
      console.error(err)
      alert('Error al verificar la contraseña. Inténtalo de nuevo.')
    })
  }

  function handleSaveEdit(id: string, name: string, description: string) {
    if (!project) return
    
    setShowEditModal(false)

    solicitarReautenticacion(() => {
      const url = new URL(`http://localhost:8080/proyectos/${project.id}`)
      url.searchParams.append('nombre', name)
      if (description) url.searchParams.append('descripcion', description)
      url.searchParams.append('passwordConfirmacion', reauthPassword)

      const token = getAccessToken() // <-- TOKEN CORREGIDO

      fetch(url.toString(), {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      .then(response => {
        if (response.ok) {
          setProject({ ...project, nombre: name, descripcion: description })
        } else {
          throw new Error('Error al modificar el proyecto')
        }
      })
      .catch((err) => {
        alert(err instanceof Error ? err.message : 'Error al modificar el proyecto')
      })
    })
  }

  function handleArchive() {
    if (!project || !window.confirm('¿Querés archivar este proyecto?')) return

    solicitarReautenticacion(() => {
      const url = new URL(`http://localhost:8080/proyectos/${project.id}`)
      url.searchParams.append('passwordConfirmacion', reauthPassword)
      const token = getAccessToken() // <-- TOKEN CORREGIDO

      fetch(url.toString(), {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      .then(response => {
        if (response.ok) {
          setProject({ ...project, activo: false })
        } else {
          throw new Error('Error al archivar el proyecto')
        }
      })
      .catch((err) => {
        alert(err instanceof Error ? err.message : 'Error al archivar el proyecto')
      })
    })
  }

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

  const isOwner = userRole === 'OWNER'

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
                {getRoleLabel(userRole)}
              </span>
            </div>
          </div>

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

        <FileList projectId={project.id.toString()} userRole={userRole} />
      </section>

      {showEditModal && project && (
        <EditProjectModal
          project={{ 
            id: project.id.toString(), 
            name: project.nombre, 
            description: project.descripcion,
            status: project.activo ? 'ACTIVE' : 'ARCHIVED',
            myRole: userRole 
          }}
          onClose={() => setShowEditModal(false)}
          onSave={handleSaveEdit}
        />
      )}

      {/* Modal de Reautenticación para operaciones de alto riesgo */}
      {showReauthModal && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
        }}>
          <div style={{
            backgroundColor: 'white', padding: '2rem', borderRadius: '8px', width: '400px', maxWidth: '90%',
            boxShadow: '0 4px 6px rgba(0,0,0,0.1)'
          }}>
            <h2 style={{ marginTop: 0, color: '#333' }}>Confirmar identidad</h2>
            <p style={{ color: '#666', marginBottom: '1rem' }}>
              Para realizar esta operación de alto riesgo, ingresá tu contraseña:
            </p>
            <input
              type="password"
              value={reauthPassword}
              onChange={(e) => setReauthPassword(e.target.value)}
              placeholder="Tu contraseña"
              style={{ width: '100%', padding: '0.75rem', marginBottom: '1.5rem', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box', fontSize: '1rem' }}
              autoFocus
              onKeyDown={(e) => e.key === 'Enter' && confirmarReautenticacion()}
            />
            <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
              <button 
                type="button" 
                onClick={() => setShowReauthModal(false)}
                style={{ padding: '0.5rem 1rem', cursor: 'pointer', border: '1px solid #ccc', backgroundColor: 'white', borderRadius: '4px' }}
              >
                Cancelar
              </button>
              <button 
                type="button" 
                onClick={confirmarReautenticacion}
                style={{ padding: '0.5rem 1rem', cursor: 'pointer', border: 'none', backgroundColor: '#007bff', color: 'white', borderRadius: '4px', fontWeight: 'bold' }}
              >
                Confirmar
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  )
}