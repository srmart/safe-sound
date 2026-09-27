import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { logout as logoutRequest, refresh as refreshRequest } from '../api/authApi'
import { getRefreshToken, saveTokens, clearTokens } from '../api/tokenStorage'

export default function HomePage() {
  const navigate = useNavigate()
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function handleLogout() {
    setBusy(true)
    setMessage(null)
    try {
      const refreshToken = getRefreshToken()
      if (refreshToken) {
        await logoutRequest(refreshToken)
      }
    } catch {
      // si el refresh token ya venció, igual limpiamos la sesión local
    } finally {
      clearTokens()
      setBusy(false)
      navigate('/login')
    }
  }

  async function handleRefresh() {
    setBusy(true)
    setMessage(null)
    try {
      const refreshToken = getRefreshToken()
      if (!refreshToken) {
        throw new Error('No hay sesión activa')
      }
      const tokens = await refreshRequest(refreshToken)
      saveTokens(tokens)
      setMessage('Sesión renovada correctamente.')
    } catch {
      clearTokens()
      navigate('/login')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-form">
        <h1>Sesión iniciada</h1>
        {message && (
          <p className="alert alert-success" role="status">
            {message}
          </p>
        )}
        <button type="button" onClick={handleRefresh} disabled={busy}>
          Renovar sesión
        </button>
        <button type="button" onClick={handleLogout} disabled={busy}>
          Cerrar sesión
        </button>
      </div>
    </main>
  )
}
