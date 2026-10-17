# Safe & Sound

Safe & Sound es una aplicación web orientada a artistas y productores, pensada para facilitar la colaboración en proyectos musicales. Permite registrarse, iniciar sesión, crear y administrar proyectos, gestionar miembros y consultar una biblioteca de archivos por proyecto.

El proyecto fue desarrollado para la materia **Desarrollo Seguro** de la **Facultad de Ingeniería de la Universidad de Montevideo**, teniendo en cuenta tanto las funcionalidades de la aplicación como aspectos de autenticación, autorización, control de acceso y auditoría.

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

### 1. Crear el archivo `.env`

En la raíz del repositorio, crear un archivo llamado `.env` con las siguientes variables:

```env
POSTGRES_DB=safe_and_sound
POSTGRES_USER=safeandsound
POSTGRES_PASSWORD=change_me
POSTGRES_PORT=5433

DB_APP_USER=safeandsound
DB_APP_PASSWORD=change_me

JWT_SECRET=REEMPLAZAR_POR_UN_SECRETO_BASE64
```

`JWT_SECRET` debe ser una cadena Base64 válida. No debe usarse literalmente el texto `REEMPLAZAR_POR_UN_SECRETO_BASE64`.

Para generar una clave válida:

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

```bash
openssl rand -base64 32
```

El archivo `.env` está excluido de Git y debe conservarse localmente. No se deben subir contraseñas ni secretos reales al repositorio.

### 2. Levantar la base de datos

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

#### Problemas de credenciales

PostgreSQL solo aplica `POSTGRES_USER` y `POSTGRES_PASSWORD` la primera vez que se crea el contenedor, cuando el volumen de datos está vacío. Si se cambian esas variables en `.env` después de levantar la base, puede aparecer el error `password authentication failed` porque el volumen conserva las credenciales iniciales.

Si no es necesario conservar los datos locales, se puede recrear la base con las credenciales actuales:

```bash
docker compose down -v
docker compose up -d
```

El flag `-v` elimina el volumen con los datos de PostgreSQL.

La base de datos utiliza el puerto `5433` en el equipo local para evitar conflictos con instalaciones de PostgreSQL que puedan estar utilizando el puerto `5432`.

#### Actualizar una base existente para RF7

Una base creada desde cero recibe los cambios de RF7 automáticamente: `database/init.sql` se ejecuta cuando Docker inicializa un volumen nuevo.

Si ya existía un volumen de PostgreSQL antes de incorporar el perfil de usuario y sus preferencias, ejecutar una vez la migración. No elimina datos:

**macOS, Linux o Git Bash:**

```bash
docker compose exec -T postgres psql -U safeandsound -d safe_and_sound < database/migrate-rf7.sql
```

**Windows PowerShell:**

```powershell
Get-Content database/migrate-rf7.sql | docker compose exec -T postgres psql -U safeandsound -d safe_and_sound
```

La migración agrega `foto_perfil`, `notificaciones_habilitadas` y `modo_oscuro` a la tabla `usuarios`. Es segura de ejecutar más de una vez porque usa `ADD COLUMN IF NOT EXISTS`.

#### Registrar intentos de acceso a recursos protegidos (RS8)

Las instalaciones nuevas crean automáticamente la tabla `intentos_acceso_protegido`. Si la base ya existía, aplicar la migración RS8 una vez, sin eliminar datos:

**macOS, Linux o Git Bash:**

```bash
docker compose exec -T postgres psql -U safeandsound -d safe_and_sound < database/migrate-rs8.sql
```

**Windows PowerShell:**

```powershell
Get-Content database/migrate-rs8.sql | docker compose exec -T postgres psql -U safeandsound -d safe_and_sound
```

### 3. Ejecutar el backend

El backend necesita recibir las variables de `.env` en el proceso que lo ejecuta.

#### Windows PowerShell

```powershell
$env:POSTGRES_PORT="5433"
$env:POSTGRES_DB="safe_and_sound"
$env:POSTGRES_USER="safeandsound"
$env:POSTGRES_PASSWORD="change_me"
$env:DB_APP_USER="safeandsound"
$env:DB_APP_PASSWORD="change_me"
$env:JWT_SECRET="<tu clave Base64 válida>"

cd backend
.\mvnw.cmd spring-boot:run
```

