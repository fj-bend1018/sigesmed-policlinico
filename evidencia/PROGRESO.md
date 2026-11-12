# Registro de Progreso del Proyecto SIGESMED (Fase 1.6, Fase 2, Fase 3)

Fecha de actualización: 2026-11-12

---

## 1. Fase 1.6 — Cierre de Huecos (COMPLETADA)

### 1.1. Estado de los Puntos de la Fase 1.6

1. **Reversión de modificaciones manuales en base de datos (`sigesmed_test`, puerto 5433):**
   - Se revirtió el ajuste manual sobre la cita 3 ejecutando `UPDATE cita SET paciente_id = 3 WHERE id = 3;`.
   - Se estableció la regla de cero modificaciones directas por SQL para pasar pruebas. Todas las citas requeridas en los recorridos e2e se generan exclusivamente desde los formularios de la interfaz Angular.

2. **Recorrido completo en navegador y capturas reales (`evidencia/recorrido_fase1_6_completo.js`):**
   - **Paciente:**
     - Reserva de cita médica en Pediatría (Dra. Ana Demo) en estado `RESERVADA` (`02_paciente_reserva_cita.png`).
     - Reprogramación de la cita a nuevo horario disponible (`03_paciente_reprograma_cita.png`).
     - Cancelación de la cita y verificación inmediata en la vista de disponibilidad de que la franja horaria queda libre para nuevas reservas (`04_paciente_cancela_cita_horario_libre.png`).
   - **Recepcionista:**
     - Intento de marcar cita futura como `ATENDIDA` y `NO_ASISTIO` (`05_recepcionista_marcar_atendida.png`, `06_recepcionista_marcar_no_asistio.png`).
     - Verificación del historial de atenciones médicas (`07_recepcionista_historial_verificado.png`).
   - **Administrador:**
     - Creación de 5 horarios futuros en la agenda (`01_admin_crea_horarios.png`).
     - Intento de eliminar un horario con cita activa asociada: visualización inmediata del mensaje HTTP 409 en español *"No se puede eliminar el horario porque tiene una cita reservada o confirmada"* (`08_admin_eliminar_horario_409.png`).
   - **Métricas:** 0 errores en la consola del navegador (`consoleErrors: 0`).
   - **Sección de Pasos No Cubiertos Directamente (Honestidad Técnica):**
     - *Paso:* Marcar efectivamente una cita futura como `ATENDIDA` o `NO_ASISTIO` con estado 200 en tiempo de ejecución.
     - *Justificación técnica:* La regla de dominio en `CitaManagementService.java` (línea 127) establece:
       ```java
       if (instante(cita.getHorario()).isAfter(clock.instant())) {
           throw new ApiException(HttpStatus.CONFLICT, "No se puede cerrar una cita antes de su horario");
       }
       ```
       Dado que el sistema prohíbe crear citas o franjas en el pasado desde la UI, y la regla 1 prohíbe manipular la BD manualmente, una cita futura no puede cerrarse anticipadamente. La prueba demostró que el sistema rechaza correctamente el cierre prematuro con HTTP 409 y alerta en español.

3. **Desacoplamiento de `mvn test` respecto a PostgreSQL:**
   - La suite por defecto `.\mvnw.cmd test -f backend/pom.xml` ejecuta **36 pruebas unitarias** aisladas con Spring Mock / H2 / mocks en memoria, tardando ~18 segundos sin requerir PostgreSQL ni conexiones externas.
   - Las pruebas de integración que requieren la base real `sigesmed_test` (puerto 5433) fueron anotadas con `@Tag("integration")` (`CitaConcurrenciaIntegrationTest.java`).
   - Se configuró el perfil Maven `-Pintegration` para ejecutar las 2 pruebas concurrentes contra PostgreSQL cuando se requiera validación profunda.
   - Total de pruebas automáticas: **38 pruebas** (36 unitarias + 2 de integración).
   - Se agregó limpieza completa de datos de prueba (`usuario`, `paciente`, `cita`, `horario`) en el método `@AfterEach tearDown()`.

