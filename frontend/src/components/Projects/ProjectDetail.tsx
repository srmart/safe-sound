// src/components/Projects/ProjectDetail.tsx
import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { mockProjects } from '../../pruebas/data';
import type { Project } from '../../types';
import { FileList } from '../Files/FileList';
import { EditProjectModal } from './EditProjectModal'; // <--- Importar modal

export const ProjectDetail: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  
  // Estado local para simular la edición en esta vista
  const [project, setProject] = useState<Project | undefined>(mockProjects.find(p => p.id === projectId));
  const [showEditModal, setShowEditModal] = useState(false);

  const handleSaveEdit = (id: string, name: string, description: string) => {
    if (project) {
      // Le decimos explícitamente que es de tipo Project
      const updatedProject: Project = { ...project, name, description };
      setProject(updatedProject);
      
      // Actualizar también en mockProjects para que se vea en la lista
      const index = mockProjects.findIndex(p => p.id === id);
      if (index !== -1) mockProjects[index] = updatedProject;
      console.log('Proyecto modificado:', updatedProject);
    }
  };

  const handleArchive = () => {
    if (project) {
      // Le decimos explícitamente que es de tipo Project
      const archivedProject: Project = { ...project, status: 'ARCHIVED' };
      setProject(archivedProject);
      
      const index = mockProjects.findIndex(p => p.id === project.id);
      if (index !== -1) mockProjects[index] = archivedProject;
      console.log('Proyecto archivado (baja lógica).');
    }
  };

  if (!project) {
    return (  
      <div className="p-4 text-center">
        <h2 className="text-xl text-red-600">Proyecto no encontrado</h2>
        <button onClick={() => navigate('/projects')} className="mt-4 text-blue-600 hover:underline">
          ← Volver a Proyectos
        </button>
      </div>
    );
  }

  return (
    <div className="p-4 max-w-6xl mx-auto">
      <button onClick={() => navigate('/projects')} className="text-sm text-gray-500 hover:text-gray-700 mb-4">
        ← Volver a Mis Proyectos
      </button>

      <div className="flex justify-between items-start mb-8 border-b pb-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">{project.name}</h1>
          <p className="text-gray-600 mt-1">{project.description}</p>
          <div className="flex gap-2 mt-3">
            <span className={`text-xs px-2 py-1 rounded ${
              project.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-gray-300 text-gray-700'
            }`}>
              {project.status === 'ACTIVE' ? 'Activo' : 'Archivado'}
            </span>
            <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded">
              Tu rol: {project.myRole}
            </span>
          </div>
        </div>

        <div className="flex gap-3">
          {project.myRole === 'OWNER' && project.status === 'ACTIVE' && (
            <>
              <button 
                onClick={() => setShowEditModal(true)}
                className="bg-yellow-500 text-white px-4 py-2 rounded hover:bg-yellow-600 transition"
              >
                ✏️ Modificar
              </button>
              <button 
                onClick={handleArchive}
                className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 transition"
              >
                ️ Archivar
              </button>
            </>
          )}
        </div>
      </div>

      <FileList projectId={project.id} userRole={project.myRole} />

      {showEditModal && (
        <EditProjectModal
          project={project}
          onClose={() => setShowEditModal(false)}
          onSave={handleSaveEdit}
        />
      )}
    </div>
  );
};