#### macOS o Linux

```bash
cd backend
set -a
source ../.env
set +a
./mvnw spring-boot:run
```

El backend queda disponible en:

```text
http://localhost:8080
```

El arranque correcto termina con un mensaje similar a `Tomcat started on port 8080`. Si se actualizó el código del backend, detener la instancia anterior con `Ctrl + C` y volver a iniciarla para evitar ejecutar clases antiguas.

### 4. Ejecutar el frontend

Desde la carpeta `frontend/`:

```powershell
npm install
npm run dev
```

El frontend se ejecuta mediante Vite y normalmente queda disponible en:

```text
http://localhost:5173
```

Abrir esa dirección en el navegador. Las rutas del frontend son en inglés (`/login`, `/register`, `/projects`); la API del backend utiliza, entre otros, el endpoint `/proyectos`.

---

## Uso de la aplicación

### 1. Crear una cuenta e iniciar sesión

1. Abrir `http://localhost:5173`.
2. Si no existe una cuenta, seleccionar **Registrate**, completar email, nombre de usuario y una contraseña de al menos ocho caracteres.
3. Volver a **Iniciar sesión** e ingresar con las mismas credenciales.
4. Al autenticarse, la aplicación redirige a **Mis proyectos**. El token de acceso se usa para las operaciones protegidas.

Si la sesión vence o el token no es válido, la aplicación borra la sesión local y dirige nuevamente a `/login`.

### 2. Gestionar proyectos

En **Mis proyectos** se puede:

* Crear un proyecto con **Nuevo proyecto**.
* Buscar proyectos por nombre.
* Filtrar por estado: todos, activos o archivados.
* Abrir un proyecto al seleccionar su tarjeta.
* Editar información del proyecto, según los permisos correspondientes.
* Archivar un proyecto cuando se cuenta con autorización.

Cada proyecto muestra el estado y el rol de la persona autenticada.

| Rol                          | Acciones disponibles en la interfaz                                                                              |
| ---------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| Propietario (`OWNER`)        | Crear y archivar proyectos; acceder a los controles de edición; subir, descargar y eliminar archivos.            |
| Colaborador (`COLLABORATOR`) | Trabajar dentro del proyecto de acuerdo con sus permisos, incluyendo las operaciones habilitadas sobre archivos. |
| Solo lectura (`VIEWER`)      | Visualizar y descargar archivos.                                                                                 |

Las operaciones se autorizan en el backend según la identidad autenticada, la membresía del proyecto y los permisos correspondientes. La visibilidad de una acción en el frontend no sustituye la autorización del servidor.

### 3. Gestionar archivos de un proyecto

1. Abrir la tarjeta de un proyecto.
2. Ir a la sección **Archivos**.
3. Con rol Propietario o Colaborador, seleccionar **Subir archivo**, elegir un archivo y confirmar.
4. Usar **Descargar** para obtener un archivo de la lista.
5. Con permisos de modificación, usar **Eliminar** y confirmar la acción.

La interfaz de R6 aplica las acciones según el rol. En esta entrega, la biblioteca de archivos es una implementación de frontend: los archivos de muestra y los cambios de carga o eliminación se mantienen mientras la pantalla está abierta; el almacenamiento permanente y los endpoints de archivos requieren la integración del backend. Del mismo modo, los controles de edición dentro del detalle actualizan la vista abierta.

### Comprobación rápida

Con los tres servicios levantados, este flujo permite comprobar las funcionalidades principales:

```text
Registro → Inicio de sesión → Crear proyecto → Abrir proyecto → Gestionar archivos
```

Si el navegador muestra `401 Unauthorized` al cargar proyectos, verificar que el backend haya sido reiniciado con las variables indicadas arriba e iniciar sesión nuevamente.

### 4. Configurar perfil y preferencias

Desde **Perfil** en la barra superior se puede actualizar el nombre de usuario y cargar una foto de perfil PNG, JPEG o WEBP de hasta 1 MB. También se pueden activar o desactivar las notificaciones dentro de la aplicación y el modo oscuro.

