import { useState } from 'react'
import type { SubmitEvent } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import {
  login,
  LoginValidationError,
  CredencialesInvalidasError,
  type LoginFieldErrors,
} from '../api/authApi'
import { saveTokens } from '../api/tokenStorage'

export default function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<LoginFieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError(null)
    setFieldErrors({})
    setSubmitting(true)

    try {
      const tokens = await login({ email, password })
      saveTokens(tokens)
      navigate('/')
    } catch (error) {
      if (error instanceof LoginValidationError) {
        setFieldErrors(error.fieldErrors)
      } else if (error instanceof CredencialesInvalidasError) {
        setFormError(error.message)
      } else {
        setFormError('No se pudo iniciar sesión. Intentá nuevamente.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <h1>Iniciar sesión</h1>

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
          {submitting ? 'Ingresando...' : 'Ingresar'}
        </button>

        <p className="auth-switch">
          ¿No tenés cuenta? <Link to="/register">Registrate</Link>
        </p>
      </form>
    </main>
  )
}
