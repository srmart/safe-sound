CREATE TABLE IF NOT EXISTS usuarios (
id BIGSERIAL PRIMARY KEY,
email VARCHAR(255) NOT NULL UNIQUE,
username VARCHAR(30) NOT NULL UNIQUE,
password_hash VARCHAR(255) NOT NULL
);

ALTER TABLE usuarios
ADD COLUMN IF NOT EXISTS foto_perfil TEXT,
ADD COLUMN IF NOT EXISTS notificaciones_habilitadas BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS modo_oscuro BOOLEAN NOT NULL DEFAULT FALSE;



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



CREATE TABLE IF NOT EXISTS archivos_proyecto (
id BIGSERIAL PRIMARY KEY,
proyecto_id BIGINT NOT NULL,
usuario_id BIGINT NOT NULL,
nombre VARCHAR(255) NOT NULL,
ruta VARCHAR(255) NOT NULL,
tamano BIGINT NOT NULL,
fecha_subida DATE NOT NULL,

eliminado BOOLEAN NOT NULL DEFAULT FALSE,
eliminado_en TIMESTAMP WITH TIME ZONE,

CONSTRAINT fk_archivo_proyecto
    FOREIGN KEY (proyecto_id)
    REFERENCES proyectos(id)
    ON DELETE CASCADE,

CONSTRAINT fk_archivo_usuario
    FOREIGN KEY (usuario_id)
    REFERENCES usuarios(id)
    ON DELETE CASCADE

);


ALTER TABLE archivos_proyecto
ADD COLUMN IF NOT EXISTS eliminado BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS eliminado_en TIMESTAMP WITH TIME ZONE;


CREATE INDEX IF NOT EXISTS idx_archivos_proyecto_eliminados
ON archivos_proyecto (eliminado, eliminado_en);



CREATE TABLE IF NOT EXISTS solicitudes_restauracion (
id BIGSERIAL PRIMARY KEY,

archivo_id BIGINT NOT NULL,
proyecto_id BIGINT NOT NULL,
solicitante_id BIGINT NOT NULL,
aprobador_id BIGINT,

estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',

creada_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
aprobada_en TIMESTAMP WITH TIME ZONE,
vence_en TIMESTAMP WITH TIME ZONE,
usada_en TIMESTAMP WITH TIME ZONE,

CONSTRAINT fk_solicitud_restauracion_archivo
    FOREIGN KEY (archivo_id)
    REFERENCES archivos_proyecto(id)
    ON DELETE CASCADE,

CONSTRAINT fk_solicitud_restauracion_proyecto
    FOREIGN KEY (proyecto_id)
    REFERENCES proyectos(id)
    ON DELETE CASCADE,

CONSTRAINT fk_solicitud_restauracion_solicitante
    FOREIGN KEY (solicitante_id)
    REFERENCES usuarios(id),

CONSTRAINT fk_solicitud_restauracion_aprobador
    FOREIGN KEY (aprobador_id)
    REFERENCES usuarios(id),

CONSTRAINT chk_estado_solicitud_restauracion
    CHECK (estado IN (
        'PENDIENTE',
        'APROBADA',
        'RECHAZADA',
        'UTILIZADA',
        'VENCIDA'
    ))

);

CREATE INDEX IF NOT EXISTS idx_solicitudes_restauracion_proyecto_estado
ON solicitudes_restauracion (proyecto_id, estado);

CREATE INDEX IF NOT EXISTS idx_solicitudes_restauracion_archivo
ON solicitudes_restauracion (archivo_id);



CREATE TABLE IF NOT EXISTS registros_auditoria (
id BIGSERIAL PRIMARY KEY,
usuario_id BIGINT NOT NULL,
proyecto_id BIGINT NOT NULL,
operacion VARCHAR(50) NOT NULL,
detalles TEXT,
fecha_hora TIMESTAMP WITH TIME ZONE NOT NULL,

CONSTRAINT fk_auditoria_usuario
    FOREIGN KEY (usuario_id)
    REFERENCES usuarios(id),

CONSTRAINT fk_auditoria_proyecto
    FOREIGN KEY (proyecto_id)
    REFERENCES proyectos(id)

);



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



CREATE TABLE IF NOT EXISTS archivos (
id BIGSERIAL PRIMARY KEY,

proyecto_id BIGINT NOT NULL,
subido_por BIGINT NOT NULL,

nombre_original VARCHAR(255) NOT NULL,
nombre_almacenado UUID NOT NULL UNIQUE,
tipo_mime VARCHAR(100) NOT NULL,
tamanio BIGINT NOT NULL CHECK (tamanio >= 0),
fecha_subida TIMESTAMP WITH TIME ZONE NOT NULL,

CONSTRAINT fk_archivo_proyecto_archivos
    FOREIGN KEY (proyecto_id)
    REFERENCES proyectos(id),

CONSTRAINT fk_archivo_usuario_archivos
    FOREIGN KEY (subido_por)
    REFERENCES usuarios(id)

);

CREATE INDEX IF NOT EXISTS idx_archivos_proyecto
ON archivos (proyecto_id);

CREATE INDEX IF NOT EXISTS idx_archivos_proyecto_usuario
ON archivos (proyecto_id, subido_por);