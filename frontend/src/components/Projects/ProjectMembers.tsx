
import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { getAccessToken } from '../../api/tokenStorage'
import './ProjectMembers.css'

type Rol = 'OWNER' | 'COLLABORATOR' | 'VIEWER'
type RolEditable = 'COLLABORATOR' | 'VIEWER'
type Accion = 'agregar' | 'modificar'

interface Miembro {
    usuarioId: number
    username: string
    rol: Rol
}

interface Props {
    projectId: string
}

const baseUrl = (
    import.meta.env.VITE_API_URL || 'http://localhost:8080'
).replace(/\/$/, '')

const nombreRol: Record<Rol, string> = {
    OWNER: 'Propietario',
    COLLABORATOR: 'Colaborador',
    VIEWER: 'Solo lectura',
}

function iniciales(nombre: string): string {
    return nombre.slice(0, 2).toUpperCase()
}

export function ProjectMembers({ projectId }: Props) {
    const [miembros, setMiembros] = useState<Miembro[]>([])
    const [cargandoMiembros, setCargandoMiembros] = useState(true)
    const [errorListado, setErrorListado] = useState('')

    const [usuarioId, setUsuarioId] = useState('')
    const [rol, setRol] = useState<RolEditable>('COLLABORATOR')
    const [accion, setAccion] = useState<Accion>('agregar')
    const [password, setPassword] = useState('')
    const [loading, setLoading] = useState(false)
    const [mensaje, setMensaje] = useState('')
    const [error, setError] = useState('')

    const cargarMiembros = useCallback(async (signal?: AbortSignal) => {
        const token = getAccessToken()

        if (!token) {
            setErrorListado('Tu sesión expiró. Iniciá sesión nuevamente.')
            setCargandoMiembros(false)
            return
        }

        setCargandoMiembros(true)
        setErrorListado('')

        try {
            const response = await fetch(
                `${baseUrl}/proyectos/${projectId}/miembros`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                    signal,
                }
            )

            if (!response.ok) {
                if (response.status === 403) {
                    throw new Error('No tenés permiso para ver los miembros.')
                }
                throw new Error('No se pudo cargar la lista de miembros.')
            }

            const data: Miembro[] = await response.json()

            if (!signal?.aborted) {
                setMiembros(data)
            }
        } catch (err) {
            if (!signal?.aborted) {
                setErrorListado(
                    err instanceof Error
                        ? err.message
                        : 'Ocurrió un error al cargar los miembros.'
                )
            }
        } finally {
            if (!signal?.aborted) {
                setCargandoMiembros(false)
            }
        }
    }, [projectId])

    useEffect(() => {
        const controller = new AbortController()
        void cargarMiembros(controller.signal)

        return () => controller.abort()
    }, [cargarMiembros])

    function seleccionarMiembro(miembro: Miembro) {
        if (miembro.rol === 'OWNER') return

        setAccion('modificar')
        setUsuarioId(String(miembro.usuarioId))
        setRol(miembro.rol)
        setPassword('')
        setError('')
        setMensaje('')
    }

    function cambiarAccion(nuevaAccion: Accion) {
        setAccion(nuevaAccion)
        setUsuarioId('')
        setRol('COLLABORATOR')
        setPassword('')
        setError('')
        setMensaje('')
    }

    async function handleSubmit(e: FormEvent<HTMLFormElement>) {
        e.preventDefault()
        setMensaje('')
        setError('')

        if (!/^[1-9]\d*$/.test(usuarioId.trim())) {
            setError('Ingresá un ID de usuario válido.')
            return
        }

        if (!password) {
            setError('Ingresá tu contraseña para confirmar.')
            return
        }

        const token = getAccessToken()

        if (!token) {
            setError('Tu sesión expiró. Iniciá sesión nuevamente.')
            return
        }

        const params = new URLSearchParams({
            rol,
            passwordConfirmacion: password,
        })

        const ruta = `${baseUrl}/proyectos/${projectId}/miembros/${usuarioId.trim()}`
        const url = accion === 'agregar'
            ? `${ruta}?${params.toString()}`
            : `${ruta}/rol?${params.toString()}`

        setLoading(true)

        try {
            const response = await fetch(url, {
                method: accion === 'agregar' ? 'POST' : 'PUT',
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            })

            if (!response.ok) {
                if (response.status === 401) {
                    throw new Error('Sesión expirada o contraseña incorrecta.')
                }
                if (response.status === 403) {
                    throw new Error('No tenés permisos o la confirmación fue rechazada.')
                }
                if (response.status === 404) {
                    throw new Error('No se encontró el proyecto o el usuario.')
                }
                if (response.status === 409) {
                    throw new Error('El usuario ya pertenece al proyecto.')
                }

                throw new Error(
                    `No se pudo completar la operación (HTTP ${response.status}).`
                )
            }

            setMensaje(
                accion === 'agregar'
                    ? '¡Miembro agregado correctamente!'
                    : '¡Rol actualizado correctamente!'
            )

            setUsuarioId('')
            setPassword('')

            await cargarMiembros()
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : 'Ocurrió un error inesperado.'
            )
        } finally {
            setPassword('')
            setLoading(false)
        }
    }

    return (
        <section className="members-panel">
            <div className="members-panel__header">
                <div>
                    <p className="members-panel__eyebrow">COLABORACIÓN</p>
                    <h2>Miembros del proyecto</h2>
                    <p className="members-panel__description">
                        Las personas que forman parte de este espacio musical.
                    </p>
                </div>

                <span className="members-panel__icon" aria-hidden="true">
                    ♫
                </span>
            </div>

            <div className="members-list">
                <div className="members-list__heading">
                    <h3>Integrantes</h3>
                    {!cargandoMiembros && !errorListado && (
                        <span className="members-list__count">
                            {miembros.length}{' '}
                            {miembros.length === 1 ? 'miembro' : 'miembros'}
                        </span>
                    )}
                </div>

                {cargandoMiembros && (
                    <p className="members-list__status">
                        Cargando integrantes...
                    </p>
                )}

                {errorListado && (
                    <div className="members-list__error">
                        <p>{errorListado}</p>
                        <button
                            type="button"
                            onClick={() => void cargarMiembros()}
                        >
                            Reintentar
                        </button>
                    </div>
                )}

                {!cargandoMiembros &&
                    !errorListado &&
                    miembros.length === 0 && (
                        <p className="members-list__status">
                            Todavía no hay miembros para mostrar.
                        </p>
                    )}

                {!cargandoMiembros &&
                    !errorListado &&
                    miembros.map(miembro => (
                        <div
                            className="member-card"
                            key={miembro.usuarioId}
                        >
                            <div
                                className={`member-card__avatar ${
                                    miembro.rol === 'OWNER'
                                        ? 'member-card__avatar--owner'
                                        : ''
                                }`}
                            >
                                {iniciales(miembro.username)}
                            </div>

                            <div className="member-card__identity">
                                <strong>{miembro.username}</strong>
                                <span>ID #{miembro.usuarioId}</span>
                            </div>

                            <span
                                className={`member-card__role member-card__role--${miembro.rol.toLowerCase()}`}
                            >
                                {nombreRol[miembro.rol]}
                            </span>

                            {miembro.rol !== 'OWNER' && (
                                <button
                                    type="button"
                                    className="member-card__edit"
                                    onClick={() => seleccionarMiembro(miembro)}
                                >
                                    Cambiar rol
                                </button>
                            )}
                        </div>
                    ))}
            </div>

            <div className="members-panel__management">
                <div className="members-panel__management-heading">
                    <h3>Gestionar miembros</h3>
                    <p>
                        Agregá usuarios por su ID o modificá sus permisos.
                    </p>
                </div>

                <div className="members-panel__tabs">
                    <button
                        type="button"
                        className={accion === 'agregar' ? 'active' : ''}
                        onClick={() => cambiarAccion('agregar')}
                    >
                        Agregar miembro
                    </button>

                    <button
                        type="button"
                        className={accion === 'modificar' ? 'active' : ''}
                        onClick={() => cambiarAccion('modificar')}
                    >
                        Cambiar rol
                    </button>
                </div>

                <form
                    className="members-panel__form"
                    onSubmit={handleSubmit}
                >
                    <div className="members-panel__field">
                        <label htmlFor="member-user-id">
                            ID del usuario
                        </label>
                        <input
                            id="member-user-id"
                            type="text"
                            inputMode="numeric"
                            placeholder="Ej. 54"
                            value={usuarioId}
                            onChange={e => setUsuarioId(e.target.value)}
                            required
                        />
                    </div>

                    <div className="members-panel__field">
                        <label htmlFor="member-role">
                            Rol del usuario
                        </label>
                        <select
                            id="member-role"
                            value={rol}
                            onChange={e =>
                                setRol(e.target.value as RolEditable)
                            }
                        >
                            <option value="COLLABORATOR">
                                Colaborador
                            </option>
                            <option value="VIEWER">
                                Solo lectura
                            </option>
                        </select>
                    </div>

                    <div className="members-panel__field members-panel__full">
                        <label htmlFor="member-password">
                            Confirmá tu contraseña
                        </label>
                        <input
                            id="member-password"
                            type="password"
                            placeholder="Tu contraseña"
                            autoComplete="current-password"
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            required
                        />
                        <small>
                            Por seguridad, necesitamos verificar tu identidad
                            antes de realizar esta acción.
                        </small>
                    </div>

                    {error && (
                        <p
                            className="members-panel__message members-panel__message--error"
                            role="alert"
                        >
                            {error}
                        </p>
                    )}

                    {mensaje && (
                        <p
                            className="members-panel__message members-panel__message--success"
                            role="status"
                        >
                            {mensaje}
                        </p>
                    )}

                    <button
                        className="members-panel__submit"
                        type="submit"
                        disabled={loading}
                    >
                        {loading
                            ? 'Procesando...'
                            : accion === 'agregar'
                                ? '+ Agregar miembro'
                                : 'Guardar nuevo rol'}
                    </button>
                </form>
            </div>
        </section>
    )
}
