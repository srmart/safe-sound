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

export interface LoginRequest {
  email: string
  password: string
}

export interface Tokens {
  accessToken: string
  refreshToken: string
}

export type LoginFieldErrors = Record<string, string>

export class LoginValidationError extends Error {
  fieldErrors: LoginFieldErrors

  constructor(fieldErrors: LoginFieldErrors) {
    super('Datos de inicio de sesión inválidos')
    this.fieldErrors = fieldErrors
  }
}

export class CredencialesInvalidasError extends Error {}

export async function login(request: LoginRequest): Promise<Tokens> {
  const response = await fetch('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })

  if (response.status === 200) {
    return (await response.json()) as Tokens
  }

  if (response.status === 400) {
    const fieldErrors = (await response.json()) as LoginFieldErrors
    throw new LoginValidationError(fieldErrors)
  }

  if (response.status === 401) {
    const message = await response.text()
    throw new CredencialesInvalidasError(message)
  }

  throw new Error('No se pudo iniciar sesión. Intentá nuevamente.')
}

export async function logout(refreshToken: string): Promise<void> {
  const response = await fetch('/auth/logout', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })

  if (response.status !== 204) {
    throw new Error('No se pudo cerrar sesión. Intentá nuevamente.')
  }
}

export async function refresh(refreshToken: string): Promise<Tokens> {
  const response = await fetch('/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })

  if (response.status === 200) {
    return (await response.json()) as Tokens
  }

  throw new Error('No se pudo renovar la sesión.')
}
