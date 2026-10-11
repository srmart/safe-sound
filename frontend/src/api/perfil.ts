import { getAccessToken } from './tokenStorage'

export interface Perfil {
  id: number
  email: string
  username: string
  fotoPerfil: string | null
  notificacionesHabilitadas: boolean
  modoOscuro: boolean
}

export class PerfilApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

function headers() {
  const token = getAccessToken()
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
}

async function parseResponse(response: Response): Promise<Perfil> {
  if (response.ok) return response.json() as Promise<Perfil>

  if (response.status === 401) {
    throw new PerfilApiError('Tu sesión expiró. Iniciá sesión nuevamente.', 401)
  }

  const message = await response.text()
  throw new PerfilApiError(message || 'No se pudo guardar la información.', response.status)
}

export const perfilApi = {
  obtener: async () => parseResponse(await fetch('/perfil', { headers: headers() })),

  actualizar: async (username: string, fotoPerfil: string | null) => parseResponse(await fetch('/perfil', {
    method: 'PUT',
    headers: headers(),
    body: JSON.stringify({ username, fotoPerfil }),
  })),

  actualizarPreferencias: async (notificacionesHabilitadas: boolean, modoOscuro: boolean) => parseResponse(await fetch('/perfil/preferencias', {
    method: 'PUT',
    headers: headers(),
    body: JSON.stringify({ notificacionesHabilitadas, modoOscuro }),
  })),
}
