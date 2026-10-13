import { useEffect, useMemo, useState } from 'react'
import { getAccessToken } from '../../api/tokenStorage'
import { proyectosApi } from '../../api/proyectos'
import type { ProjectRole } from '../../types'
import { UploadFileModal } from './UploadFileModal'
import './files.css'

// Interfaz para representar un archivo según viene del backend
interface ArchivoBackend {
  id: number
  nombre: string
  tamano: number
  fechaSubida: string
  usuario?: {
    username?: string
    email?: string
  }
}

// Interfaz para el archivo en el frontend (lo que muestra la tabla)
interface ArchivoUI {
  id: string
  fileName: string
  fileSize: string
  uploadedBy: string
  uploadDate: string
  backendId: number
}

interface FileListProps {
  projectId: string
  userRole: ProjectRole
}

const rolesWithModification = new Set<ProjectRole>(['OWNER', 'COLLABORATOR'])

const API_URL = import.meta.env.VITE_API_URL || ''

export function FileList({ projectId, userRole }: FileListProps) {
  const [files, setFiles] = useState<ArchivoUI[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isUploadOpen, setIsUploadOpen] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const canModify = rolesWithModification.has(userRole)

  const fileCountLabel = useMemo(
    () => `${files.length} ${files.length === 1 ? 'archivo disponible' : 'archivos disponibles'}`,
    [files.length],
  )

  // Cargar archivos reales desde el backend al montar el componente
  useEffect(() => {
    let mounted = true
    setIsLoading(true)
    setError(null)

    proyectosApi
      .listarArchivos(projectId)
      .then((archivos: ArchivoBackend[]) => {
        if (!mounted) return
        const archivosUI: ArchivoUI[] = archivos.map((a) => ({
          id: String(a.id),
          backendId: a.id,
          fileName: a.nombre,
          fileSize: formatFileSize(a.tamano),
          uploadedBy: a.usuario?.username || a.usuario?.email || 'Desconocido',
          uploadDate: new Intl.DateTimeFormat('es-UY', { dateStyle: 'medium' }).format(
            new Date(a.fechaSubida),
          ),
        }))
        setFiles(archivosUI)
      })
      .catch((err) => {
        if (!mounted) return
        setError(err instanceof Error ? err.message : 'Error al cargar los archivos')
      })
      .finally(() => {
        if (mounted) setIsLoading(false)
      })

    return () => {
      mounted = false
    }
  }, [projectId])

  // Subir archivo real al backend
  async function handleUpload(source: File) {
    setError(null)
    try {
      const archivoCreado = await proyectosApi.subirArchivo(projectId, source)
      
      const nuevoArchivo: ArchivoUI = {
        id: String(archivoCreado.id),
        backendId: archivoCreado.id,
        fileName: archivoCreado.nombre,
        fileSize: formatFileSize(archivoCreado.tamano),
        uploadedBy: archivoCreado.usuario?.username || 'Vos',
        uploadDate: new Intl.DateTimeFormat('es-UY', { dateStyle: 'medium' }).format(
          new Date(archivoCreado.fechaSubida),
        ),
      }
      
      setFiles((currentFiles) => [nuevoArchivo, ...currentFiles])
      setNotice(`"${source.name}" se agregó al proyecto.`)
      setIsUploadOpen(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al subir el archivo')
    }
  }

  // Eliminar archivo real del backend
  async function handleDelete(file: ArchivoUI) {
    if (!window.confirm(`¿Eliminar "${file.fileName}"? Esta acción no se puede deshacer.`)) return
    
    setError(null)
    try {
      await proyectosApi.eliminarArchivo(projectId, file.id)
      setFiles((currentFiles) => currentFiles.filter((f) => f.id !== file.id))
      setNotice(`"${file.fileName}" fue eliminado del proyecto.`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al eliminar el archivo')
    }
  }

  // Descargar archivo real desde el backend
  async function handleDownload(file: ArchivoUI) {
    try {
      const token = getAccessToken()
      const response = await fetch(`${API_URL}/proyectos/${projectId}/archivos/${file.backendId}`, {
        method: 'GET',
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      })

      if (!response.ok) {
        throw new Error('No se pudo descargar el archivo')
      }

      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = file.fileName
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
      setNotice(`Se descargó "${file.fileName}".`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al descargar')
    }
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
      {error && <p className="file-notice" role="alert" style={{ background: '#fee', color: '#c00' }}>{error}</p>}

      {isLoading ? (
        <div className="file-empty-state">
          <span className="file-empty-state__icon" aria-hidden="true">⌁</span>
          <h3>Cargando archivos...</h3>
        </div>
      ) : files.length === 0 ? (
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