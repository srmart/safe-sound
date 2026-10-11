-- RS8: registro de intentos de acceso a recursos protegidos.
-- Ejecutar una vez en bases creadas antes de esta funcionalidad.
CREATE TABLE IF NOT EXISTS intentos_acceso_protegido (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    recurso VARCHAR(255) NOT NULL,
    operacion VARCHAR(16) NOT NULL,
    autorizado BOOLEAN NOT NULL,
    fecha_hora TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    codigo_respuesta INTEGER NOT NULL,

    CONSTRAINT fk_intento_acceso_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_intentos_acceso_fecha_hora
    ON intentos_acceso_protegido (fecha_hora DESC);
