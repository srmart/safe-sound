# Safe & Sound

Safe & Sound es una aplicación web orientada a artistas y productores, pensada para facilitar la colaboración en proyectos musicales. La aplicación permite gestionar proyectos, usuarios, roles y, a futuro, los archivos y versiones asociados a cada proyecto.

El proyecto fue desarrollado para la materia **Desarrollo Seguro** de la **Facultad de Ingeniería de la Universidad de Montevideo**, teniendo en cuenta tanto las funcionalidades de la aplicación como aspectos de autenticación, autorización y control de acceso.

## Equipo de Desarrollo

* Martín Bentura
* Santiago Martínez
* Agustina Pereyra
* Gastón Suárez
* Mateo Yavitz

## Requisitos

### Para todo el equipo

* **Docker** y **Docker Compose**, utilizados para levantar la base de datos PostgreSQL.
* **Git** para trabajar con el repositorio.

### Para trabajar en el Backend

* **Java 21 JDK** ([Descargar](https://adoptium.net/))
* El proyecto incluye Maven Wrapper (`mvnw`), por lo que no es necesario instalar Maven de forma global.

### Para trabajar en el Frontend

* **Node.js** ([Descargar](https://nodejs.org/))
* `npm`, que viene incluido con Node.js.

Se recomienda utilizar una versión LTS de Node.js compatible con las dependencias del proyecto.

---

## Configuración y Ejecución

### 1. Base de datos

La aplicación utiliza PostgreSQL como base de datos y se levanta mediante Docker Compose.

Desde la carpeta raíz del proyecto:

```bash
docker compose up -d
```

Para comprobar que el contenedor esté funcionando:

```bash
docker compose ps
```

También se pueden consultar los logs de PostgreSQL con:

```bash
docker compose logs postgres
```

La base de datos utiliza el puerto `5433` en el equipo local para evitar conflictos con instalaciones de PostgreSQL que puedan estar utilizando el puerto `5432`.

### 2. Variables de entorno

El proyecto utiliza variables de entorno para configurar la conexión a la base de datos y la autenticación.

Se debe crear un archivo `.env` en la raíz del proyecto:

```env
POSTGRES_DB=safe_and_sound
POSTGRES_USER=safeandsound
POSTGRES_PASSWORD=change_me
POSTGRES_PORT=5433

DB_APP_USER=safeandsound
DB_APP_PASSWORD=change_me

JWT_SECRET=YOUR_BASE64_SECRET
```

El archivo `.env` contiene información que no debería quedar expuesta en el repositorio, por lo que debe mantenerse fuera del control de versiones.

Si el backend se ejecuta directamente desde PowerShell, las variables también deben estar disponibles en el entorno. Por ejemplo:

```powershell
$env:POSTGRES_PORT="5433"
$env:POSTGRES_DB="safe_and_sound"
$env:POSTGRES_USER="safeandsound"
$env:POSTGRES_PASSWORD="change_me"
$env:DB_APP_USER="safeandsound"
$env:DB_APP_PASSWORD="change_me"
$env:JWT_SECRET="YOUR_BASE64_SECRET"
```

### 3. Ejecutar el Backend

Desde la carpeta `backend/`:

```powershell
.\mvnw.cmd spring-boot:run
```

El backend queda disponible en:

```text
http://localhost:8080
```

### 4. Ejecutar el Frontend

Desde la carpeta `frontend/`:

```powershell
npm install
npm run dev
```

El frontend se ejecuta mediante Vite y normalmente queda disponible en:

```text
http://localhost:5173
```

---

## Arquitectura

El proyecto está dividido principalmente en tres partes:

```text
┌──────────────────────────┐
│         Frontend         │
│   React + TypeScript     │
│          + Vite          │
└────────────┬─────────────┘
             │
             │ HTTP / REST
             ▼
┌──────────────────────────┐
│         Backend          │
│       Spring Boot        │
│    Spring Security + JWT │
└────────────┬─────────────┘
             │
             │ JPA / Hibernate
             ▼
┌──────────────────────────┐
│       PostgreSQL         │
│        Database          │
└──────────────────────────┘
```

El frontend se encarga de la interfaz y de comunicarse con la API.

El backend concentra la lógica de negocio, autenticación y autorización.

PostgreSQL se utiliza para almacenar la información de los usuarios, proyectos, membresías y demás datos necesarios para la aplicación.

---

## Tecnologías utilizadas

### Backend

| Tecnología    | Uso                                 |
|---------------| ----------------------------------- |
| Java 21       | Lenguaje de programación            |
| Spring Boot   | Framework principal                 |
| Spring Web MVC | Desarrollo de la API REST           |
| Spring Security | Autenticación y seguridad           |
| Spring Data JPA | Persistencia                        |
| Hibernate     | ORM                                 |
| PostgreSQL    | Base de datos                       |
| JWT           | Generación y validación de JWT      |
| Maven         | Gestión del proyecto y dependencias |

### Frontend

| Tecnología    | Uso                                           |
| ------------- | --------------------------------------------- |
| React         | Interfaz de usuario                           |
| TypeScript    | Lenguaje utilizado en el frontend             |
| React Router  | Navegación entre páginas                      |
| Vite          | Servidor de desarrollo y herramienta de build |
| Node.js / npm | Entorno y gestión de dependencias             |

### Otras herramientas

* Docker
* Docker Compose
* Git
* GitHub
* OWASP Dependency-Check

---

## Estructura del proyecto

```text
safe-and-sound/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/safeandsound/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── exception/
│   │   │   │   ├── model/
│   │   │   │   ├── repository/
│   │   │   │   ├── security/
│   │   │   │   └── service/
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   ├── test/
│   │   ├── pom.xml
│   │   └── mvnw
│   │
│   └── ...
│
├── database/
│   ├── init.sql
│   └── init-app-user.sh
│
├── frontend/
│   └── ...
│
├── docs/
│
├── docker-compose.yml
└── .gitignore
```

---

## Autenticación

La autenticación se realiza mediante **JSON Web Tokens (JWT)**.

Al iniciar sesión, el backend genera dos tokens:

* Access Token
* Refresh Token

El Access Token se utiliza para acceder a los endpoints que requieren autenticación.

El Refresh Token permite solicitar un nuevo Access Token cuando sea necesario.

El flujo básico es:

```text
Usuario
   │
   │ Login
   ▼
Backend
   │
   ├── Access Token
   └── Refresh Token
   │
   ▼
Usuario
   │
   │ Request + Access Token
   ▼
Backend
   │
   └── Verifica autenticación y permisos
```

Los refresh tokens se almacenan en el backend y pueden ser revocados.

---

## Roles y permisos

Los usuarios pueden tener diferentes roles dentro de cada proyecto:

| Rol            | Descripción                                                               |
| -------------- | ------------------------------------------------------------------------- |
| `OWNER`        | Es el propietario del proyecto y tiene permisos de administración.        |
| `COLLABORATOR` | Puede trabajar dentro del proyecto de acuerdo con los permisos definidos. |
| `VIEWER`       | Tiene acceso de consulta al proyecto.                                     |

La relación entre usuarios y proyectos se maneja mediante la tabla `proyecto_miembros`.

```text
Usuario
   │
   ▼
ProyectoMiembro
   │
   ├── rol
   │
   ▼
Proyecto
```

El rol de cada usuario se obtiene desde la información almacenada en el backend.

Las decisiones de autorización se realizan del lado del servidor y no dependen de un rol enviado por el cliente.

---

## API

Algunos de los endpoints principales son:

| Método | Endpoint         | Descripción                       |
| ------ | ---------------- | --------------------------------- |
| `POST` | `/auth/register` | Registrar un usuario              |
| `POST` | `/auth/login`    | Iniciar sesión                    |
| `POST` | `/auth/refresh`  | Renovar los tokens                |
| `POST` | `/auth/logout`   | Cerrar sesión                     |
| `GET`  | `/proyectos`     | Obtener los proyectos del usuario |
| `POST` | `/proyectos`     | Crear un proyecto                 |

Los endpoints que requieren autenticación utilizan un JWT mediante el siguiente header:

```http
Authorization: Bearer <token>
```

---

## Base de datos

El proyecto utiliza **PostgreSQL**.

Entre las principales tablas se encuentran:

### `usuarios`

Contiene la información de los usuarios registrados en el sistema.

### `proyectos`

Contiene la información de los proyectos.

Algunos de sus campos son:

* `id`
* `nombre`
* `descripcion`
* `activo`
* `creado_por`

### `proyecto_miembros`

Relaciona usuarios con proyectos y guarda el rol que tiene cada usuario dentro del proyecto.

Algunos de sus campos son:

* `id`
* `proyecto_id`
* `usuario_id`
* `rol`

Esto permite que un mismo usuario pueda tener diferentes roles dependiendo del proyecto al que pertenezca.

---

## Testing

Para probar la API se pueden utilizar herramientas como PowerShell, Postman o Insomnia.

### Registro

```powershell
$body = @{
    email = "test@safeandsound.com"
    username = "testuser"
    password = "password123"
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:8080/auth/register" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

### Login

```powershell
$body = @{
    email = "test@safeandsound.com"
    password = "password123"
} | ConvertTo-Json

$response = Invoke-RestMethod `
    -Uri "http://localhost:8080/auth/login" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body

$token = $response.accessToken
```

### Obtener proyectos

```powershell
$resultado = Invoke-RestMethod `
    -Uri "http://localhost:8080/proyectos" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $token" }

$resultado | ConvertTo-Json
```

---

## Análisis de dependencias

El proyecto utiliza **OWASP Dependency-Check** para revisar las dependencias utilizadas por el backend y detectar vulnerabilidades conocidas.

Para ejecutar el análisis:

```powershell
.\mvnw.cmd org.owasp:dependency-check-maven:check
```

Las vulnerabilidades encontradas deben revisarse y, cuando corresponda, actualizar las dependencias afectadas.

En caso de utilizar una API key de NVD, esta debe mantenerse fuera del repositorio mediante una variable de entorno:

```powershell
$env:NVD_API_KEY="YOUR_NVD_API_KEY"
```

---

## Gestión de archivos

La interfaz permite consultar y descargar los archivos de un proyecto. Los roles `OWNER` y `COLLABORATOR` también pueden cargar y eliminar archivos, mientras que `VIEWER` dispone únicamente de consulta y descarga.

La persistencia y las operaciones remotas de archivos requieren los endpoints correspondientes del backend. Mientras se desarrolla esa integración, el frontend conserva las modificaciones de archivos durante la sesión del navegador.

---

## Desarrollo

Para obtener el proyecto:

```powershell
git clone https://github.com/srmart/safe-sound.git
cd safe-and-sound
```

La rama utilizada para el desarrollo es:

```text
Development
```


---

## Contexto académico

Proyecto desarrollado para la materia **Desarrollo Seguro** de la **Facultad de Ingeniería de la Universidad de Montevideo**.

El objetivo del proyecto es aplicar conceptos de desarrollo web y buenas prácticas de seguridad, trabajando con autenticación, autorización, roles y control de acceso dentro de una aplicación web.
