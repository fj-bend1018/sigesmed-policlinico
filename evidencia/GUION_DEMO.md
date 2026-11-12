# Guion de Demostración del Sistema SIGESMED (8 Minutos)

**Sistema Integral de Gestión de Citas Médicas**  
**Policlínico Infantil Nuestra Señora del Sagrado Corazón**  
**Hito:** Cierre y Demostración (`avance3-ok`)

---

## 1. Instrucciones y Comandos de Arranque desde Cero

Para iniciar el sistema de manera íntegra y reproducible desde un entorno limpio:

### 1.1. Prerrequisitos de Persistencia
El motor PostgreSQL 17 debe estar en ejecución en el puerto `5433` (instancia de prueba aislada) con la base de datos `sigesmed_test`.

### 1.2. Variables de Entorno Requeridas
Configure en su terminal las siguientes variables (manteniendo seguras las credenciales):

```powershell
$env:SIGESMED_DB_URL = "jdbc:postgresql://localhost:5433/sigesmed_test"
$env:SIGESMED_DB_USERNAME = "postgres"
$env:SIGESMED_DB_PASSWORD = "<contraseña_de_postgresql>"

# Cuentas de desarrollo creadas automáticamente por DevDataInitializer:
$env:SIGESMED_DEV_ADMIN_PASSWORD = "<clave_admin>"
$env:SIGESMED_DEV_RECEPCIONISTA_PASSWORD = "<clave_recepcionista>"
$env:SIGESMED_DEV_PACIENTE_PASSWORD = "<clave_paciente>"
```

### 1.3. Arranque del Backend (Puerto 8081)
Ejecute desde la raíz del repositorio (`sistema_migracion/`):

```powershell
.\mvnw.cmd spring-boot:run -f backend/pom.xml "-Dspring-boot.run.profiles=dev"
```

> **Verificación automática:** Flyway ejecuta y valida las migraciones de base de datos **V1 a V4** (`V1__schema.sql`, `V2__sprint2.sql`, `V3__cita_especialidad.sql`, `V4__servicios_complementarios_reservables.sql`). Tomcat inicializa en el puerto `8081`.

### 1.4. Arranque del Frontend (Puerto 4200)
En una segunda terminal, ejecute:

```powershell
npm start --prefix frontend
```

> **Acceso:** Navegue a `http://localhost:4200/` una vez compilado el bundle.

---

## 2. Guion de Demostración Cronometrado (8 Minutos)

### Minuto 0:00 – 1:00 | Paso 1: Landing Page y Especialidades Médicas
- **URL:** `http://localhost:4200/`
- **Cuenta:** Acceso público anónimo (sin autenticación).
- **Acción:**
  1. Mostrar la barra de navegación superior con identidad clínica y accesos directos.
  2. Explorar el hero principal con el lema institucional y el botón "Reservar cita".
  3. Desplazarse a la sección de especialidades: destacar Pediatría general, Medicina familiar, Nutrición, Psicología infantil, Odontología pediátrica y Otorrinolaringología.
- **Qué debe verse:** Diseño responsive limpio con tokens institucionales verde botella (`--clinic-brand`), tarjetas accesibles sin pantalla de carga residual y sección de la Diócesis de Chosica en el footer.

---

### Minuto 1:00 – 2:00 | Paso 2: Registro de Nuevo Paciente
- **URL:** `http://localhost:4200/registro`
- **Cuenta:** Registro de usuario nuevo.
- **Acción:**
  1. Ingresar nombres, apellidos (paterno y materno opcional), DNI válido de 8 dígitos, fecha de nacimiento, correo electrónico institucional/personal y contraseña segura (mínimo 8 caracteres con números y símbolos).
  2. Demostrar la validación visual en tiempo real de formato de DNI y coincidencia de clave.
  3. Presionar "Crear cuenta".
- **Qué debe verse:** Notificación de cuenta creada satisfactoriamente y redirección fluida a la pantalla de inicio de sesión `/login`.

---

### Minuto 2:00 – 2:45 | Paso 3: Inicio de Sesión
- **URL:** `http://localhost:4200/login`
- **Cuenta a usar:**
  - **Identificador / DNI:** `90000003` (o `paciente@example.test`)
  - **Contraseña:** Valor configurado en la variable de entorno `SIGESMED_DEV_PACIENTE_PASSWORD`.
- **Acción:**
  1. Ingresar las credenciales del paciente demo.
  2. Presionar "Iniciar sesión".
- **Qué debe verse:** Validación de credenciales con hash BCrypt, establecimiento de sesión HTTP segura con token CSRF y redirección inmediata al área del paciente (`/paciente/citas`), mostrando el nombre "Paciente Prueba".

---

