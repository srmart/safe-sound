# Safe & Sound

Safe & Sound es una aplicación web orientada a artistas y productores para colaborar en proyectos musicales, gestionando archivos, versiones, roles y comunicación de forma centralizada.

## 👥 Equipo de Desarrollo
- Martín Bentura
- Santiago Martínez
- Agustina Pereyra
- Gastón Suárez
- Mateo Yavitz

## 📋 Requisitos

### Para todo el equipo:
- **Docker** y **Docker Compose** (para la base de datos PostgreSQL)

### Para trabajar en el Backend:
- **Java 21 JDK** ([Descargar](https://adoptium.net/))
- El proyecto incluye Maven Wrapper (`mvnw`), no es necesario instalar Maven globalmente

### Para trabajar en el Frontend:
- **Node.js** (versión LTS recomendada: v20.x o v22.x) ([Descargar](https://nodejs.org/))
- Esto incluye `npm` para gestionar dependencias

## 🚀 Configuración y Ejecución

### 1. Base de Datos (PostgreSQL)

El proyecto utiliza PostgreSQL mediante Docker. Para levantar la base de datos:

```bash
# Desde la carpeta raíz del proyecto
docker compose up -d