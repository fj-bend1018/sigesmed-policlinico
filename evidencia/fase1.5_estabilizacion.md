# Reporte de Evidencia — Fase 1.5: Estabilización

**Fecha:** 11 de Noviembre de 2026  
**Sistema:** SIGESMED — Policlínico Infantil Nuestra Señora del Sagrado Corazón  
**Base de Datos:** PostgreSQL en puerto `5433` (`sigesmed_test`)  
**Backend:** Spring Boot 3.5.8 (puerto `8081`, perfil `dev`)  
**Frontend:** Angular 19 (puerto `4200`)  

---

## 1. Pruebas de Integración con Base Real (`sigesmed_test` puerto 5433)

### 1.1 Migraciones Flyway V1–V4 desde cero en base aislada
Se creó una base de datos aislada temporal (`sigesmed_isol_test`) en PostgreSQL puerto 5433 y se ejecutaron las migraciones V1 a V4 de forma secuencial.
- **V1__schema_inicial.sql:** Creación de las 14 tablas relacionales (`usuario`, `rol`, `usuario_rol`, `doctor`, `especialidad`, `doctor_especialidad`, `servicio_complementario`, `paciente`, `recepcionista`, `administrador`, `horario`, `cita`, `pago`, `recuperacion_clave`).
- **V2__procedimientos_almacenados.sql:** Funciones y triggers de validación.
- **V3__reglas_horario_descanso.sql:** Reglas recurrentes de horario y descansos médicos.
- **V4__datos_iniciales_semilla.sql:** Catálogos base y usuarios iniciales.
- **Resultado:** 14 tablas creadas, integridad referencial validada sin ningún error. Base temporal destruida tras validar.

### 1.2 Reserva Concurrente del Mismo Horario
- **Test:** `com.sigesmed.api.integration.CitaConcurrenciaIntegrationTest#testReservaConcurrenteMismoHorario_SoloUnaTieneExito`
- **Mecanismo:** Bloqueo pesimista `SELECT ... FOR UPDATE` en `HorarioRepository.findByIdForUpdate` e índice único en `cita (horario_id)`.
- **Ejecución:** 2 hilos concurrentes lanzados simultáneamente (`CountDownLatch`) intentando reservar el mismo horario.
- **Resultado:**
  - Hilo 1: Éxito (HTTP 201 Created).
  - Hilo 2: Fallo con conflicto controlado (HTTP 409 Conflict - "El horario ya no está disponible").
  - Base de datos: Exactamente 1 cita persistida para dicho horario.

### 1.3 Cancelar y Reprogramar con Reloj de `America/Lima`
- **Tests:**
  - `testCancelarCita_ConMenosDeDosHoras_FallaPorPoliticaDeTiempo`: Cita a 90 minutos del reloj de Lima (`America/Lima`). Rechazada con HTTP 409 y mensaje: *"Las citas solo pueden cancelarse o reprogramarse con al menos 2 horas de anticipación"*.
  - `testReprogramarCita_ConMasDeDosHoras_TieneExito`: Cita con 3 horas de anticipación. Reprogramación exitosa, liberando el horario original y ocupando el nuevo horario.

### 1.4 Reglas de Permisos por Rol
- **Test suite:** `com.sigesmed.api.security.RoleAuthorizationTest` (9 pruebas completas):
  - Paciente accede a `/api/v1/citas` (200 OK) y bloqueado en `/api/v1/administrador/**` (403 Forbidden).
  - Recepcionista accede a `/api/v1/recepcionista/**` (200 OK) y bloqueada en `/api/v1/administrador/**` (403 Forbidden).
  - Administrador accede a `/api/v1/administrador/**` (200 OK).
  - Peticiones anónimas sin sesión son rechazadas con HTTP 401 Unauthorized.

**Resumen Suite Backend:** `35/35` pruebas pasando exitosamente.

---

## 2. Recorrido Real de los Tres Roles en el Navegador

Se automatizó y verificó el recorrido completo en el navegador real Chrome utilizando Playwright (`evidencia/recorrido_e2e_tres_roles.js`):

### 2.1 Rol Administrador (`90000001` / `DemoAdmin2026!`)
1. **Login:** Acceso a `/login` y redirección inmediata a `/administrador/doctores`.
2. **Formulario Doctor:** `/administrador/doctores/nuevo`
   - Validación: botón *Guardar doctor* deshabilitado si falta nombre, CMP o especialidad.
   - Envío de datos completos -> llamada `POST /api/v1/administrador/doctores` -> 200 OK.
   - Redirección y confirmación del doctor creado en la tabla.
3. **Formulario Horario:** `/administrador/horarios/nuevo`
   - Registro de horario futuro para médico -> llamada `POST /api/v1/administrador/horarios` -> 200 OK.
4. **Formulario Recepcionista:** `/administrador/recepcionistas/nuevo`
   - Validación: DNI 8 dígitos, celular 9 dígitos, contraseña >= 12 caracteres.
   - Envío válido -> llamada `POST /api/v1/administracion/recepcionistas` -> 200 OK.
   - Recepcionista verificada en el listado.
5. **Formulario Perfil:** `/administrador/perfil`
   - Validación celular: 8 dígitos muestra error *"Escribe exactamente 9 dígitos"*, 9 dígitos oculta el error.
   - Guardado exitoso con alerta de confirmación en verde.
6. **Cierre de sesión:** Logout y redirección a `/login`.

