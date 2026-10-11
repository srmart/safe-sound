// src/components/Files/UploadFileModal.tsx
import { useEffect, useId, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import './files.css'

interface UploadFileModalProps {
  onClose: () => void
  onUpload: (file: File) => void
}

export function UploadFileModal({ onClose, onUpload }: UploadFileModalProps) {
  const [file, setFile] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const inputId = useId()
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    inputRef.current?.focus()
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onClose])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!file) {
      setError('Seleccioná un archivo para continuar.')
      return
    }
    onUpload(file)
    onClose()
  }

  return (
    <div className="file-modal-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.currentTarget === event.target) onClose()
    }}>
      <section className="file-modal" role="dialog" aria-modal="true" aria-labelledby="upload-file-title">
        <div className="file-modal__heading">
          <div>
            <p className="file-modal__eyebrow">Proyecto</p>
            <h2 id="upload-file-title">Subir archivo</h2>
          </div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Cerrar">×</button>
        </div>
        <form onSubmit={handleSubmit}>
          <label className="file-picker" htmlFor={inputId}>
            <span className="file-picker__icon" aria-hidden="true">↑</span>
            <span className="file-picker__title">{file ? file.name : 'Elegí un archivo'}</span>
            <span className="file-picker__hint">{file ? formatFileSize(file.size) : 'Se conservará el nombre original'}</span>
          </label>
          <input ref={inputRef} id={inputId} className="file-picker__input" type="file" onChange={(event) => {
            setFile(event.target.files?.[0] ?? null)
            setError(null)
          }} />
          {error && <p className="file-form-error" role="alert">{error}</p>}
          <div className="file-modal__actions">
            <button className="button button--secondary" type="button" onClick={onClose}>Cancelar</button>
            <button className="button button--primary" type="submit">Subir archivo</button>
          </div>
        </form>
      </section>
    </div>
  )
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
