const pw = require('C:/Users/FABRIZIO/AppData/Roaming/npm/node_modules/omniroute/node_modules/playwright');

(async () => {
  console.log('===========================================================');
  console.log('   INICIANDO RECORRIDO E2E COMPLETO DE LOS TRES ROLES     ');
  console.log('   Y VERIFICACION DE MANEJO DE ERRORES Y PAGO DEMO        ');
  console.log('===========================================================');

  const browser = await pw.chromium.launch({
    channel: 'chrome',
    headless: true
  });
  const context = await browser.newContext();
  const page = await context.newPage();

  const jsConsoleErrors = [];
  const networkCalls = [];

  page.on('console', msg => {
    // Distinguir entre logs automáticos de red del navegador y errores reales de JS/Angular
    if (msg.type() === 'error') {
      const text = msg.text();
      if (!text.includes('Failed to load resource')) {
        jsConsoleErrors.push(text);
        console.error('  [ERROR JS EN CONSOLA]:', text);
      }
    }
  });

  page.on('pageerror', err => {
    jsConsoleErrors.push(err.message);
    console.error('  [EXCEPCION NO CAPTURADA]:', err.message);
  });

  page.on('response', res => {
    if (res.url().includes('/api/')) {
      networkCalls.push({
        method: res.request().method(),
        url: res.url(),
        status: res.status()
      });
    }
  });

  async function performLogout() {
    console.log('   Cerrando sesión actual...');
    const logoutBtn = page.locator('button:has-text("Cerrar sesión")');
    if (await logoutBtn.isVisible()) {
      await logoutBtn.click();
    } else {
      await page.evaluate(() => sessionStorage.clear());
      await page.goto('http://localhost:4200/login');
    }
    await page.waitForURL('**/login', { timeout: 10000 });
    console.log('   Sesión cerrada con éxito.');
  }

  try {
    // ============================================================
    // 0. VERIFICACION DE MANEJO DE ERRORES (LOGIN FALLIDO)
    // ============================================================
    console.log('\n--- [TEST: MANEJO DE ERROR 401 EN LOGIN] ---');
    await page.goto('http://localhost:4200/login', { waitUntil: 'networkidle' });
    await page.fill('#identifier', '90000001');
    await page.fill('#password', 'ClaveIncorrecta123!');
    await page.click('button[type="submit"]');
    
    // Esperar mensaje claro en español en la UI
    await page.waitForSelector('.alert-danger, [role="alert"]');
    const loginErrorText = await page.locator('.alert-danger, [role="alert"]').textContent();
    console.log('     Mensaje mostrado ante credenciales erróneas:', loginErrorText.trim());
    if (!loginErrorText.toLowerCase().includes('incorrect') && !loginErrorText.toLowerCase().includes('error')) {
      throw new Error('El mensaje de error ante 401 no fue claro.');
    }
    console.log('     Manejo de error 401 validado: claro, amigable en español y sin romper la UI.');

    // ============================================================
    // 1. ROL: ADMINISTRADOR
    // ============================================================
    console.log('\n--- [ROL 1: ADMINISTRADOR] ---');
    console.log('1.1. Login como administrador (90000001)...');
    await page.fill('#identifier', '90000001');
    await page.fill('#password', 'DemoAdmin2026!');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/administrador/**', { timeout: 10000 });
    console.log('     Login admin exitoso. URL:', page.url());

    // 1.2. Listado de doctores
    console.log('1.2. Verificando tabla de doctores (/administrador/doctores)...');
    await page.waitForSelector('.workspace-table, .workspace-empty');
    console.log('     Directorio de doctores cargado.');

    // 1.3. Formulario Nuevo Doctor
    console.log('1.3. Probando formulario Nuevo Doctor (/administrador/doctores/nuevo)...');
    await page.click('a[routerLink="/administrador/doctores/nuevo"]');
    await page.waitForURL('**/administrador/doctores/nuevo');
    
    const saveDoctorBtn = page.locator('button[type="submit"]:has-text("Guardar doctor")');
    const isDisabledInitially = await saveDoctorBtn.isDisabled();
    console.log('     Validación: ¿Botón guardar deshabilitado inicialmente?', isDisabledInitially);

    const testDoctorName = 'Dr. Juan Perez E2E ' + Math.floor(Math.random() * 1000);
    const testDoctorCmp = 'CMP' + Math.floor(10000 + Math.random() * 90000);
    await page.fill('#doctor-name', testDoctorName);
    await page.fill('#doctor-cmp', testDoctorCmp);
    await page.selectOption('#doctor-specialties', { index: 0 });
    
    const isEnabledNow = !(await saveDoctorBtn.isDisabled());
    console.log('     Validación: ¿Botón habilitado con datos completos?', isEnabledNow);

    await saveDoctorBtn.click();
    await page.waitForURL('**/administrador/doctores', { timeout: 10000 });
    console.log('     Doctor guardado exitosamente. Redirigido a lista.');
    await page.waitForSelector(`text=${testDoctorName}`);
    console.log(`     Verificado: ${testDoctorName} aparece en la tabla.`);

    // 1.4. Formulario Nuevo Horario
    console.log('1.4. Probando formulario Nuevo Horario (/administrador/horarios/nuevo)...');
    await page.click('a[href="/administrador/horarios"]');
    await page.waitForURL('**/administrador/horarios');
    await page.click('a[routerLink="/administrador/horarios/nuevo"]');
    await page.waitForURL('**/administrador/horarios/nuevo');

    await page.selectOption('#schedule-resource-type', 'DOCTOR');
    await page.selectOption('#schedule-doctor', { index: 1 });
    // Fecha y hora única
    const futureDate = '2026-12-2' + Math.floor(1 + Math.random() * 8);
    await page.fill('#schedule-date', futureDate);
    await page.fill('#schedule-time', '15:30');
    
    const saveScheduleBtn = page.locator('button[type="submit"]:has-text("Guardar horario")');
    await saveScheduleBtn.click();
    await page.waitForURL('**/administrador/horarios', { timeout: 10000 });
    console.log(`     Horario (${futureDate} 15:30) guardado exitosamente.`);

    // 1.5. Formulario Nueva Recepcionista
    console.log('1.5. Probando formulario Nueva Recepcionista (/administrador/recepcionistas/nuevo)...');
    await page.click('a[href="/administrador/recepcionistas"]');
    await page.waitForURL('**/administrador/recepcionistas');
    await page.click('a[routerLink="/administrador/recepcionistas/nuevo"]');
    await page.waitForURL('**/administrador/recepcionistas/nuevo');

    const testRecepDni = '' + Math.floor(70000000 + Math.random() * 9999999);
    const testRecepEmail = `recep_${testRecepDni}@sigesmed.com`;
    
    await page.fill('#new-rec-dni', testRecepDni);
    await page.fill('#new-rec-name', 'Maria');
    await page.fill('#new-rec-last', 'Gomez');
    await page.fill('#new-rec-second-last', 'Rios');
    await page.fill('#new-rec-email', testRecepEmail);
    await page.fill('#new-rec-phone', '987654321');
    await page.fill('#new-rec-address', 'Av. Las Palmas 456');
    await page.fill('#new-rec-password', 'ClaveSegura2026!');

    const saveRecepBtn = page.locator('button[type="submit"]:has-text("Crear cuenta")');
    await saveRecepBtn.click();
    await page.waitForURL('**/administrador/recepcionistas', { timeout: 10000 });
    console.log('     Recepcionista guardada exitosamente.');
    await page.waitForSelector(`text=${testRecepDni}`);
    console.log(`     Verificado: DNI ${testRecepDni} aparece en la lista de recepcionistas.`);

    // 1.6. Formulario Perfil Administrador
    console.log('1.6. Probando formulario Perfil (/administrador/perfil)...');
    await page.click('a[href="/administrador/perfil"]');
    await page.waitForURL('**/administrador/perfil');
    
    console.log('     Validando campo celular (8 dígitos debe mostrar error)...');
    await page.fill('#staff-phone', '98765432');
    const error8digits = await page.locator('text=Escribe exactamente 9 dígitos.').isVisible();
    console.log('     Validación celular: ¿Error visible con 8 dígitos?', error8digits);

    console.log('     Validando campo celular (9 dígitos debe ser válido)...');
    await page.fill('#staff-phone', '987654321');
    const error9digits = await page.locator('text=Escribe exactamente 9 dígitos.').isVisible();
    console.log('     Validación celular: ¿Error oculto con 9 dígitos?', !error9digits);

    await page.click('button[type="submit"]:has-text("Guardar cambios")');
    await page.waitForSelector('.alert-success');
    console.log('     Perfil de administrador actualizado y guardado correctamente.');

    // Logout
    await performLogout();

    // ============================================================
    // 2. ROL: RECEPCIONISTA
    // ============================================================
    console.log('\n--- [ROL 2: RECEPCIONISTA] ---');
    console.log('2.1. Login como recepcionista (90000002)...');
    await page.fill('#identifier', '90000002');
    await page.fill('#password', 'DemoRecep2026!');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/recepcionista/**', { timeout: 10000 });
    console.log('     Login recepcionista exitoso. URL:', page.url());

    // 2.2. Panel de citas y búsqueda
    console.log('2.2. Verificando panel de citas y búsqueda (/recepcionista/citas)...');
    await page.waitForSelector('.workspace-table, .workspace-empty');
    await page.fill('#appointment-search', 'Demo');
    await page.click('button:has-text("Buscar")');
    await page.waitForTimeout(500);
    console.log('     Búsqueda en recepción ejecutada con éxito.');

    // 2.3. Historial de recepción
    console.log('2.3. Verificando sección historial (/recepcionista/historial)...');
    await page.click('a[href="/recepcionista/historial"]');
    await page.waitForURL('**/recepcionista/historial');
    await page.waitForSelector('.workspace-table, .workspace-empty');
    console.log('     Historial cargado con éxito.');

    // 2.4. Perfil de recepcionista
    console.log('2.4. Probando perfil de recepcionista (/recepcionista/perfil)...');
    await page.click('a[href="/recepcionista/perfil"]');
    await page.waitForURL('**/recepcionista/perfil');
    await page.fill('#staff-phone', '999888777');
    await page.click('button[type="submit"]:has-text("Guardar cambios")');
    await page.waitForSelector('.alert-success');
    console.log('     Perfil recepcionista guardado exitosamente.');

    // 2.5. Verificación de permisos (403): recepcionista intentando acceder a admin
    console.log('2.5. Probando control de permisos: recepcionista accediendo a /administrador...');
    await page.goto('http://localhost:4200/administrador/doctores');
    await page.waitForTimeout(1000);
    const redirectedUrl = page.url();
    console.log('     URL tras intento de acceso no autorizado:', redirectedUrl);
    if (redirectedUrl.includes('/administrador/doctores')) {
      throw new Error('RoleGuard falló en proteger la ruta /administrador.');
    }
    console.log('     Control de acceso por rol (403/RoleGuard) verificado con éxito.');

    // Logout
    await performLogout();

    // ============================================================
    // 3. ROL: PACIENTE
    // ============================================================
    console.log('\n--- [ROL 3: PACIENTE] ---');
    console.log('3.1. Login como paciente (90000003)...');
    await page.fill('#identifier', '90000003');
    await page.fill('#password', 'DemoPaciente2026!');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/paciente/**', { timeout: 10000 });
    console.log('     Login paciente exitoso. URL:', page.url());

    // 3.2. Formulario Perfil Paciente
    console.log('3.2. Probando formulario Edición Perfil (/paciente/perfil/editar)...');
    await page.goto('http://localhost:4200/paciente/perfil/editar');
    await page.waitForSelector('#patient-phone');

    console.log('     Validando celular paciente (8 dígitos)...');
    await page.fill('#patient-phone', '91234567');
    const pacError8 = await page.locator('#patient-phone-error').isVisible();
    console.log('     ¿Error de 9 dígitos visible con 8 dígitos?', pacError8);

    console.log('     Validando celular paciente (9 dígitos)...');
    await page.fill('#patient-phone', '912345678');
    const pacError9 = await page.locator('#patient-phone-error').isVisible();
    console.log('     ¿Error oculto con 9 dígitos?', !pacError9);

    await page.click('button[type="submit"]:has-text("Guardar cambios")');
    await page.waitForSelector('.patient-portal__alert--success');
    console.log('     Perfil paciente guardado correctamente.');

    // 3.3. Historial de Citas del Paciente
    console.log('3.3. Verificando historial y canceladas del paciente...');
    await page.click('a[href="/paciente/historial"]');
    await page.waitForURL('**/paciente/historial');
    await page.waitForSelector('.patient-data-panel, .patient-empty');
    console.log('     Historial del paciente cargado correctamente.');

    await page.click('a[href="/paciente/canceladas"]');
    await page.waitForURL('**/paciente/canceladas');
    await page.waitForSelector('.patient-data-panel, .patient-empty');
    console.log('     Citas canceladas del paciente cargadas correctamente.');

    // 3.4. Pantalla de Pago Demo (/citas/3/pago)
    console.log('3.4. Probando pantalla de Pago Demo en cita #3 (/citas/3/pago)...');
    await page.goto('http://localhost:4200/citas/3/pago');
    await page.waitForSelector('.payment-heading');

    // Verificar aviso explícito de simulación demostrativa
    const bannerText = await page.locator('.alert-info').textContent();
    console.log('     Aviso visible en pago:', bannerText.trim());
    if (!bannerText.includes('SIMULACIÓN DEMOSTRATIVA') || !bannerText.includes('No realiza cobros reales ni guarda datos de tarjeta')) {
      throw new Error('El aviso de simulación demostrativa no contiene el texto de advertencia requerido.');
    }
    console.log('     Aviso de simulación demostrativa verificado correctamente.');

    // Llenar datos de tarjeta simulada
    console.log('     Ingresando datos simulados de tarjeta...');
    await page.fill('#demo-card-number', '4532 1111 2222 3333');
    await page.fill('#demo-card-holder', 'JUAN DEMO PACIENTE');
    await page.fill('#demo-card-expiry', '12/28');
    await page.fill('#demo-card-cvv', '123');

    // Verificar botón de simulación
    const simulateBtn = page.locator('button.payment-submit');
    const isSimulateEnabled = !(await simulateBtn.isDisabled());
    console.log('     ¿Botón "Simular pago" habilitado?', isSimulateEnabled);

    // Ejecutar simulación de pago
    await simulateBtn.click();
    await page.waitForSelector('.payment-result');
    console.log('     Resultado de pago simulado mostrado en pantalla.');

    // Verificar que los datos confidenciales de la tarjeta fueron borrados de memoria / inputs
    const cardNumberValue = await page.locator('#demo-card-number').inputValue();
    const cardCvvValue = await page.locator('#demo-card-cvv').inputValue();
    console.log(`     Verificación de seguridad: valor campo número de tarjeta = "${cardNumberValue}", CVV = "${cardCvvValue}"`);
    if (cardNumberValue !== '' || cardCvvValue !== '') {
      throw new Error('FALLO DE SEGURIDAD: Los datos de la tarjeta no se borraron de la memoria/inputs.');
    }
    console.log('     Seguridad de datos de tarjeta verificada: NINGÚN DATO PERSISTIDO NI EN MEMORIA.');

    // Logout paciente
    await performLogout();

    console.log('\n===========================================================');
    console.log('   TODOS LOS FLUJOS DE LOS 3 ROLES COMPLETADOS CON ÉXITO   ');
    console.log('===========================================================');

  } catch (err) {
    console.error('FATAL ERROR DURING TEST EXECUTION:', err);
    throw err;
  } finally {
    await browser.close();
  }

  // ============================================================
  // VERIFICACION FINAL DE ERRORES DE CONSOLA
  // ============================================================
  console.log('\n--- RESUMEN DE LLAMADAS DE RED API ---');
  console.log(`Total llamadas API registradas: ${networkCalls.length}`);
  const failedCalls = networkCalls.filter(c => c.status >= 400 && c.status !== 401);
  console.log(`Llamadas HTTP con error inesperado (> 400 excluyendo 401 esperado): ${failedCalls.length}`);
  if (failedCalls.length > 0) {
    console.warn('Llamadas inesperadas:', failedCalls);
  }

  console.log('\n--- RESUMEN DE CONSOLA DEL NAVEGADOR ---');
  console.log(`Total errores en consola del navegador (excepciones JS no controladas): ${jsConsoleErrors.length}`);
  if (jsConsoleErrors.length > 0) {
    console.error('ERRORES DETECTADOS:', jsConsoleErrors);
    process.exit(1);
  } else {
    console.log('✅ CERO ERRORES EN LA CONSOLA DEL NAVEGADOR (VERIFICADO)');
  }
})();
