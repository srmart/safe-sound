import { useState } from 'react'
import type { FormEvent } from 'react'
import {
  registrar,
  RegistroConflictError,
  RegistroValidationError,
  type RegistroFieldErrors,
} from '../api/authApi'

const USERNAME_PATTERN = /^[a-zA-Z0-9_]+$/

function validate(email: string, username: string, password: string): RegistroFieldErrors {
  const errors: RegistroFieldErrors = {}

  if (!email.trim()) {
    errors.email = 'El email es obligatorio'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.email = 'El formato del email no es válido'
  }

  if (!username.trim()) {
    errors.username = 'El nombre de usuario es obligatorio'
  } else if (username.length < 3 || username.length > 30) {
    errors.username = 'El nombre de usuario debe tener entre 3 y 30 caracteres'
  } else if (!USERNAME_PATTERN.test(username)) {
    errors.username = 'El nombre de usuario solo puede contener letras, números y guiones bajos'
  }

  if (!password) {
    errors.password = 'La contraseña es obligatoria'
  } else if (password.length < 8) {
    errors.password = 'La contraseña debe tener al menos 8 caracteres'
  }

  return errors
}

export default function RegisterPage() {
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<RegistroFieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError(null)
    setSuccess(false)

    const errors = validate(email, username, password)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) {
      return
    }

    setSubmitting(true)
    try {
      await registrar({ email, username, password })
      setSuccess(true)
      setEmail('')
      setUsername('')
      setPassword('')
      setFieldErrors({})
    } catch (error) {
      if (error instanceof RegistroValidationError) {
        setFieldErrors(error.fieldErrors)
      } else if (error instanceof RegistroConflictError) {
        setFormError(error.message)
      } else {
        setFormError('No se pudo completar el registro. Intentá nuevamente.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <h1>Crear cuenta</h1>

        {success && (
          <p className="alert alert-success" role="status">
            Registro exitoso. Ya podés iniciar sesión.
          </p>
        )}
        {formError && (
          <p className="alert alert-error" role="alert">
            {formError}
          </p>
        )}

        <label htmlFor="email">Email</label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          aria-invalid={Boolean(fieldErrors.email)}
        />
        {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}

        <label htmlFor="username">Nombre de usuario</label>
        <input
          id="username"
          type="text"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          aria-invalid={Boolean(fieldErrors.username)}
        />
        {fieldErrors.username && <span className="field-error">{fieldErrors.username}</span>}

        <label htmlFor="password">Contraseña</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          aria-invalid={Boolean(fieldErrors.password)}
        />
        {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}

        <button type="submit" disabled={submitting}>
          {submitting ? 'Registrando...' : 'Registrarme'}
        </button>
      </form>
    </main>
  )
}
