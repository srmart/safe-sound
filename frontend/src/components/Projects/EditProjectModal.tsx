import { useState } from 'react'
import type { FormEvent } from 'react'
import type { Project } from '../../types'
import './projects.css'

interface EditProjectModalProps {
  project: Project
  onClose: () => void
  onSave: (id: string, name: string, description: string) => void
}

export function EditProjectModal({ project, onClose, onSave }: EditProjectModalProps) {
  const [name, setName] = useState(project.name)
  const [description, setDescription] = useState(project.description)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (name.trim() && description.trim()) onSave(project.id, name.trim(), description.trim())
  }

  return (
    <div className="project-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.currentTarget === event.target) onClose() }}>
      <section className="project-modal" role="dialog" aria-modal="true" aria-labelledby="edit-project-title">
        <div className="project-modal__heading"><div><p className="eyebrow">Proyecto</p><h2 id="edit-project-title">Editar detalles</h2></div><button className="modal-close" type="button" onClick={onClose} aria-label="Cerrar">×</button></div>
        <form className="project-form" onSubmit={handleSubmit}>
          <label htmlFor="edit-project-name">Nombre del proyecto<input id="edit-project-name" value={name} onChange={(event) => setName(event.target.value)} required autoFocus /></label>
          <label htmlFor="edit-project-description">Descripción<textarea id="edit-project-description" value={description} onChange={(event) => setDescription(event.target.value)} rows={4} required /></label>
          <div className="project-modal__actions"><button className="quiet-action" type="button" onClick={onClose}>Cancelar</button><button className="primary-action" type="submit">Guardar cambios</button></div>
        </form>
      </section>
    </div>
  )
}
