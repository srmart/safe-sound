import { useEffect, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { PerfilApiError, perfilApi, type Perfil } from '../api/perfil'
import { clearTokens } from '../api/tokenStorage'
import './ProfilePage.css'

const MAX_IMAGE_SIZE = 1_000_000
const SUPPORTED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])

export default function ProfilePage() {
  const navigate = useNavigate()
  const inputRef = useRef<HTMLInputElement>(null)
  const [perfil, setPerfil] = useState<Perfil | null>(null)
  const [username, setUsername] = useState('')
  const [fotoPerfil, setFotoPerfil] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [savingProfile, setSavingProfile] = useState(false)
  const [savingPreferences, setSavingPreferences] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void loadProfile()
  }, [])

  useEffect(() => {
    if (perfil) applyTheme(perfil.modoOscuro)
  }, [perfil])

  async function loadProfile() {
    setLoading(true)
    try {
      const result = await perfilApi.obtener()
      setPerfil(result)
      setUsername(result.username)
      setFotoPerfil(result.fotoPerfil)
    } catch (requestError) {
      handleRequestError(requestError)
    } finally {
      setLoading(false)
    }
  }

  async function saveProfile() {
    if (!username.trim()) {
      setError('Ingresá un nombre de usuario.')
      return
    }

    setSavingProfile(true)
    setError(null)
    setMessage(null)
    try {
      const result = await perfilApi.actualizar(username.trim(), fotoPerfil)
      setPerfil(result)
      setUsername(result.username)
      setFotoPerfil(result.fotoPerfil)
      setMessage('Tu perfil se actualizó correctamente.')
    } catch (requestError) {
      handleRequestError(requestError)
    } finally {
      setSavingProfile(false)
    }
  }

  async function updatePreferences(nextNotifications: boolean, nextDarkMode: boolean) {
    if (!perfil) return
    setSavingPreferences(true)
    setError(null)
    setMessage(null)
    applyTheme(nextDarkMode)
    setPerfil({ ...perfil, notificacionesHabilitadas: nextNotifications, modoOscuro: nextDarkMode })

    try {
      const result = await perfilApi.actualizarPreferencias(nextNotifications, nextDarkMode)
      setPerfil(result)
      setMessage('Tus preferencias fueron guardadas.')
    } catch (requestError) {
      applyTheme(perfil.modoOscuro)
      setPerfil(perfil)
      handleRequestError(requestError)
    } finally {
      setSavingPreferences(false)
    }
  }

  function handleImageChange(event: ChangeEvent<HTMLInputElement>) {
    const image = event.target.files?.[0]
    if (!image) return

    if (!SUPPORTED_IMAGE_TYPES.has(image.type)) {
      setError('Elegí una imagen PNG, JPEG o WEBP.')
      return
    }

    if (image.size > MAX_IMAGE_SIZE) {
      setError('La imagen debe pesar menos de 1 MB.')
      return
    }

    const reader = new FileReader()
    reader.onload = () => {
      setFotoPerfil(typeof reader.result === 'string' ? reader.result : null)
      setError(null)
      setMessage(null)
    }
    reader.readAsDataURL(image)
  }

  function handleRequestError(requestError: unknown) {
    if (requestError instanceof PerfilApiError && requestError.status === 401) {
      clearTokens()
      navigate('/login', { replace: true })
      return
    }
    setError(requestError instanceof Error ? requestError.message : 'No se pudo completar la operación.')
  }

  if (loading) {
    return <main className="workspace"><div className="profile-loading"><span className="loading-orb" /> Cargando tu perfil…</div></main>
  }

  if (!perfil) {
    return <main className="workspace"><div className="profile-loading">No pudimos cargar tu perfil. <button className="quiet-action" type="button" onClick={() => void loadProfile()}>Reintentar</button></div></main>
  }

  return (
    <main className="workspace profile-page">
      <header className="workspace-bar">
        <button className="brand" type="button" onClick={() => navigate('/projects')}>
          <span className="brand__mark" aria-hidden="true">⌁</span><span>Safe &amp; Sound</span>
        </button>
        <button className="profile-back" type="button" onClick={() => navigate('/projects')}>← Proyectos</button>
      </header>

      <section className="profile-shell">
        <header className="profile-heading">
          <p className="eyebrow">Cuenta y preferencias</p>
          <h1>Tu espacio, a tu manera.</h1>
          <p>Actualizá la información que ve tu equipo y elegí cómo querés recibir novedades de tus proyectos.</p>
        </header>

        {message && <p className="profile-alert profile-alert--success" role="status">{message}</p>}
        {error && <p className="profile-alert profile-alert--error" role="alert">{error}</p>}

        <div className="profile-layout">
          <section className="profile-card profile-card--identity" aria-labelledby="profile-title">
            <div className="profile-card__heading"><div><p className="eyebrow">Identidad</p><h2 id="profile-title">Perfil público</h2></div><span className="profile-card__number">01</span></div>
            <div className="avatar-editor">
              <button className="avatar" type="button" aria-label="Cambiar foto de perfil" onClick={() => inputRef.current?.click()}>
                {fotoPerfil ? <img src={fotoPerfil} alt="Foto de perfil" /> : initials(perfil.username)}
                <span className="avatar__edit" aria-hidden="true">↗</span>
              </button>
              <div><strong>{perfil.username}</strong><span>{perfil.email}</span><button className="text-button" type="button" onClick={() => inputRef.current?.click()}>Cambiar foto</button></div>
              <input ref={inputRef} className="sr-only" type="file" accept="image/png,image/jpeg,image/webp" onChange={handleImageChange} />
            </div>
            <div className="profile-field">
              <label htmlFor="profile-username">Nombre de usuario</label>
              <input id="profile-username" value={username} maxLength={30} onChange={(event) => setUsername(event.target.value)} />
              <span>Usá entre 3 y 30 caracteres: letras, números o guion bajo.</span>
            </div>
            <div className="profile-actions">
              {fotoPerfil && <button className="text-button text-button--muted" type="button" onClick={() => setFotoPerfil(null)}>Quitar foto</button>}
              <button className="primary-action" type="button" disabled={savingProfile} onClick={() => void saveProfile()}>{savingProfile ? 'Guardando…' : 'Guardar perfil'}</button>
            </div>
          </section>

          <section className="profile-card profile-card--preferences" aria-labelledby="preferences-title">
            <div className="profile-card__heading"><div><p className="eyebrow">Preferencias</p><h2 id="preferences-title">Mantenerte al tanto</h2></div><span className="profile-card__number">02</span></div>
            <PreferenceToggle
              label="Notificaciones de proyectos"
              description="Avisos dentro de la aplicación sobre nuevos archivos y cambios de estado en tus proyectos."
              checked={perfil.notificacionesHabilitadas}
              disabled={savingPreferences}
              onChange={(checked) => void updatePreferences(checked, perfil.modoOscuro)}
            />
            <PreferenceToggle
              label="Modo oscuro"
              description="Usá una interfaz de bajo contraste para trabajar con mayor comodidad."
              checked={perfil.modoOscuro}
              disabled={savingPreferences}
              onChange={(checked) => void updatePreferences(perfil.notificacionesHabilitadas, checked)}
            />
            <p className="preference-note"><span aria-hidden="true">✦</span> Los cambios se guardan automáticamente.</p>
          </section>
        </div>
      </section>
    </main>
  )
}

function PreferenceToggle({ label, description, checked, disabled, onChange }: { label: string; description: string; checked: boolean; disabled: boolean; onChange: (checked: boolean) => void }) {
  return <label className="preference-row"><span><strong>{label}</strong><small>{description}</small></span><input type="checkbox" checked={checked} disabled={disabled} onChange={(event) => onChange(event.target.checked)} /><i aria-hidden="true" /></label>
}

function initials(username: string) {
  return username.slice(0, 2).toUpperCase()
}

function applyTheme(darkMode: boolean) {
  document.documentElement.dataset.theme = darkMode ? 'dark' : 'light'
}
