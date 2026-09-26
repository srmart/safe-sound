export interface RegistroRequest {
  email: string
  username: string
  password: string
}

export type RegistroFieldErrors = Record<string, string>

export class RegistroValidationError extends Error {
  fieldErrors: RegistroFieldErrors

  constructor(fieldErrors: RegistroFieldErrors) {
    super('Datos de registro inválidos')
    this.fieldErrors = fieldErrors
  }
}

export class RegistroConflictError extends Error {}

export async function registrar(request: RegistroRequest): Promise<void> {
  const response = await fetch('/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })

  if (response.status === 201) {
    return
  }

  if (response.status === 400) {
    const fieldErrors = (await response.json()) as RegistroFieldErrors
    throw new RegistroValidationError(fieldErrors)
  }

  if (response.status === 409) {
    const message = await response.text()
    throw new RegistroConflictError(message)
  }

  throw new Error('No se pudo completar el registro. Intentá nuevamente.')
}
