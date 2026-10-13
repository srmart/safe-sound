// frontend/src/api/proyectos.ts
import { getAccessToken } from './tokenStorage';

const API_URL = import.meta.env.VITE_API_URL || '';

export class SesionExpiradaError extends Error {}

// Función auxiliar para construir los headers con el JWT (RS7)
// Agregamos 'isMultipart' para no forzar application/json cuando subimos archivos
const getHeaders = (isMultipart = false) => {
  const token = getAccessToken();
  const headers: Record<string, string> = {};

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // Solo establecemos Content-Type json si NO es multipart
  if (!isMultipart) {
    headers['Content-Type'] = 'application/json';
  }
  // Si es multipart, el navegador establece automáticamente el Content-Type correcto con el boundary

  return headers;
};

// Función auxiliar para manejo seguro de errores (RS9)
const handleApiError = async (response: Response, action: string) => {
  if (response.status === 401) {
    throw new SesionExpiradaError('Tu sesión expiró. Iniciá sesión nuevamente.');
  }

  if (response.status === 403) {
    throw new Error('No tienes permisos para realizar esta acción.');
  }

  throw new Error(
      `Ocurrió un error al intentar ${action}. Inténtalo de nuevo más tarde.`
  );
};

export const proyectosApi = {

  // 1. R5 - Listar proyectos con búsqueda y filtros
  listar: async (nombre?: string, activo?: boolean) => {
    const params = new URLSearchParams();
    if (nombre?.trim()) params.append('nombre', nombre.trim());
    if (activo !== undefined) params.append('activo', String(activo));

    const query = params.toString();
    const url = `${API_URL}/proyectos${query ? `?${query}` : ''}`;

    const response = await fetch(url, { method: 'GET', headers: getHeaders() });
    if (!response.ok) await handleApiError(response, 'listar los proyectos');
    return response.json();
  },

  // 2. Crear proyecto (RF4)
  crear: async (nombre: string, descripcion: string) => {
    const url = `${API_URL}/proyectos?nombre=${encodeURIComponent(nombre)}&descripcion=${encodeURIComponent(descripcion)}`;
    const response = await fetch(url, { method: 'POST', headers: getHeaders() });
    if (!response.ok) await handleApiError(response, 'crear el proyecto');
    return response.json();
  },

  // 3. Obtener detalle de un proyecto (RF4)
  obtenerPorId: async (proyectoId: string) => {
    const response = await fetch(`${API_URL}/proyectos/${proyectoId}`, { method: 'GET', headers: getHeaders() });
    if (!response.ok) await handleApiError(response, 'obtener el proyecto');
    return response.json();
  },

  // 4. Modificar proyecto (RF4)
  modificar: async (proyectoId: string, nombre: string, descripcion: string) => {
    const url = `${API_URL}/proyectos/${proyectoId}?nombre=${encodeURIComponent(nombre)}&descripcion=${encodeURIComponent(descripcion)}`;
    const response = await fetch(url, { method: 'PUT', headers: getHeaders() });
    if (!response.ok) await handleApiError(response, 'modificar el proyecto');
    return response.json();
  },

  // 5. Archivar proyecto (Baja lógica - RF4)
  archivar: async (proyectoId: string) => {
    const response = await fetch(`${API_URL}/proyectos/${proyectoId}`, { method: 'DELETE', headers: getHeaders() });
    if (!response.ok) await handleApiError(response, 'archivar el proyecto');
    return true;
  },

  // ==========================================
  // NUEVOS MÉTODOS PARA ARCHIVOS
  // ==========================================

  // 6. Listar archivos de un proyecto
  listarArchivos: async (proyectoId: string) => {
    const response = await fetch(`${API_URL}/proyectos/${proyectoId}/archivos`, {
      method: 'GET',
      headers: getHeaders(),
    });
    if (!response.ok) await handleApiError(response, 'listar los archivos');
    return response.json();
  },

  // 7. Subir archivo a un proyecto
  subirArchivo: async (proyectoId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${API_URL}/proyectos/${proyectoId}/archivos`, {
      method: 'POST',
      headers: getHeaders(true), // true = es multipart, NO envía Content-Type: application/json
      body: formData,
    });

    if (!response.ok) await handleApiError(response, 'subir el archivo');
    return response.json();
  },

  // 8. Eliminar archivo de un proyecto
  eliminarArchivo: async (proyectoId: string, archivoId: string) => {
    const response = await fetch(`${API_URL}/proyectos/${proyectoId}/archivos/${archivoId}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    if (!response.ok) await handleApiError(response, 'eliminar el archivo');
    return true;
  },
};