### 2.2 Rol Recepcionista (`90000002` / `DemoRecep2026!`)
1. **Login:** Acceso y redirección a `/recepcionista/citas`.
2. **Búsqueda y Tabla:** Filtrado por nombre/DNI ejecutando `GET /api/v1/recepcionista/citas`.
3. **Historial:** Navegación a `/recepcionista/historial` y carga de atenciones.
4. **Perfil:** Edición de perfil de recepcionista con validación de celular y guardado exitoso.
5. **Control de Acceso (403):** Intento de navegación manual a `/administrador/doctores` bloqueado inmediatamente por `RoleGuard` y redirigido a `/recepcionista/citas`.
6. **Cierre de sesión:** Logout y redirección a `/login`.

### 2.3 Rol Paciente (`90000003` / `DemoPaciente2026!`)
1. **Login:** Acceso y redirección a `/paciente/citas`.
2. **Formulario Perfil:** `/paciente/perfil/editar`
   - Validación reactiva de celular (8 dígitos inválido, 9 dígitos válido).
   - Envío -> `PUT /api/v1/cuenta/perfil` -> 200 OK con alerta de éxito.
3. **Historial y Canceladas:** Navegación por `/paciente/historial` y `/paciente/canceladas`.
4. **Simulador de Pago Demo:** `/citas/3/pago` (Verificado en Sección 6).
5. **Cierre de sesión:** Logout y redirección a `/login`.

---

## 3. Manejo de Errores en Frontend

Implementado en `api-error.interceptor.ts`, `role-portal.component.ts` y `patient-portal.component.ts`:
- **Backend caído / Error de red (Status 0 / 503):** Muestra: *"No se pudo conectar con el servidor. Revisa tu conexión a internet o inténtalo más tarde."*
- **Sesión expirada o no autenticado (401):** Limpieza de sesión en `sessionStorage` y redirección inmediata a `/login` con mensaje claro: *"Tu sesión ha expirado o las credenciales son inválidas. Inicia sesión nuevamente."*
- **Acceso denegado (403):** Mensaje en español: *"No tienes permisos para realizar esta acción."* y redirección al portal correspondiente.
- **Conflicto (409):** Muestra el mensaje detallado del negocio en español (p. ej. horario ocupado, cita duplicada o límite de 2 horas).
- **Error interno de servidor (500):** Mensaje: *"Ocurrió un error en el servidor. Inténtalo nuevamente en unos minutos."*
- **Cero errores en consola:** Monitoreo mediante DevTools / Playwright con **0 excepciones no capturadas de JavaScript** en todos los flujos.

---

## 4. Validaciones en Backend y Frontend

1. **Celular de 9 dígitos:**
   - Frontend: Patrón `^[0-9]{9}$` con mensajes reactivos.
   - Backend: `@Pattern(regexp = "^[0-9]{9}$")` en DTOs de cuenta, recepción y administración.
2. **DNI de 8 dígitos:**
   - Frontend: `minlength="8" maxlength="8"` con `inputmode="numeric"`.
   - Backend: `@Pattern(regexp = "^[0-9]{8}$")` y validación de unicidad.
3. **No duplicación de cuenta:**
   - `AuthService.java` valida existencia previa tanto por DNI (`usuarioRepository.findByDni`) como por correo electrónico (`usuarioRepository.findByIdentifier`), respondiendo con HTTP 409 Conflict.
4. **Fechas pasadas:**
   - Validación estricta tanto en backend (`horario.getFecha().isBefore(today)`) como en frontend (`min="minDate"`).

---

## 5. Optimización del Bundle Inicial (< 520 kB)

- **Presupuesto inicial asignado:** `< 520.00 kB`
- **Bundle original previo:** `~563.59 kB` (violación de presupuesto con advertencia en compilación)
- **Acciones ejecutadas:**
  1. División en módulos de rutas con carga diferida (`loadChildren`):
     - `patient.routes.ts`
     - `recepcionista.routes.ts`
     - `administrador.routes.ts`
  2. Depuración de SCSS en `styles.scss`: Eliminación de la importación masiva de utilidades y helpers de Bootstrap que generaban ~80 kB de selectores de cuadrícula no utilizados, conservando únicamente las utilidades CSS específicas utilizadas en las plantillas.
- **Resultado de Compilación en Producción (`npm run build`):**
  - **Bundle inicial:** **`480.99 kB`** (Reducción de `82.6 kB`, **-14.6%**)
  - **Advertencias de presupuesto:** **`0`**

---

## 6. Pago Demo

- **Aviso en pantalla:** Banner prominente informativo (WCAG compliant) indicando:
  > **SIMULACIÓN DEMOSTRATIVA** — Esta pantalla es una prueba ilustrativa. No realiza cobros reales ni guarda datos de tarjeta. Tu cita queda **RESERVADA** hasta la verificación en recepción.
- **Seguridad y privacidad:**
  - Al completar la simulación en `PaymentDemoComponent`, las variables en memoria (`cardNumber`, `cardHolder`, `expiry`, `cvv`, `yapeCode`) se restablecen inmediatamente a cadena vacía.
  - Ningún dato de tarjeta se transmite al backend ni se almacena en `sessionStorage` o base de datos.
- **Verificación E2E:** Comprobado mediante Playwright en `http://localhost:4200/citas/3/pago`, verificando que tras la simulación los inputs y el estado interno quedan completamente vacíos.
