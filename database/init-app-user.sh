#!/bin/bash
set -euo pipefail

# Comprobar que las variables estén configuradas
: "${POSTGRES_USER:?POSTGRES_USER no configurado}"
: "${POSTGRES_DB:?POSTGRES_DB no configurado}"
: "${DB_APP_USER:?DB_APP_USER no configurado}"
: "${DB_APP_PASSWORD:?DB_APP_PASSWORD no configurado}"

# El backend no puede utilizar la cuenta administradora
if [[ "$DB_APP_USER" == "$POSTGRES_USER" ]]; then
    echo "ERROR: DB_APP_USER debe ser diferente de POSTGRES_USER" >&2
    exit 1
fi

echo "Configurando cuenta de minimo privilegio para el backend..."

psql -v ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    --set=app_user="$DB_APP_USER" \
    --set=app_password="$DB_APP_PASSWORD" \
    --set=app_database="$POSTGRES_DB" <<'EOSQL'

-- Crear el usuario si todavía no existe
SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION',
    :'app_user',
    :'app_password'
)
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_catalog.pg_roles
    WHERE rolname = :'app_user'
)
\gexec

-- Actualizar sus credenciales y atributos si ya existe
SELECT format(
    'ALTER ROLE %I WITH LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION',
    :'app_user',
    :'app_password'
)
\gexec

-- Permitir conexión a la base
SELECT format(
    'GRANT CONNECT ON DATABASE %I TO %I',
    :'app_database',
    :'app_user'
)
\gexec

-- Permitir uso del esquema
SELECT format(
    'GRANT USAGE ON SCHEMA public TO %I',
    :'app_user'
)
\gexec

-- Permisos de lectura y escritura sobre tablas
SELECT format(
    'GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO %I',
    :'app_user'
)
\gexec

-- Permisos sobre secuencias
SELECT format(
    'GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO %I',
    :'app_user'
)
\gexec

EOSQL

echo "Cuenta del backend configurada correctamente."
