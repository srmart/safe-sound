import { useState } from 'react'
import type { FormEvent } from 'react'
import './projects.css'

interface CreateProjectModalProps {
  onClose: () => void
  onCreate: (name: string, description: string) => void
}

export function CreateProjectModal({ onClose, onCreate }: CreateProjectModalProps) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (name.trim() && description.trim()) onCreate(name.trim(), description.trim())
  }

  return (
    <div className="project-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.currentTarget === event.target) onClose() }}>
      <section className="project-modal" role="dialog" aria-modal="true" aria-labelledby="create-project-title">
        <div className="project-modal__heading"><div><p className="eyebrow">Nuevo espacio creativo</p><h2 id="create-project-title">Crear proyecto</h2></div><button className="modal-close" type="button" onClick={onClose} aria-label="Cerrar">×</button></div>
        <form className="project-form" onSubmit={handleSubmit}>
          <label htmlFor="project-name">Nombre del proyecto<input id="project-name" value={name} onChange={(event) => setName(event.target.value)} placeholder="Ej. EP acústico" required autoFocus /></label>
          <label htmlFor="project-description">Descripción<textarea id="project-description" value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Contá brevemente qué estás creando" rows={4} required /></label>
          <div className="project-modal__actions"><button className="quiet-action" type="button" onClick={onClose}>Cancelar</button><button className="primary-action" type="submit">Crear proyecto</button></div>
        </form>
      </section>
    </div>
  )
}
