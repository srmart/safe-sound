import { useMemo, useState } from 'react'
import { mockFiles } from '../../pruebas/data'
import type { ProjectFile, ProjectRole } from '../../types'
import { UploadFileModal } from './UploadFileModal'
import './files.css'

interface FileListProps {
  projectId: string
  userRole: ProjectRole
}

type LocalProjectFile = ProjectFile & { source?: File }

const rolesWithModification = new Set<ProjectRole>(['OWNER', 'COLLABORATOR'])

export function FileList({ projectId, userRole }: FileListProps) {
  const [files, setFiles] = useState<LocalProjectFile[]>(() =>
    mockFiles.filter((file) => file.projectId === projectId),
  )
  const [isUploadOpen, setIsUploadOpen] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const canModify = rolesWithModification.has(userRole)
  const fileCountLabel = useMemo(
    () => `${files.length} ${files.length === 1 ? 'archivo disponible' : 'archivos disponibles'}`,
    [files.length],
  )

  function handleUpload(source: File) {
    const addedFile: LocalProjectFile = {
      id: crypto.randomUUID(),
      projectId,
      fileName: source.name,
      fileSize: formatFileSize(source.size),
      uploadedBy: 'Vos',
      uploadDate: new Intl.DateTimeFormat('es-UY', { dateStyle: 'medium' }).format(new Date()),
      source,
    }
    setFiles((currentFiles) => [addedFile, ...currentFiles])
    setNotice(`“${source.name}” se agregó al proyecto.`)
  }

  function handleDelete(file: LocalProjectFile) {
    if (!window.confirm(`¿Eliminar “${file.fileName}”? Esta acción no se puede deshacer.`)) return
    setFiles((currentFiles) => currentFiles.filter((currentFile) => currentFile.id !== file.id))
    setNotice(`“${file.fileName}” fue eliminado del proyecto.`)
  }

  function handleDownload(file: LocalProjectFile) {
    const blob = file.source ?? new Blob(
      [`Archivo de muestra: ${file.fileName}\nSubido por ${file.uploadedBy} el ${file.uploadDate}.`],
      { type: 'text/plain;charset=utf-8' },
    )
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = file.fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    setNotice(`Se descargó “${file.fileName}”.`)
  }

  return (
    <section className="file-manager" aria-labelledby="files-title">
      <div className="file-manager__header">
        <div>
          <p className="file-manager__eyebrow">Biblioteca del proyecto</p>
          <h2 id="files-title">Archivos</h2>
          <p className="file-manager__count">{fileCountLabel}</p>
        </div>
        {canModify && (
          <button className="button button--primary" type="button" onClick={() => setIsUploadOpen(true)}>
            <span aria-hidden="true">＋</span> Subir archivo
          </button>
        )}
      </div>

      {notice && <p className="file-notice" role="status">{notice}</p>}

      {files.length === 0 ? (
        <div className="file-empty-state">
          <span className="file-empty-state__icon" aria-hidden="true">⌁</span>
          <h3>Todavía no hay archivos</h3>
          <p>
            {canModify
              ? 'Subí el primer archivo para que las personas del proyecto puedan consultarlo.'
              : 'Cuando se agreguen archivos al proyecto, vas a poder descargarlos desde acá.'}
          </p>
        </div>
      ) : (
        <div className="file-table-wrapper">
          <table className="file-table">
            <thead>
              <tr>
                <th scope="col">Archivo</th>
                <th scope="col">Tamaño</th>
                <th scope="col">Subido por</th>
                <th scope="col">Fecha</th>
                <th scope="col"><span className="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {files.map((file) => (
                <tr key={file.id}>
                  <td data-label="Archivo">
                    <div className="file-name">
                      <span className="file-type-icon" aria-hidden="true">{extensionLabel(file.fileName)}</span>
                      <span>{file.fileName}</span>
                    </div>
                  </td>
                  <td data-label="Tamaño">{file.fileSize}</td>
                  <td data-label="Subido por">{file.uploadedBy}</td>
                  <td data-label="Fecha">{file.uploadDate}</td>
                  <td className="file-actions" data-label="Acciones">
                    <button className="button-link" type="button" onClick={() => handleDownload(file)}>Descargar</button>
                    {canModify && (
                      <button className="button-link button-link--danger" type="button" onClick={() => handleDelete(file)}>Eliminar</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {isUploadOpen && <UploadFileModal onClose={() => setIsUploadOpen(false)} onUpload={handleUpload} />}
    </section>
  )
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function extensionLabel(fileName: string) {
  const extension = fileName.split('.').pop()?.toUpperCase()
  return extension && extension.length <= 4 ? extension : 'FILE'
}
