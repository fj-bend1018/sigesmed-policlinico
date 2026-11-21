# 🏥 SIGESMED — Sistema de Gestión de Citas Médicas

Sistema web para la gestión integral de citas médicas del **Policlínico Infantil Nuestra Señora del Sagrado Corazón** (Diócesis de Chosica · San Gregorio, Ate), migrado a una arquitectura desacoplada con **Angular 17**, **Spring Boot 3.5**, **Flyway** y **PostgreSQL 17 (Supabase)**.

---

## 🎯 Alcance Actual

- **4 portales por rol:**
  - **Paciente:** registro, inicio de sesión, consulta de disponibilidad por especialidad/médico/servicio complementario, reserva, reprogramación, cancelación, historial y perfil.
  - **Doctor:** agenda del día, mis horarios (próximos 30 días), historial de pacientes, marcación de atención (`ATENDIDA` / `NO_ASISTIO`) y perfil médico.
  - **Recepcionista:** búsqueda de citas por paciente/DNI/médico, reprogramación, confirmación de asistencia, cancelación, historial total y perfil.
  - **Administrador:** gestión de médicos y especialidades, programación y protección de horarios (`409` si tienen cita activa), gestión de recepcionistas y perfil.
- **Seguridad:** sesiones HTTP con protección contra fijación de sesión, `BCrypt`, protección CSRF (`XSRF-TOKEN` / `X-XSRF-TOKEN`) y bloqueo automático tras 3 intentos fallidos por 15 minutos.

---

## 💻 Tecnologías Utilizadas

| Tecnología | Rol |
|---|---|
| **Angular 17 (`TypeScript`, `SCSS`, `Bootstrap 5`)** | Frontend SPA responsive, portales por rol y gráficos SVG animados |
| **Java 21 + Spring Boot 3.5.3** | Backend y API REST (`/api/v1`) |
| **Spring Security + Spring Data JPA** | Autenticación, autorización por roles, CSRF y persistencia |
| **PostgreSQL 17 (Supabase)** | Base de datos relacional principal (demo y entrega) |
| **PostgreSQL local (`5433`)** | Base de respaldo y pruebas de integración (`-Pintegration`) |
| **Flyway (`V1` a `V5`)** | Versionamiento automático del esquema en `backend/src/main/resources/db/migration` |
| **JUnit 5 + Mockito + Karma/Jasmine + Playwright** | Pruebas unitarias, de integración (concurrencia) y E2E/caja negra |

---

## 📁 Estructura del Repositorio

```text
sistema_migracion/
├── backend/            # API REST Spring Boot 3.5 + Flyway + pruebas (unitarias e integración)
├── frontend/           # Aplicación Angular 17 (puerto 4200)
├── scripts/            # Scripts auxiliares de pruebas
├── legacy/             # Código monolítico original de referencia (pre-migración)
├── docker-compose.yml  # PostgreSQL 17 local listo para desarrollo
└── .env.example        # Plantilla opcional de variables de entorno (ej. Supabase)
```

---

## 🚀 Requisitos y Ejecución Local

### 1. Requisitos previos
- **Java JDK 21+** (`java -version`)
- **Apache Maven 3.9+** (`mvn -version`)
- **Node.js 20+ y npm** (`node -v`, `npm -v`)
- **Docker** (recomendado para levantar PostgreSQL 17 al instante) o **PostgreSQL 17 local** en `localhost:5432` (base `sigesmed`, usuario/clave `postgres`/`postgres`)

### 2. Base de datos (solo la primera vez, o `.env` opcional)
Si no tienes PostgreSQL local instalado, levanta la base con Docker:

```powershell
docker compose up -d
```

> **Variables de entorno (opcionales):** por defecto el backend usa el perfil `dev` y se conecta a `jdbc:postgresql://localhost:5432/sigesmed` (`postgres` / `postgres`). Si prefieres usar **Supabase** u otra instancia, copia `.env.example` a `.env` en la raíz del proyecto y completa tus credenciales (`.env` está en `.gitignore` y nunca se sube a Git).

### 3. Levantar Backend (puerto `8081`)
```powershell
cd backend
mvn spring-boot:run
```
Flyway ejecutará automáticamente las migraciones (`V1` a `V5`) al iniciar.

### 4. Levantar Frontend (puerto `4200`)
```powershell
cd frontend
npm install
npm start
```
Luego abre `http://localhost:4200` en tu navegador.

---

## 🧪 Pruebas

- **Backend (unitarias y seguridad, sin tocar BD de integración):**
  ```powershell
  mvn test -f backend/pom.xml
  ```
- **Backend (concurrencia e integración contra `SIGESMED_TEST_DB_URL`):**
  ```powershell
  mvn test -Pintegration -f backend/pom.xml
  ```
- **Frontend (compilación y unitarias):**
  ```powershell
  npm --prefix frontend run build
  npm --prefix frontend test -- --watch=false
  ```
