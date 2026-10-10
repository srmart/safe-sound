
// frontend/src/api/proyectos.ts
import { getAccessToken } from './tokenStorage';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Función auxiliar para construir los headers con el JWT (RS7)
const getHeaders = () => {
  const token = getAccessToken();

  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
};

// Función auxiliar para manejo seguro de errores (RS9)
const handleApiError = async (response: Response, action: string) => {
  if (response.status === 401) {
    throw new Error(
        'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.'
    );
  }

  if (response.status === 403) {
    throw new Error('No tienes permisos para realizar esta acción.');
  }

  // Mensaje genérico para cualquier otro error (500, 400, etc.)
  throw new Error(
      `Ocurrió un error al intentar ${action}. Inténtalo de nuevo más tarde.`
  );
};

export const proyectosApi = {

  // 1. R5 - Listar proyectos con búsqueda y filtros
  listar: async (nombre?: string, activo?: boolean) => {
    const params = new URLSearchParams();

    if (nombre?.trim()) {
      params.append('nombre', nombre.trim());
    }

    if (activo !== undefined) {
      params.append('activo', String(activo));
    }

    const query = params.toString();
    const url = `${API_URL}/proyectos${query ? `?${query}` : ''}`;

    const response = await fetch(url, {
      method: 'GET',
      headers: getHeaders(),
    });

    if (!response.ok) {
      await handleApiError(response, 'listar los proyectos');
    }

    return response.json();
  },

  // 2. Crear proyecto (RF4)
  crear: async (nombre: string, descripcion: string) => {
    const url = `${API_URL}/proyectos?nombre=${encodeURIComponent(nombre)}&descripcion=${encodeURIComponent(descripcion)}`;

    const response = await fetch(url, {
      method: 'POST',
      headers: getHeaders(),
    });

    if (!response.ok) {
      await handleApiError(response, 'crear el proyecto');
    }

    return response.json();
  },

  // 3. Obtener detalle de un proyecto (RF4)
  obtenerPorId: async (proyectoId: string) => {
    const response = await fetch(`${API_URL}/proyectos/${proyectoId}`, {
      method: 'GET',
      headers: getHeaders(),
    });

    if (!response.ok) {
      await handleApiError(response, 'obtener el proyecto');
    }

    return response.json();
  },

  // 4. Modificar proyecto (RF4)
  modificar: async (
      proyectoId: string,
      nombre: string,
      descripcion: string
  ) => {
    const url = `${API_URL}/proyectos/${proyectoId}?nombre=${encodeURIComponent(nombre)}&descripcion=${encodeURIComponent(descripcion)}`;

    const response = await fetch(url, {
      method: 'PUT',
      headers: getHeaders(),
    });

    if (!response.ok) {
      await handleApiError(response, 'modificar el proyecto');
    }

    return response.json();
  },

  // 5. Archivar proyecto (Baja lógica - RF4)
  archivar: async (proyectoId: string) => {
    const response = await fetch(`${API_URL}/proyectos/${proyectoId}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });

    if (!response.ok) {
      await handleApiError(response, 'archivar el proyecto');
    }

    return true;
  },
};
