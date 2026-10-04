CREATE TABLE IF NOT EXISTS usuarios (
                                        id BIGSERIAL PRIMARY KEY,
                                        email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(30) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
    );

CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id BIGSERIAL PRIMARY KEY,
                                              usuario_id BIGINT NOT NULL,
                                              token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                             revoked BOOLEAN NOT NULL DEFAULT FALSE,

                             CONSTRAINT fk_refresh_token_usuario
                             FOREIGN KEY (usuario_id)
    REFERENCES usuarios(id)
                         ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS proyectos (
                                         id BIGSERIAL PRIMARY KEY,
                                         nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_por BIGINT NOT NULL,

    CONSTRAINT fk_proyecto_creador
    FOREIGN KEY (creado_por)
    REFERENCES usuarios(id)
    );

CREATE TABLE IF NOT EXISTS proyecto_miembros (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 proyecto_id BIGINT NOT NULL,
                                                 usuario_id BIGINT NOT NULL,
                                                 rol VARCHAR(20) NOT NULL,

    CONSTRAINT fk_miembro_proyecto
    FOREIGN KEY (proyecto_id)
    REFERENCES proyectos(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_miembro_usuario
    FOREIGN KEY (usuario_id)
    REFERENCES usuarios(id)
    ON DELETE CASCADE,

    CONSTRAINT uq_proyecto_usuario
    UNIQUE (proyecto_id, usuario_id),

    CONSTRAINT chk_rol
    CHECK (rol IN ('OWNER', 'COLLABORATOR', 'VIEWER'))
    );

