# Safe & Sound

Safe & Sound es una aplicación web orientada a artistas y productores para colaborar en proyectos musicales.

## Requisitos

- Java 21
- Docker
- Docker Compose

## Configuración

El proyecto utiliza PostgreSQL mediante Docker.

Crear las variables de entorno tomando como referencia `.env.example`.

Para levantar la base de datos:

```bash
docker compose up -d
```

Luego ejecutar el backend de Spring Boot.