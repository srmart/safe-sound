-- RS19: Tabla de auditoría para operaciones administrativas de alto riesgo
CREATE TABLE IF NOT EXISTS registros_auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    proyecto_id BIGINT NOT NULL,
    operacion VARCHAR(50) NOT NULL,
    detalles TEXT,
    fecha_hora TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_auditoria_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_auditoria_proyecto
        FOREIGN KEY (proyecto_id)
        REFERENCES proyectos(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_auditoria_proyecto_fecha
    ON registros_auditoria (proyecto_id, fecha_hora DESC);