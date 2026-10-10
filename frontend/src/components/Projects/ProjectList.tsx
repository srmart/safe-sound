
import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Project, ProjectRole } from '../../types';
import { proyectosApi } from '../../api/proyectos';
import { CreateProjectModal } from './CreateProjectModal';

// Estructura que devuelve el backend para R5
interface ProyectoListadoResponse {
  id: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
  rol: ProjectRole;
}

type FiltroEstado = 'ALL' | 'ACTIVE' | 'ARCHIVED';

export const ProjectList: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // R5 - Búsqueda y filtros
  const [nombre, setNombre] = useState('');
  const [filtroEstado, setFiltroEstado] = useState<FiltroEstado>('ALL');

  const navigate = useNavigate();

  // R5 - Cargar proyectos desde el backend
  const fetchProjects = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      let activo: boolean | undefined;

      if (filtroEstado === 'ACTIVE') {
        activo = true;
      } else if (filtroEstado === 'ARCHIVED') {
        activo = false;
      }

      const data: ProyectoListadoResponse[] =
          await proyectosApi.listar(nombre, activo);

      // Convertir la respuesta del backend al tipo Project
      const mappedProjects: Project[] = data.map((p) => ({
        id: p.id.toString(),
        name: p.nombre,
        description: p.descripcion || '',
        status: p.activo ? 'ACTIVE' : 'ARCHIVED',
        myRole: p.rol,
      }));

      setProjects(mappedProjects);
    } catch (err: unknown) {
      setError(
          err instanceof Error
              ? err.message
              : 'No se pudieron cargar los proyectos.'
      );
    } finally {
      setLoading(false);
    }
  }, [nombre, filtroEstado]);

  // R5 - Actualizar listado al cambiar búsqueda o filtros
  useEffect(() => {
    void fetchProjects();
  }, [fetchProjects]);

  // Archivar proyecto
  const handleArchive = async (projectId: string) => {
    if (
        !window.confirm(
            '¿Estás seguro de que deseas archivar este proyecto?'
        )
    ) {
      return;
    }

    try {
      await proyectosApi.archivar(projectId);

      // Recargar para respetar los filtros actuales
      await fetchProjects();
    } catch (err: unknown) {
      alert(
          err instanceof Error
              ? err.message
              : 'No se pudo archivar el proyecto.'
      );
    }
  };

  // Crear proyecto
  const handleCreateProject = async (
      name: string,
      description: string
  ) => {
    try {
      await proyectosApi.crear(name, description);
      setShowCreateModal(false);

      // Actualizar el listado después de crear
      await fetchProjects();
    } catch (err: unknown) {
      alert(
          err instanceof Error
              ? err.message
              : 'No se pudo crear el proyecto.'
      );
    }
  };

  return (
      <div className="p-4 max-w-6xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-gray-800">
            Mis Proyectos
          </h2>

          <button
              onClick={() => setShowCreateModal(true)}
              className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition shadow-sm"
          >
            + Crear Nuevo Proyecto
          </button>
        </div>

        {/* R5 - Controles de búsqueda y filtrado */}
        <div className="flex flex-col md:flex-row gap-4 mb-6">
          <div className="flex-1">
            <label
                htmlFor="buscar-proyecto"
                className="block text-sm font-medium text-gray-700 mb-1"
            >
              Buscar por nombre
            </label>

            <input
                id="buscar-proyecto"
                type="text"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                placeholder="Buscar proyectos..."
                className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="w-full md:w-56">
            <label
                htmlFor="filtro-estado"
                className="block text-sm font-medium text-gray-700 mb-1"
            >
              Estado
            </label>

            <select
                id="filtro-estado"
                value={filtroEstado}
                onChange={(e) =>
                    setFiltroEstado(e.target.value as FiltroEstado)
                }
                className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="ALL">Todos</option>
              <option value="ACTIVE">Activos</option>
              <option value="ARCHIVED">Archivados</option>
            </select>
          </div>

          <div className="flex items-end">
            <button
                onClick={() => {
                  setNombre('');
                  setFiltroEstado('ALL');
                }}
                className="border border-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-100"
            >
              Limpiar filtros
            </button>
          </div>
        </div>

        {/* Estado de carga */}
        {loading && (
            <div className="p-8 text-center text-gray-600">
              Cargando proyectos...
            </div>
        )}

        {/* Manejo de errores */}
        {!loading && error && (
            <div className="p-8 text-center">
              <h2 className="text-xl text-red-600 mb-4">
                {error}
              </h2>

              <button
                  onClick={() => void fetchProjects()}
                  className="text-blue-600 hover:underline"
              >
                Reintentar
              </button>
            </div>
        )}

        {/* Listado de proyectos */}
        {!loading && !error && (
            <div className="grid gap-4">
              {projects.length === 0 ? (
                  <p className="text-gray-500 text-center py-8">
                    {nombre.trim() || filtroEstado !== 'ALL'
                        ? 'No se encontraron proyectos con los filtros seleccionados.'
                        : 'No tienes proyectos aún. ¡Crea uno nuevo!'}
                  </p>
              ) : (
                  projects.map((project) => (
                      <div
                          key={project.id}
                          onClick={() =>
                              navigate(`/projects/${project.id}`)
                          }
                          className={`border p-5 rounded-lg shadow-sm cursor-pointer transition hover:shadow-md ${
                              project.status === 'ARCHIVED'
                                  ? 'bg-gray-100 opacity-75'
                                  : 'bg-white'
                          }`}
                      >
                        <div className="flex justify-between items-start">
                          <div>
                            <h3 className="text-xl font-semibold text-gray-900">
                              {project.name}
                            </h3>

                            <p className="text-gray-600 mt-1">
                              {project.description}
                            </p>

                            <div className="flex gap-2 mt-3">
                      <span
                          className={`text-xs px-2 py-1 rounded font-medium ${
                              project.status === 'ACTIVE'
                                  ? 'bg-green-100 text-green-800'
                                  : 'bg-gray-300 text-gray-700'
                          }`}
                      >
                        {project.status === 'ACTIVE'
                            ? 'Activo'
                            : 'Archivado'}
                      </span>

                              <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded font-medium">
                        Tu rol: {project.myRole}
                      </span>
                            </div>
                          </div>

                          {/* Acciones según rol y estado */}
                          <div
                              className="flex gap-2"
                              onClick={(e) => e.stopPropagation()}
                          >
                            {project.myRole === 'OWNER' &&
                                project.status === 'ACTIVE' && (
                                    <>
                                      <button
                                          onClick={(e) => {
                                            e.stopPropagation();
                                            navigate(
                                                `/projects/${project.id}`
                                            );
                                          }}
                                          className="text-blue-600 hover:underline px-3 py-1 text-sm"
                                      >
                                        Editar
                                      </button>

                                      <button
                                          onClick={(e) => {
                                            e.stopPropagation();
                                            void handleArchive(project.id);
                                          }}
                                          className="text-red-600 hover:underline px-3 py-1 text-sm"
                                      >
                                        Archivar
                                      </button>
                                    </>
                                )}

                            {project.myRole === 'VIEWER' && (
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
        )}

        {/* Modal para crear proyectos */}
        {showCreateModal && (
            <CreateProjectModal
                onClose={() => setShowCreateModal(false)}
                onCreate={handleCreateProject}
            />
        )}
      </div>
  );
};

