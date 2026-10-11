-- RF7: perfil de usuario y preferencias
-- Ejecutar una vez en bases creadas antes de esta funcionalidad.
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS foto_perfil TEXT,
    ADD COLUMN IF NOT EXISTS notificaciones_habilitadas BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS modo_oscuro BOOLEAN NOT NULL DEFAULT FALSE;