### Minuto 2:45 – 4:00 | Paso 4: Reserva de Cita Médica y Pago Demo
- **URL:** `http://localhost:4200/reservar`
- **Cuenta:** Paciente autenticado (`ROLE_PACIENTE`).
- **Acción:**
  1. **Paso 1 (Especialidad):** Seleccionar "Pediatría general".
  2. **Paso 2 (Médico):** Seleccionar "Dra. Ana Demo" (CMP DEV-CMP-0001).
  3. **Paso 3 (Fecha):** Elegir una fecha futura dentro de los próximos 7 días.
  4. **Paso 4 (Horario):** Seleccionar una franja disponible (ej. 10:00 AM).
  5. **Confirmación y Pago:** Pasar a la pantalla de demostración de pago y confirmar la transacción simulada.
- **Qué debe verse:** Resumen interactivo lateral actualizándose paso a paso; confirmación exitosa con código de cita generado y estado `RESERVADA` / `CONFIRMADA`.

---

### Minuto 4:00 – 4:45 | Paso 5: Reprogramación de Cita
- **URL:** `http://localhost:4200/paciente/citas`
- **Cuenta:** Paciente autenticado (`ROLE_PACIENTE`).
- **Acción:**
  1. Localizar la cita recién generada en la lista "Atenciones programadas".
  2. Hacer clic en el botón "Reprogramar".
  3. Seleccionar una nueva fecha y franja horaria futura para la misma especialidad.
  4. Confirmar el cambio.
- **Qué debe verse:** El sistema valida la regla de negocio de al menos 2 horas de anticipación, reasigna el nuevo horario liberando el anterior de forma atómica y actualiza la tarjeta de la cita con la nueva fecha y hora.

---

### Minuto 4:45 – 5:30 | Paso 6: Cancelación de Cita y Liberación de Franja
- **URL:** `http://localhost:4200/paciente/citas` $\rightarrow$ `http://localhost:4200/reservar`
- **Cuenta:** Paciente autenticado (`ROLE_PACIENTE`).
- **Acción:**
  1. En la lista de citas activas, pulsar "Cancelar".
  2. Indicar el motivo de cancelación (ej. "Imprevisto familiar").
  3. Abrir en otra pestaña la pantalla de reserva `/reservar` para el mismo médico y fecha.
- **Qué debe verse:** La cita se traslada a la pestaña "Canceladas" con estado `CANCELADA`. En la pantalla de reserva, la franja horaria cancelada vuelve a mostrarse disponible de inmediato para otros pacientes.

---

### Minuto 5:30 – 6:45 | Paso 7: Portal de Recepción (Agenda y Búsqueda en Vivo)
- **URL:** `http://localhost:4200/login` $\rightarrow$ `http://localhost:4200/recepcionista/citas`
- **Cuenta a usar:**
  - **Identificador / DNI:** `90000002` (o `recepcion@example.test`)
  - **Contraseña:** Valor configurado en la variable de entorno `SIGESMED_DEV_RECEPCIONISTA_PASSWORD`.
- **Acción:**
  1. Acceder al espacio de trabajo de recepción.
  2. Filtrar la agenda por fecha y por estado (`RESERVADA`, `ATENDIDA`, `NO_ASISTIO`).
  3. Usar la barra de búsqueda en tiempo real escribiendo el DNI `90000003` o el apellido `Prueba`.
  4. Demostrar las acciones directas de ventanilla: marcar cita como `ATENDIDA` o reprogramar en mostrador.
- **Qué debe verse:** Tabla de datos con actualización reactiva, filtros instantáneos y control completo de estados de atención clínica.

---

### Minuto 6:45 – 8:00 | Paso 8: Portal de Administración (Gestión y Rechazo 409)
- **URL:** `http://localhost:4200/login` $\rightarrow$ `http://localhost:4200/administrador/doctores` y `/administrador/horarios`
- **Cuenta a usar:**
  - **Identificador / DNI:** `90000001` (o `admin@example.test`)
  - **Contraseña:** Valor configurado en la variable de entorno `SIGESMED_DEV_ADMIN_PASSWORD`.
- **Acción:**
  1. Ingresar al panel del Administrador y revisar el Directorio de Médicos (CMP, especialidades, estado ACTIVO/INACTIVO).
  2. Ir a la gestión de horarios y dar de alta un nuevo horario futuro para un médico disponible.
  3. **Demostración de Integridad Referencial (Rechazo 409):** Intentar eliminar un horario que tiene una cita activa vinculada.
- **Qué debe verse:** El sistema bloquea la eliminación y responde con un código de error controlado **HTTP 409 Conflict** mostrando el mensaje: *"No se puede eliminar el horario porque tiene una cita reservada o confirmada"*, garantizando la integridad de la base de datos sin excepciones no controladas.
