// src/components/Files/FileList.tsx
import React, { useState } from 'react';
import { mockFiles } from '../../pruebas/data';
import type { ProjectFile, ProjectRole } from '../../types';
import { UploadFileModal } from './UploadFileModal';

interface FileListProps {
  projectId: string;
  userRole: ProjectRole;
}

export const FileList: React.FC<FileListProps> = ({ projectId, userRole }) => {
  const [files, setFiles] = useState(mockFiles.filter(f => f.projectId === projectId));
  const [showUploadModal, setShowUploadModal] = useState(false);

  // Permisos según rol (RF6)
  const canModify = userRole === 'OWNER' || userRole === 'COLLABORATOR';
  const canView = userRole === 'OWNER' || userRole === 'COLLABORATOR' || userRole === 'VIEWER';

  // Eliminar archivo
  const handleDelete = (fileId: string) => {
    setFiles((prev) => prev.filter((f) => f.id !== fileId));
    console.log(`Archivo ${fileId} eliminado.`);
  };

  // Descargar archivo (simulado con Blob)
  const handleDownload = (file: ProjectFile) => {
    // Creamos contenido simulado para el archivo
    const content = `Este es el contenido simulado del archivo: ${file.fileName}\nSubido por: ${file.uploadedBy}\nFecha: ${file.uploadDate}`;
    
    // Creamos un Blob (objeto tipo archivo) en el navegador
    const blob = new Blob([content], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    
    // Creamos un enlace invisible y forzamos el clic para descargar
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', file.fileName);
    document.body.appendChild(link);
    link.click();
    
    // Limpiamos
    link.parentNode?.removeChild(link);
    window.URL.revokeObjectURL(url);
    console.log(`Archivo ${file.fileName} descargado exitosamente.`);
  };

  // Subir archivo (desde el modal)
  const handleUpload = (fileName: string, fileSize: string) => {
    const newFile: ProjectFile = {
      id: Date.now().toString(),
      projectId,
      fileName,
      fileSize,
      uploadedBy: 'Usuario Actual', // Simulado
      uploadDate: new Date().toISOString().split('T')[0],
    };
    setFiles((prev) => [...prev, newFile]);
    console.log('Archivo subido exitosamente:', newFile);
  };

  if (!canView) {
    return <p className="text-red-600">No tienes permiso para ver los archivos de este proyecto.</p>;
  }

  return (
    <div className="p-4">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-2xl font-bold text-gray-800">Archivos del Proyecto</h2>
        
        {/* Solo visible para roles con permiso de modificación (OWNER, COLLABORATOR) */}
        {canModify && (
          <button 
            onClick={() => setShowUploadModal(true)}
            className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 transition"
          >
            📤 Subir Archivo
          </button>
        )}
      </div>

      {files.length === 0 ? (
        <p className="text-gray-500">No hay archivos en este proyecto.</p>
      ) : (
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b">
              <th className="p-2">Nombre</th>
              <th className="p-2">Tamaño</th>
              <th className="p-2">Subido por</th>
              <th className="p-2">Fecha</th>
              <th className="p-2">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {files.map((file) => (
              <tr key={file.id} className="border-b hover:bg-gray-50">
                <td className="p-2">{file.fileName}</td>
                <td className="p-2">{file.fileSize}</td>
                <td className="p-2">{file.uploadedBy}</td>
                <td className="p-2">{file.uploadDate}</td>
                <td className="p-2 flex gap-3">
                  {/* Descargar: Disponible para TODOS los roles con acceso al proyecto */}
                  <button 
                    onClick={() => handleDownload(file)}
                    className="text-blue-600 hover:underline"
                  >
                    Descargar
                  </button>
                  
                  {/* Eliminar: SOLO para OWNER y COLLABORATOR */}
                  {canModify && (
                    <button 
                      onClick={() => handleDelete(file.id)}
                      className="text-red-600 hover:underline"
                    >
                      Eliminar
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {/* Modal de subida de archivos */}
      {showUploadModal && (
        <UploadFileModal
          projectId={projectId}
          onClose={() => setShowUploadModal(false)}
          onUpload={handleUpload}
        />
      )}
    </div>
  );
};