// src/components/Projects/ProjectList.tsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mockProjects } from '../../pruebas/data';
import type { Project } from '../../types';
import { CreateProjectModal } from './CreateProjectModal'; // <--- Importar el modal

export const ProjectList: React.FC = () => {
  const [projects, setProjects] = useState(mockProjects);
  const [showCreateModal, setShowCreateModal] = useState(false); // <--- Estado para el modal
  const navigate = useNavigate();

  const handleArchive = (projectId: string) => {
    setProjects((prev) =>
      prev.map((p) =>
        p.id === projectId ? { ...p, status: 'ARCHIVED' } : p
      )
    );
    console.log(`Proyecto ${projectId} archivado (baja lógica).`);
  };

  // <--- Nueva función para crear proyecto
  const handleCreateProject = (name: string, description: string) => {
    const newProject: Project = {
      id: Date.now().toString(), // ID único simulado
      name,
      description,
      status: 'ACTIVE',
      myRole: 'OWNER', // El creador siempre es OWNER (RF4)
    };
    setProjects((prev) => [...prev, newProject]);
    console.log('Proyecto creado exitosamente:', newProject);
  };

  return (
    <div className="p-4 max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-800">Mis Proyectos</h2>
        <button 
          onClick={() => setShowCreateModal(true)} // <--- Abrir modal
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition shadow-sm"
        >
          + Crear Nuevo Proyecto
        </button>
      </div>

      <div className="grid gap-4">
        {projects.map((project) => (
          <div
            key={project.id}
            onClick={() => navigate(`/projects/${project.id}`)}
            className={`border p-5 rounded-lg shadow-sm cursor-pointer transition hover:shadow-md ${
              project.status === 'ARCHIVED' ? 'bg-gray-100 opacity-75' : 'bg-white'
            }`}
          >
            {/* ... (El resto del contenido de la tarjeta queda igual) ... */}
            <div className="flex justify-between items-start">
              <div>
                <h3 className="text-xl font-semibold text-gray-900">{project.name}</h3>
                <p className="text-gray-600 mt-1">{project.description}</p>
                <div className="flex gap-2 mt-3">
                  <span className={`text-xs px-2 py-1 rounded font-medium ${
                    project.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-gray-300 text-gray-700'
                  }`}>
                    {project.status === 'ACTIVE' ? 'Activo' : 'Archivado'}
                  </span>
                  <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded font-medium">
                    Tu rol: {project.myRole}
                  </span>
                </div>
              </div>
              <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
                {project.myRole === 'OWNER' && project.status === 'ACTIVE' && (
                  <>
                    <button className="text-blue-600 hover:underline px-3 py-1 text-sm">Editar</button>
                    <button
                      onClick={() => handleArchive(project.id)}
                      className="text-red-600 hover:underline px-3 py-1 text-sm"
                    >
                      Archivar
                    </button>
                  </>
                )}
                {project.myRole !== 'OWNER' && (
                  <button className="text-gray-400 cursor-not-allowed px-3 py-1 text-sm" disabled>
                    Gestionar
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Renderizar el modal si showCreateModal es true */}
      {showCreateModal && (
        <CreateProjectModal
          onClose={() => setShowCreateModal(false)}
          onCreate={handleCreateProject}
        />
      )}
    </div>
  );
};