4. **Seguridad y Enmascaramiento de Contraseñas:**
   - Todos los scripts de evidencia (`recorrido_e2e_tres_roles.js`, `recorrido_fase1_6_completo.js`) leen las credenciales mediante variables de entorno:
     - `process.env.SIGESMED_DEV_ADMIN_PASSWORD`
     - `process.env.SIGESMED_DEV_RECEPCIONISTA_PASSWORD`
     - `process.env.SIGESMED_DEV_PACIENTE_PASSWORD`
   - Todas las contraseñas en archivos Markdown (`evidencia/fase1_pruebas_backend.md`, `fase1.5_estabilizacion.md`, `PROGRESO.md`) se encuentran estrictamente enmascaradas como `********`.

5. **Comparativa visual (360px y 1440px) y corrección de roturas CSS:**
   - Se generaron capturas en resolución móvil (360px) y de escritorio (1440px) para Landing, Login, Registro, Disponibilidad, Portal Paciente, Portal Recepcionista y Portal Administrador en `evidencia/capturas_fase1.6/`.
   - Se identificaron clases utilitarias ausentes tras la poda de Bootstrap (`.align-items-center`, `.gap-2`, `.ms-1`, `.text-bg-warning`), las cuales fueron restauradas e integradas en `frontend/src/styles.scss`.
   - El bundle inicial de producción se mantiene en **482.36 kB**, respetando holgadamente el límite presupuestario (< 520 kB).

6. **Correcciones en `evidencia/fase1.5_estabilizacion.md`:**
   - Recuento total actualizado a 38 pruebas reales.
   - Corrección de la ruta del endpoint administrativo de recepcionistas a `/api/v1/administrador/recepcionistas`.
   - Inclusión de la sección detallada de "Limitaciones".

---

## 2. Fase 2 — Rediseño Visual del Frontend (EN PROGRESO)

- **Objetivo:** Capa visual solamente, evolucionando los componentes existentes sin romper bindings, formularios ni servicios Angular.
- **Sistema de Diseño y Tokens:** Centralización de variables en `frontend/src/styles.scss` (paleta médica profesional, tipografía accesible, espaciados, bordes redondeados, sombras suaves, contraste WCAG AA >= 4.5:1).
- **Héroe de Landing Page:** Contraste reforzado sobre imagen de fondo mediante gradiente/overlay oscuro (`rgba(15, 23, 42, 0.75)`) asegurando legibilidad inmediata de los textos.
- **Secuencia de Prioridad:**
  1. Landing Page (Home)
  2. Autenticación (Login / Registro / Recuperar / Restablecer)
  3. Flujo de Reserva y Disponibilidad (Especialidades, Médicos, Horarios)
  4. Confirmación y Pago Demo
  5. Portal de Paciente (Citas, Historial, Canceladas, Perfil, Reprogramación)
  6. Portal de Recepcionista (Agenda diaria, Búsqueda, Cambio de estados, Historial)
  7. Portal de Administrador (Directorio médico, Horarios, Recepcionistas)
- **Criterio de éxito:** Bundle inicial < 520 kB, pruebas `npm test` en verde, contraste AA verificado.

---

## 3. Fase 3 — Verificación Final (PENDIENTE)

- Ejecución completa de tests:
  - `.\mvnw.cmd test -f backend/pom.xml` (36 pruebas unitarias)
  - `.\mvnw.cmd test -f backend/pom.xml -Pintegration` (2 pruebas de concurrencia e integración)
  - `npm --prefix frontend test -- --watch=false`
  - `npm --prefix frontend run build` (auditoría de bundle size)
- Recorrido completo con capturas finales en 360px y 1440px en `evidencia/capturas_finales/`.
- Generación del informe consolidado `evidencia/RESUMEN_FINAL.md`.
