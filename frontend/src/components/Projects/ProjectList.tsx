// src/components/Projects/ProjectList.tsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Project } from '../../types';
import { proyectosApi } from '../../api/proyectos';
import { CreateProjectModal } from './CreateProjectModal';

export const ProjectList: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const navigate = useNavigate();

  // Función para cargar proyectos reales del backend
  const fetchProjects = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await proyectosApi.listar();
      
      // Mapear la respuesta del backend al tipo Project del frontend
      const mappedProjects: Project[] = data.map((p: any) => ({
        id: p.id.toString(),
        name: p.nombre,
        description: p.descripcion || '',
        // Asumimos que el backend tiene un campo 'activo' (boolean) o similar. 
        // Si se llama 'estado', ajustalo a: p.estado === 'ACTIVO'
        status: p.activo !== false ? 'ACTIVE' : 'ARCHIVED', 
        // Si el backend no devuelve el rol en la lista, default a 'OWNER' o 'VIEWER' según convenga
        myRole: p.miRol || 'OWNER', 
      }));
      
      setProjects(mappedProjects);
    } catch (err: any) {
      setError(err.message); // RS9: Captura el mensaje de error seguro
    } finally {
      setLoading(false);
    }
  };

  // Cargar al montar el componente
  useEffect(() => {
    fetchProjects();
  }, []);

  const handleArchive = async (projectId: string) => {
    if (!window.confirm('¿Estás seguro de que deseas archivar este proyecto?')) return;

    try {
      await proyectosApi.archivar(projectId);
      // Actualizar estado local para reflejar el cambio inmediatamente sin recargar todo
      setProjects((prev) =>
        prev.map((p) =>
          p.id === projectId ? { ...p, status: 'ARCHIVED' } : p
        )
      );
    } catch (err: any) {
      alert(err.message); // RS9: Muestra el mensaje genérico seguro
    }
  };

  const handleCreateProject = async (name: string, description: string) => {
    try {
      await proyectosApi.crear(name, description);
      setShowCreateModal(false);
      // Recargar la lista para obtener el proyecto recién creado con su ID real de la BD
      fetchProjects();
    } catch (err: any) {
      alert(err.message); // RS9: Muestra el mensaje genérico seguro
    }
  };

  if (loading) return <div className="p-8 text-center text-gray-600">Cargando proyectos...</div>;
  
  if (error) {
    return (
      <div className="p-8 text-center">
        <h2 className="text-xl text-red-600 mb-4">{error}</h2>
        <button onClick={fetchProjects} className="text-blue-600 hover:underline">
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div className="p-4 max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-800">Mis Proyectos</h2>
        <button 
          onClick={() => setShowCreateModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition shadow-sm"
        >
          + Crear Nuevo Proyecto
        </button>
      </div>

      <div className="grid gap-4">
        {projects.length === 0 ? (
          <p className="text-gray-500 text-center py-8">No tienes proyectos aún. ¡Crea uno nuevo!</p>
        ) : (
          projects.map((project) => (
            <div
              key={project.id}
              onClick={() => navigate(`/projects/${project.id}`)}
              className={`border p-5 rounded-lg shadow-sm cursor-pointer transition hover:shadow-md ${
                project.status === 'ARCHIVED' ? 'bg-gray-100 opacity-75' : 'bg-white'
              }`}
            >
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
                      <button 
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/projects/${project.id}`); // Navega al detalle para editar
                        }}
                        className="text-blue-600 hover:underline px-3 py-1 text-sm"
                      >
                        Editar
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleArchive(project.id);
                        }}
                        className="text-red-600 hover:underline px-3 py-1 text-sm"
                      >
                        Archivar
                      </button>
                    </>
                  )}
                  {project.myRole !== 'OWNER' && (
                    <span className="text-gray-400 px-3 py-1 text-sm">
                      Solo lectura
                    </span>
                  )}
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {showCreateModal && (
        <CreateProjectModal
          onClose={() => setShowCreateModal(false)}
          onCreate={handleCreateProject}
        />
      )}
    </div>
  );
};