Las preferencias se guardan automáticamente. La foto y el nombre de usuario requieren seleccionar **Guardar perfil**.

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

El backend concentra la lógica de negocio, autenticación, autorización y validaciones de las operaciones solicitadas.

PostgreSQL se utiliza para almacenar la información de los usuarios, proyectos, membresías y demás datos necesarios para la aplicación.

### Separación de módulos

El frontend y el backend se mantienen en directorios separados. Esta organización permite trabajar sobre la interfaz y la lógica del servidor de forma independiente, conservando una API como punto de comunicación entre ambos módulos.

---

## Tecnologías utilizadas

### Backend

| Tecnología      | Uso                                 |
| --------------- | ----------------------------------- |
| Java 21         | Lenguaje de programación            |
| Spring Boot     | Framework principal                 |
| Spring Web MVC  | Desarrollo de la API REST           |
| Spring Security | Autenticación y seguridad           |
| Spring Data JPA | Persistencia                        |
| Hibernate       | ORM                                 |
| PostgreSQL      | Base de datos                       |
| JWT             | Generación y validación de tokens   |
| Maven           | Gestión del proyecto y dependencias |

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
* YouTrack
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
│   ├── init-app-user.sh
│   ├── migrate-rf7.sql
│   └── migrate-rs8.sql
│
├── frontend/
│   └── ...
│
├── docs/
│
├── docker-compose.yml
└── .gitignore
```

El backend organiza las responsabilidades en controladores, servicios, modelos, repositorios y DTOs. La configuración de seguridad se encuentra separada de la lógica de negocio.

---

## Autenticación

La autenticación se realiza mediante **JSON Web Tokens (JWT)**.

Al iniciar sesión, el backend genera dos tokens:

* **Access Token:** se utiliza para acceder a los endpoints que requieren autenticación.
* **Refresh Token:** permite solicitar un nuevo Access Token cuando sea necesario.

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

### Reautenticación para operaciones sensibles

Se incorpora el endpoint `POST /auth/reautenticar`, que permite verificar la contraseña del usuario autenticado antes de continuar con determinadas operaciones sensibles.

La solicitud utiliza el token de acceso y envía la contraseña en el cuerpo de la petición:

```http
POST /auth/reautenticar
Authorization: Bearer <token>
Content-Type: application/json
```

Ejemplo del cuerpo:

```json
{
  "password": "contraseña_del_usuario"
}
```

La verificación comprueba la contraseña ingresada contra la almacenada para el usuario autenticado. Si las credenciales no coinciden, la operación no debe considerarse autorizada por esa confirmación.

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

### Permisos sobre proyectos

Las operaciones de modificación se controlan por tipo de acción. Esto permite aplicar permisos diferentes a campos distintos del mismo proyecto.

| Operación                              | Permiso requerido                |
| -------------------------------------- | -------------------------------- |
| Modificar el nombre del proyecto       | `MODIFICAR_NOMBRE_PROYECTO`      |
| Modificar la descripción del proyecto  | `MODIFICAR_DESCRIPCION_PROYECTO` |
| Archivar un proyecto                   | `ARCHIVAR_PROYECTO`              |
| Agregar miembros o modificar sus roles | `MODIFICAR_ROLES`                |
| Consultar los registros de auditoría   | `VER_AUDITORIA`                  |

Los permisos de modificación del nombre y la descripción se asignan de la siguiente manera:

* **`OWNER`:** puede modificar el nombre y la descripción.
* **`COLLABORATOR`:** puede modificar la descripción, pero no el nombre.
* **`VIEWER`:** no cuenta con esos permisos de modificación.

La autorización de cada operación se verifica en el backend. Además, las rutas no habilitadas explícitamente se mantienen denegadas por defecto.

### Confirmación de contraseña

Las operaciones sensibles contempladas en esta implementación requieren una confirmación de contraseña:

* Editar un proyecto.
* Archivar un proyecto.
* Agregar un miembro a un proyecto.
* Modificar el rol de un miembro.

La confirmación se verifica en el backend antes de ejecutar la operación correspondiente. La reautenticación no sustituye los controles de autorización: un usuario también debe disponer de los permisos necesarios para realizar la acción solicitada.

---

## API

Algunos de los endpoints principales son:

| Método   | Endpoint                                           | Descripción                                                |
| -------- | -------------------------------------------------- | ---------------------------------------------------------- |
| `POST`   | `/auth/register`                                   | Registrar un usuario                                       |
| `POST`   | `/auth/login`                                      | Iniciar sesión                                             |
| `POST`   | `/auth/refresh`                                    | Renovar los tokens                                         |
| `POST`   | `/auth/logout`                                     | Cerrar sesión                                              |
| `POST`   | `/auth/reautenticar`                               | Verificar nuevamente la contraseña del usuario autenticado |
| `GET`    | `/proyectos`                                       | Obtener los proyectos del usuario                          |
| `POST`   | `/proyectos`                                       | Crear un proyecto                                          |
| `PUT`    | `/proyectos/{proyectoId}`                          | Modificar la información de un proyecto                    |
| `DELETE` | `/proyectos/{proyectoId}`                          | Archivar un proyecto                                       |
| `POST`   | `/proyectos/{proyectoId}/miembros/{usuarioId}`     | Agregar un miembro al proyecto                             |
| `PUT`    | `/proyectos/{proyectoId}/miembros/{usuarioId}/rol` | Modificar el rol de un miembro                             |
| `GET`    | `/proyectos/{proyectoId}/auditoria`                | Consultar la auditoría del proyecto                        |

Los endpoints que requieren autenticación utilizan un JWT mediante el siguiente header:

```http
Authorization: Bearer <token>
```

Las operaciones de modificación, archivo y gestión de miembros requieren la confirmación de contraseña correspondiente, además de la autorización asociada al rol del usuario.

---

## Auditoría

El sistema contempla el registro de eventos asociados a operaciones de gestión de proyectos, con el objetivo de mantener la trazabilidad de los cambios.

Entre las operaciones que registran eventos se encuentran:

* Modificación del nombre del proyecto.
* Modificación de la descripción del proyecto.
* Archivo de proyectos.
* Incorporación de miembros.
* Modificación de roles.

La consulta de los registros de auditoría está protegida mediante el permiso `VER_AUDITORIA`.

---

## Base de datos

El proyecto utiliza **PostgreSQL**.

Entre las principales tablas se encuentran:

### `usuarios`

Contiene la información de los usuarios registrados en el sistema, así como los datos y preferencias asociados a su perfil.

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

### `intentos_acceso_protegido`

Registra intentos de acceso a recursos protegidos, de acuerdo con la implementación de RS8.

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

### Reautenticación

```powershell
$body = @{
    password = "password123"
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:8080/auth/reautenticar" `
    -Method Post `
    -Headers @{ Authorization = "Bearer $token" } `
    -ContentType "application/json" `
    -Body $body
```

Una respuesta exitosa indica que la contraseña fue verificada para el usuario autenticado.

### Pruebas de autorización

Las pruebas de integración contemplan escenarios de control de acceso a proyectos y operaciones de modificación.

Se deben considerar, entre otros, los siguientes casos:

* Un usuario sin autenticación no debe ejecutar operaciones protegidas.
* Un usuario autenticado sin los permisos requeridos no debe poder realizar una modificación.
* Un colaborador no debe poder modificar el nombre del proyecto si ese permiso está reservado al propietario.
* Las operaciones sensibles deben requerir confirmación de contraseña.
* La consulta de auditoría debe estar restringida a los usuarios autorizados.

La ejecución de las pruebas permite comprobar el comportamiento de la implementación y detectar posibles errores de integración.

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

El desarrollo se organiza mediante ramas e issues de trabajo, utilizando Git para el control de versiones y YouTrack para la gestión de tareas.

---

## Contexto académico

Proyecto desarrollado para la materia **Desarrollo Seguro** de la **Facultad de Ingeniería de la Universidad de Montevideo**.

El objetivo del proyecto es aplicar conceptos de desarrollo web y buenas prácticas de seguridad, trabajando con autenticación, autorización, roles, control de acceso, confirmación de identidad para operaciones sensibles y auditoría dentro de una aplicación web.

El proyecto continúa en desarrollo y sus funcionalidades y mecanismos de seguridad se incorporan progresivamente durante los sprints de trabajo.
