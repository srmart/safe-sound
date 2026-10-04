// src/App.tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import RegisterPage from './pages/RegisterPage'
import LoginPage from './pages/LoginPage'
import HomePage from './pages/HomePage'
import RequireAuth from './RequireAuth'
import { ProjectList } from './components/Projects/ProjectList'
import { ProjectDetail } from './components/Projects/ProjectDetail'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/login" element={<LoginPage />} />
        
        {/* Ruta principal: Redirige a la lista de proyectos */}
        <Route path="/" element={<Navigate to="/projects" replace />} />

        {/* Rutas protegidas para RF4 y RF6 */}
        <Route
          path="/projects"
          element={
            <RequireAuth>
              <ProjectList />
            </RequireAuth>
          }
        />
        <Route
          path="/projects/:projectId"
          element={
            <RequireAuth>
              <ProjectDetail />
            </RequireAuth>
          }
        />

        {/* Si HomePage es un dashboard general, podés dejarlo en otra ruta como /home */}
        <Route
          path="/home"
          element={
            <RequireAuth>
              <HomePage />
            </RequireAuth>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}

export default App