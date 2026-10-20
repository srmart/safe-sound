
import { useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LandingPage from './pages/LandingPage'
import RegisterPage from './pages/RegisterPage'
import LoginPage from './pages/LoginPage'
import HomePage from './pages/HomePage'
import RequireAuth from './RequireAuth'

import { ProjectList } from './components/Projects/ProjectList'
import { ProjectDetail } from './components/Projects/ProjectDetail'
import ProfilePage from './pages/ProfilePage'

import { perfilApi } from './api/perfil'
import { getAccessToken } from './api/tokenStorage'

import './App.css'

function App() {
    useEffect(() => {
        // Modo claro por defecto.
        document.documentElement.dataset.theme = 'light'

        // Si no hay sesión activa, mantener el modo claro.
        if (!getAccessToken()) {
            return
        }

        let cancelled = false

        // Recuperar la preferencia guardada del usuario.
        async function loadTheme() {
            try {
                const perfil = await perfilApi.obtener()

                if (!cancelled) {
                    document.documentElement.dataset.theme =
                        perfil.modoOscuro ? 'dark' : 'light'
                }
            } catch (error) {
                console.error(
                    'No se pudo recuperar la preferencia de tema:',
                    error
                )
            }
        }

        void loadTheme()

        return () => {
            cancelled = true
        }
    }, [])

    return (
        <BrowserRouter>
            <Routes>
                {/* Registro */}
                <Route
                    path="/register"
                    element={<RegisterPage />}
                />

                {/* Inicio de sesión */}
                <Route
                    path="/login"
                    element={<LoginPage />}
                />

                {/* Página de bienvenida pública */}
                <Route
                    path="/"
                    element={<LandingPage />}
                />

                {/* Listado de proyectos */}
                <Route
                    path="/projects"
                    element={
                        <RequireAuth>
                            <ProjectList />
                        </RequireAuth>
                    }
                />

                {/* Detalle de proyecto */}
                <Route
                    path="/projects/:projectId"
                    element={
                        <RequireAuth>
                            <ProjectDetail />
                        </RequireAuth>
                    }
                />

                {/* Página principal */}
                <Route
                    path="/home"
                    element={
                        <RequireAuth>
                            <HomePage />
                        </RequireAuth>
                    }
                />

                {/* Perfil del usuario */}
                <Route
                    path="/profile"
                    element={
                        <RequireAuth>
                            <ProfilePage />
                        </RequireAuth>
                    }
                />
            </Routes>
        </BrowserRouter>
    )
}

export default App
