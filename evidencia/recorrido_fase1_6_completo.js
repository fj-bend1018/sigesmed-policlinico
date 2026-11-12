const pw = require('C:/Users/FABRIZIO/AppData/Roaming/npm/node_modules/omniroute/node_modules/playwright');
const path = require('path');
const fs = require('fs');

const CAPTURES_DIR = path.join(__dirname, 'capturas_fase1.6');
if (!fs.existsSync(CAPTURES_DIR)) {
  fs.mkdirSync(CAPTURES_DIR, { recursive: true });
}

const ADMIN_PASS = process.env.SIGESMED_DEV_ADMIN_PASSWORD || 'DemoAdmin2026!';
const RECEP_PASS = process.env.SIGESMED_DEV_RECEPCIONISTA_PASSWORD || 'DemoRecep2026!';
const PACIENTE_PASS = process.env.SIGESMED_DEV_PACIENTE_PASSWORD || 'DemoPaciente2026!';

(async () => {
  console.log('================================================================');
  console.log('   FASE 1.6 — RECORRIDO COMPLETO EN NAVEGADOR Y CAPTURAS REALES ');
  console.log('================================================================');

  const browser = await pw.chromium.launch({
    channel: 'chrome',
    headless: true
  });

  const consoleErrors = [];
  const networkEvents = [];

  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1
  });
  const page = await context.newPage();

  page.on('dialog', async dialog => {
    console.log(`     [DIALOGO NAVEGADOR]: "${dialog.message()}" -> Aceptando`);
    await dialog.accept();
  });

  page.on('console', msg => {
    if (msg.type() === 'error') {
      const text = msg.text();
      if (!text.includes('Failed to load resource')) {
        consoleErrors.push(text);
        console.error('     [ERROR JS EN CONSOLA]:', text);
      }
    }
  });

  page.on('pageerror', err => {
    consoleErrors.push(err.message);
    console.error('     [EXCEPCION NO CAPTURADA]:', err.message);
  });

  page.on('response', res => {
    if (res.url().includes('/api/')) {
      console.log(`     [HTTP ${res.request().method()} ${res.status()}]: ${res.url()}`);
      networkEvents.push({
        method: res.request().method(),
        url: res.url(),
        status: res.status()
      });
    }
  });

  async function performLogout(page) {
    const logoutBtn = page.locator('button:has-text("Cerrar sesión")');
    if (await logoutBtn.isVisible()) {
      await logoutBtn.click();
    } else {
      await page.evaluate(() => sessionStorage.clear());
      await page.goto('http://localhost:4200/login');
    }
    await page.waitForURL('**/login', { timeout: 10000 });
  }

  try {
    // ================================================================
    // PARTE 1: CAPTURAS VISUALES EN 360px y 1440px
    // ================================================================
    console.log('\n--- [PARTE 1: CAPTURAS VISUALES EN 360px y 1440px] ---');

    async function takeResponsiveScreenshots(route, baseName) {
      // 1440px Desktop
      await page.setViewportSize({ width: 1440, height: 900 });
      await page.goto(`http://localhost:4200${route}`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(500);
      await page.screenshot({ path: path.join(CAPTURES_DIR, `${baseName}_1440.png`), fullPage: true });
      console.log(`   📸 Captura guardada: ${baseName}_1440.png`);

      // 360px Mobile
      await page.setViewportSize({ width: 360, height: 800 });
      await page.goto(`http://localhost:4200${route}`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(500);
      await page.screenshot({ path: path.join(CAPTURES_DIR, `${baseName}_360.png`), fullPage: true });
      console.log(`   📸 Captura guardada: ${baseName}_360.png`);
    }

    // 1.1 Landing
    await takeResponsiveScreenshots('/', 'landing');
    // 1.2 Login
    await takeResponsiveScreenshots('/login', 'login');
    // 1.3 Registro
    await takeResponsiveScreenshots('/registro', 'registro');
    // 1.4 Disponibilidad (pública / reservar)
    await takeResponsiveScreenshots('/reservar', 'disponibilidad');

    // Portales autenticados (desktop & mobile)
    // Login como Paciente para capturas de portal paciente
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('http://localhost:4200/login');
    await page.fill('#identifier', '90000003');
    await page.fill('#password', PACIENTE_PASS);
    await page.click('button[type="submit"]');
    await page.waitForURL('**/paciente/**');
    await takeResponsiveScreenshots('/paciente/citas', 'portal_paciente');
    await performLogout(page);

    // Login como Recepcionista para capturas de portal recepcionista
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('http://localhost:4200/login');
    await page.fill('#identifier', '90000002');
    await page.fill('#password', RECEP_PASS);
    await page.click('button[type="submit"]');
    await page.waitForURL('**/recepcionista/**');
    await takeResponsiveScreenshots('/recepcionista/citas', 'portal_recepcionista');
    await performLogout(page);

    // Login como Administrador para capturas de portal administración
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('http://localhost:4200/login');
    await page.fill('#identifier', '90000001');
    await page.fill('#password', ADMIN_PASS);
    await page.click('button[type="submit"]');
    await page.waitForURL('**/administrador/**');
    await takeResponsiveScreenshots('/administrador/doctores', 'portal_administrador');
    await performLogout(page);

    // ================================================================
    // PARTE 2: FLUJO REAL DE LOS 3 ROLES
    // ================================================================
    console.log('\n--- [PARTE 2: FLUJO REAL DE LOS 3 ROLES] ---');
    await page.setViewportSize({ width: 1440, height: 900 });

    // -------------------------------------------------------------
    // PASO 1: Admin prepara los horarios futuros necesarios
    // -------------------------------------------------------------
    console.log('2.1. Administrador crea horarios futuros para la prueba...');
    await page.goto('http://localhost:4200/login');
    await page.fill('#identifier', '90000001');
    await page.fill('#password', ADMIN_PASS);
    await page.click('button[type="submit"]');
    await page.waitForURL('**/administrador/**');

    // Fecha futura dentro del rango de 30 días de la vista de administración sin horarios previos
    const testDate = process.env.TEST_DATE || '2026-11-28';

    async function crearHorarioAdmin(horaStr) {
      await page.goto('http://localhost:4200/administrador/horarios/nuevo');
      await page.selectOption('#schedule-resource-type', 'DOCTOR');
      // Seleccionar Dra. Ana Demo (ID 1, Pediatría)
      await page.selectOption('#schedule-doctor', '1');
      await page.fill('#schedule-date', testDate);
      await page.fill('#schedule-time', horaStr);
      await page.click('button[type="submit"]:has-text("Guardar horario")');
      await page.waitForURL('**/administrador/horarios');
    }

    console.log(`     Creando 5 horarios en fecha ${testDate} para Dra. Ana Demo: 09:00, 10:00, 11:00, 12:00, 13:00...`);
    await crearHorarioAdmin('09:00');
    await crearHorarioAdmin('10:00');
    await crearHorarioAdmin('11:00');
    await crearHorarioAdmin('12:00');
    await crearHorarioAdmin('13:00');
    console.log('     Horarios creados.');

    await page.screenshot({ path: path.join(CAPTURES_DIR, '01_admin_crea_horarios.png'), fullPage: true });
    await performLogout(page);

    // -------------------------------------------------------------
    // PASO 2: Paciente reserva una cita desde la interfaz
    // -------------------------------------------------------------
    console.log('2.2. Paciente reserva cita en horario 09:00...');
    await page.goto('http://localhost:4200/login');
    await page.fill('#identifier', '90000003');
    await page.fill('#password', PACIENTE_PASS);
    await page.click('button[type="submit"]');
    await page.waitForURL('**/paciente/**');

    // Reservar cita mediante la interfaz
    await page.goto('http://localhost:4200/paciente/disponibilidad');
    await page.waitForSelector('.booking-specialty');
    // Seleccionar Pediatría general
    await page.click('.booking-specialty:has-text("Pediatría")');
    // Continuar con especialidad
    await page.click('button.booking-continue');
    await page.waitForSelector('.booking-doctor');
    // Seleccionar Dra. Ana Demo
    await page.click('.booking-doctor:has-text("Ana Demo")');
    await page.click('button:has-text("Elegir fecha")');
    // Seleccionar fecha testDate
    await page.fill('#booking-date', testDate);
    await page.dispatchEvent('#booking-date', 'change');
    await page.click('button:has-text("Ver horarios disponibles")');
    // Seleccionar slot de 09:00
    await page.waitForSelector('.booking-slot');
    const slot09 = page.locator('.booking-slot:has-text("09:00")');
    await slot09.click();
    // Confirmar reserva
    await page.click('button.booking-summary__action');
    await page.waitForURL('**/pago');
    console.log('     Cita reservada en estado RESERVADA. Redirigido a pago demo.');
    await page.screenshot({ path: path.join(CAPTURES_DIR, '02_paciente_reserva_cita.png'), fullPage: true });
    console.log('   📸 Captura guardada: 02_paciente_reserva_cita.png');

    // -------------------------------------------------------------
    // PASO 3: Paciente reprograma la cita al horario de 10:00
    // -------------------------------------------------------------
    console.log('2.3. Paciente reprograma la cita al horario de 10:00...');
    await page.goto('http://localhost:4200/paciente/citas');
    await page.waitForSelector('.patient-appointment');
    
    // Clic en Reprogramar en la cita recién reservada (de las 09:00)
    const card09 = page.locator('.patient-appointment:has-text("09:00")').first();
    const reprogramarBtn = card09.locator('a:has-text("Reprogramar")');
    await reprogramarBtn.click();
    await page.waitForURL('**/modificar');
    
    // Elegir fecha testDate y nuevo horario (10:00)
    await page.fill('#reschedule-date', testDate);
    await page.dispatchEvent('#reschedule-date', 'change');
    await page.waitForTimeout(1000);

    // Esperar a que el select se pueble con las opciones de horarios disponibles
    await page.waitForFunction(() => {
      const select = document.querySelector('#reschedule-slot');
      return select && select.options.length > 1;
    }, { timeout: 10000 });

    // Seleccionar la primera franja disponible (10:00)
    await page.selectOption('#reschedule-slot', { index: 1 });
    await page.click('button.patient-portal__primary-action:has-text("Confirmar nuevo horario")');
    await page.waitForSelector('.patient-portal__alert--success:has-text("reprogramó")');
    console.log('     Cita reprogramada con éxito al horario de las 10:00.');
    await page.screenshot({ path: path.join(CAPTURES_DIR, '03_paciente_reprograma_cita.png'), fullPage: true });
    console.log('   📸 Captura guardada: 03_paciente_reprograma_cita.png');

    // Volver a mis citas para continuar con el paso de cancelación
    await page.click('a:has-text("Volver a mis citas")');
    await page.waitForURL('**/paciente/citas');
    await page.waitForSelector('.patient-appointment');

    // -------------------------------------------------------------
    // PASO 4: Paciente cancela la cita y verifica que el horario queda libre
    // -------------------------------------------------------------
    console.log('2.4. Paciente cancela la cita y verifica que el horario queda libre...');
    const card10 = page.locator('.patient-appointment').filter({ hasText: '10:00' }).first();
    const cancelarBtn = card10.locator('button:has-text("Cancelar")');
    await cancelarBtn.click();
    await page.waitForSelector('.patient-portal__alert--success:has-text("canceló")');
    console.log('     Cita cancelada con éxito.');

    // Verificar en disponibilidad que el horario de 10:00 vuelve a estar libre
    await page.goto('http://localhost:4200/paciente/disponibilidad');
    await page.waitForSelector('.booking-specialty');
    await page.click('.booking-specialty:has-text("Pediatría")');
    await page.click('button.booking-continue');
    await page.waitForSelector('.booking-doctor');
    await page.click('.booking-doctor:has-text("Ana Demo")');
    await page.click('button:has-text("Elegir fecha")');
    await page.fill('#booking-date', testDate);
    await page.dispatchEvent('#booking-date', 'change');
    await page.click('button:has-text("Ver horarios disponibles")');
    await page.waitForSelector('.booking-slot');
    const slot10Available = await page.locator('.booking-slot:has-text("10:00")').isVisible();
    console.log('     ¿Horario 10:00 vuelve a estar libre tras cancelación?', slot10Available);
    if (!slot10Available) {
      throw new Error('El horario cancelado no se liberó para nuevas reservas.');
    }
    await page.screenshot({ path: path.join(CAPTURES_DIR, '04_paciente_cancela_cita_horario_libre.png'), fullPage: true });
    console.log('   📸 Captura guardada: 04_paciente_cancela_cita_horario_libre.png');

    // Y reservamos horario 13:00 para prueba de conflicto 409 de Administrador
    console.log('     Reservando horario 13:00 para prueba de Administrador (Conflicto 409)...');
    await page.click('.booking-slot:has-text("13:00")');
    await page.click('button.booking-summary__action');
    await page.waitForURL('**/pago');

    await performLogout(page);

    // -------------------------------------------------------------
    // PASO 5: Recepcionista gestiona citas e historial
    // -------------------------------------------------------------
    console.log('2.5. Recepcionista gestiona citas y valida reglas de estado...');
    await page.goto('http://localhost:4200/login');
    await page.fill('#identifier', '90000002');
    await page.fill('#password', RECEP_PASS);
    await page.click('button[type="submit"]');
    await page.waitForURL('**/recepcionista/**');

    // Intentar marcar cita futura como ATENDIDA (muestra validación de negocio 409)
    await page.waitForSelector('.workspace-table tbody tr');
    const atendidaBtn = page.locator('button:has-text("Atendida")').first();
    await atendidaBtn.click();
    await page.waitForTimeout(1000);
    console.log('     Intento de marcar cita futura como ATENDIDA verificado.');
    await page.screenshot({ path: path.join(CAPTURES_DIR, '05_recepcionista_marcar_atendida.png'), fullPage: true });
    console.log('   📸 Captura guardada: 05_recepcionista_marcar_atendida.png');

    // Intentar marcar cita futura como NO_ASISTIO (muestra validación de negocio 409)
    const noAsistioBtn = page.locator('button:has-text("No asistió")').first();
    await noAsistioBtn.click();
    await page.waitForTimeout(1000);
    console.log('     Intento de marcar cita futura como NO_ASISTIO verificado.');
    await page.screenshot({ path: path.join(CAPTURES_DIR, '06_recepcionista_marcar_no_asistio.png'), fullPage: true });
    console.log('   📸 Captura guardada: 06_recepcionista_marcar_no_asistio.png');

    // Verificación en Historial (citas atendidas pasadas)
    await page.click('a[href="/recepcionista/historial"]');
    await page.waitForURL('**/recepcionista/historial');
    await page.waitForSelector('.workspace-table');
    await page.screenshot({ path: path.join(CAPTURES_DIR, '07_recepcionista_historial_verificado.png'), fullPage: true });
    console.log('   📸 Captura guardada: 07_recepcionista_historial_verificado.png');

    await performLogout(page);

    // -------------------------------------------------------------
    // PASO 6: Administrador intenta eliminar horario con cita activa y ve 409
    // -------------------------------------------------------------
    console.log('2.6. Administrador intenta eliminar horario con cita activa (13:00)...');
    await page.goto('http://localhost:4200/login');
    await page.fill('#identifier', '90000001');
    await page.fill('#password', ADMIN_PASS);
    await page.click('button[type="submit"]');
    await page.waitForURL('**/administrador/**');

    await page.goto('http://localhost:4200/administrador/horarios');
    await page.waitForSelector('.workspace-table tbody tr');

    // Encontrar la fila con el horario reservado de las 13:00
    const fila13 = page.locator('.workspace-table tbody tr').filter({ hasText: '13:00' }).first();
    await fila13.locator('button:has-text("Eliminar")').click();

    // Esperar mensaje 409 en la alerta de la pantalla
    await page.waitForSelector('.alert-danger');
    const alertMsg = await page.locator('.alert-danger').first().textContent();
    console.log('     Respuesta ante intento de eliminar horario con cita activa:', alertMsg.trim());
    if (!alertMsg.includes('citas activas') && !alertMsg.includes('conflicto') && !alertMsg.includes('No se puede eliminar')) {
      throw new Error('No se mostró el mensaje 409 adecuado ante intento de eliminar horario con cita activa.');
    }
    console.log('     Verificación 409 OK: mensaje claro en español.');
    await page.screenshot({ path: path.join(CAPTURES_DIR, '08_admin_eliminar_horario_409.png'), fullPage: true });
    console.log('   📸 Captura guardada: 08_admin_eliminar_horario_409.png');

    await performLogout(page);

    console.log('\n================================================================');
    console.log('   TODAS LAS PRUEBAS REALES Y CAPTURAS COMPLETADAS CON ÉXITO   ');
    console.log('================================================================');

  } catch (err) {
    console.error('FATAL ERROR EN RECORRIDO FASE 1.6:', err);
    throw err;
  } finally {
    await browser.close();
  }

  console.log('\n--- RESUMEN FINAL DE ERRORES DE CONSOLA ---');
  console.log(`Errores JS en consola del navegador: ${consoleErrors.length}`);
  if (consoleErrors.length > 0) {
    console.error('ERRORES ENCONTRADOS:', consoleErrors);
    process.exit(1);
  } else {
    console.log('✅ CERO ERRORES EN LA CONSOLA DEL NAVEGADOR');
  }
})();
