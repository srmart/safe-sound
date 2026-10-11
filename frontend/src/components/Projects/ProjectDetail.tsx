import { useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { mockProjects } from '../../pruebas/data'
import type { Project } from '../../types'
import { FileList } from '../Files/FileList'
import { EditProjectModal } from './EditProjectModal'
import './projects.css'

interface DetailLocationState { project?: Project }

export function ProjectDetail() {
  const { projectId } = useParams<{ projectId: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as DetailLocationState | null
  const [project, setProject] = useState<Project | undefined>(state?.project ?? mockProjects.find((item) => item.id === projectId))
  const [showEditModal, setShowEditModal] = useState(false)

  function handleSaveEdit(id: string, name: string, description: string) {
    if (!project) return
    const updatedProject = { ...project, id, name, description }
    setProject(updatedProject)
    const index = mockProjects.findIndex((item) => item.id === id)
    if (index !== -1) mockProjects[index] = updatedProject
  }

  function handleArchive() {
    if (!project || !window.confirm('¿Querés archivar este proyecto?')) return
    setProject({ ...project, status: 'ARCHIVED' })
  }

  if (!project) {
    return <main className="workspace"><section className="project-not-found"><span aria-hidden="true">♪</span><h1>Proyecto no encontrado</h1><p>Volvé a tu biblioteca para elegir un proyecto disponible.</p><button className="primary-action" type="button" onClick={() => navigate('/projects')}>Ver mis proyectos</button></section></main>
  }

  return (
    <main className="workspace">
      <header className="workspace-bar">
        <button className="brand" type="button" onClick={() => navigate('/projects')}><span className="brand__mark" aria-hidden="true">⌁</span><span>Safe &amp; Sound</span></button>
        <span className="workspace-bar__status"><i /> Proyecto privado</span>
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
              <button className="archive-action" type="button" onClick={handleArchive}>Archivar</button>
            </div>
          )}
        </header>
        <FileList projectId={project.id} userRole={project.myRole} />
      </section>
      {showEditModal && <EditProjectModal project={project} onClose={() => setShowEditModal(false)} onSave={handleSaveEdit} />}
    </main>
  )
}
