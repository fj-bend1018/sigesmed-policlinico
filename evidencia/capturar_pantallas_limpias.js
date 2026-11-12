const pw = require('C:/Users/FABRIZIO/AppData/Roaming/npm/node_modules/omniroute/node_modules/playwright');
const path = require('path');
const fs = require('fs');

const OUT_DIR = path.join(__dirname, 'capturas_limpias');
if (!fs.existsSync(OUT_DIR)) {
  fs.mkdirSync(OUT_DIR, { recursive: true });
}

const ADMIN_PASS = process.env.SIGESMED_DEV_ADMIN_PASSWORD || 'DemoAdmin2026!';
const RECEP_PASS = process.env.SIGESMED_DEV_RECEPCIONISTA_PASSWORD || 'DemoRecep2026!';
const PACIENTE_PASS = process.env.SIGESMED_DEV_PACIENTE_PASSWORD || 'DemoPaciente2026!';

async function waitForCleanContent(page, contextName, minSelector = null) {
  // 1. Esperar a que la pantalla de carga ("Preparando el sistema") desaparezca por completo del DOM
  try {
    await page.waitForSelector('.startup-splash', { state: 'detached', timeout: 12000 });
  } catch (err) {
    try {
      await page.waitForSelector('.startup-splash', { state: 'hidden', timeout: 5000 });
    } catch (e) {}
  }

  // 2. Margen de 600 ms tras la desaparición para estabilización visual y transiciones CSS
  await page.waitForTimeout(600);

  // 3. Si se especificó un selector clave del contenido real, esperar a que esté visible
  if (minSelector) {
    try {
      await page.waitForSelector(minSelector, { state: 'visible', timeout: 8000 });
    } catch (err) {
      console.warn(`     [AVISO]: No se detectó selector '${minSelector}' a tiempo en ${contextName}`);
    }
  }

  // 4. Verificación exhaustiva: corroborar que .startup-splash no exista o no sea visible
  const splashCheck = await page.evaluate(() => {
    const splash = document.querySelector('.startup-splash');
    if (!splash) return { exists: false, visible: false };
    const style = window.getComputedStyle(splash);
    const visible = style.display !== 'none' && style.opacity !== '0' && style.visibility !== 'hidden';
    return { exists: true, visible };
  });

  if (splashCheck.visible) {
    console.warn(`     [AVISO]: El splash aún parecía visible en ${contextName}, esperando 1.5s adicionales...`);
    await page.waitForTimeout(1500);
  }

  // 5. Verificar que el texto "Preparando el sistema" no esté visible en primer plano
  const hasSplashText = await page.evaluate(() => {
    const splashCaption = document.querySelector('.startup-splash__caption');
    if (!splashCaption) return false;
    const style = window.getComputedStyle(splashCaption);
    return style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0';
  });

  if (hasSplashText) {
    console.error(`     [ERROR]: ¡El texto 'Preparando el sistema' sigue visible en ${contextName}!`);
  } else {
    console.log(`     [LIMPIEZA OK]: Pantalla de carga ausente. Contenido real listo en ${contextName}.`);
  }
}

async function performLogout(page) {
  try {
    const logoutBtn = page.locator('button:has-text("Cerrar sesión")');
    if (await logoutBtn.isVisible()) {
      await logoutBtn.click();
      await page.waitForURL('**/login', { timeout: 8000 }).catch(() => {});
    } else {
      await page.evaluate(() => {
        sessionStorage.clear();
        localStorage.clear();
      });
      await page.goto('http://localhost:4200/login', { waitUntil: 'networkidle' });
    }
  } catch (e) {
    await page.evaluate(() => {
      sessionStorage.clear();
      localStorage.clear();
    });
    await page.goto('http://localhost:4200/login', { waitUntil: 'networkidle' });
  }
  await page.waitForTimeout(400);
}

(async () => {
  console.log('================================================================');
  console.log('   GENERACIÓN DE CAPTURAS LIMPIAS (SIN PANTALLA DE CARGA)      ');
  console.log('   Directorio destino: evidencia/capturas_limpias/              ');
  console.log('================================================================');

  const browser = await pw.chromium.launch({
    channel: 'chrome',
    headless: true
  });

  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1
  });
  const page = await context.newPage();

  page.on('dialog', async d => {
    await d.accept();
  });

  const viewports = [
    { name: '1440', width: 1440, height: 900 },
    { name: '360', width: 360, height: 800 }
  ];

  try {
    // ----------------------------------------------------------------
    // 1. CAPTURAS PÚBLICAS: LANDING, LOGIN, REGISTRO, RECUPERAR, DISPONIBILIDAD
    // ----------------------------------------------------------------
    const publicPages = [
      { name: 'landing', route: '/', selector: '.landing-hero, .hero-content, #contenido-principal' },
      { name: 'login', route: '/login', selector: '.auth-card, form, #identifier' },
      { name: 'registro', route: '/registro', selector: '.auth-card, form, #dni' },
      { name: 'recuperar', route: '/recuperar', selector: '.auth-card, form' },
      { name: 'disponibilidad', route: '/reservar', selector: '.availability-view, form, .filter-grid' }
    ];

    for (const p of publicPages) {
      console.log(`\n>>> Procesando página pública: ${p.name.toUpperCase()} (${p.route})`);
      for (const vp of viewports) {
        await page.setViewportSize({ width: vp.width, height: vp.height });
        await page.goto(`http://localhost:4200${p.route}`, { waitUntil: 'networkidle' });
        await waitForCleanContent(page, `${p.name} (${vp.name}px)`, p.selector);

        const outPath = path.join(OUT_DIR, `${p.name}_${vp.name}.png`);
        await page.screenshot({ path: outPath, fullPage: true });
        console.log(`   📸 [GUARDADA]: ${path.basename(outPath)} (${vp.width}x${vp.height})`);
      }
    }

    // ----------------------------------------------------------------
    // 2. PORTAL PACIENTE
    // ----------------------------------------------------------------
    console.log('\n>>> Procesando PORTAL PACIENTE (/paciente/citas)');
    for (const vp of viewports) {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await page.goto('http://localhost:4200/login', { waitUntil: 'networkidle' });
      await waitForCleanContent(page, `login paciente (${vp.name}px)`, '#identifier');

      await page.fill('#identifier', '90000003');
      await page.fill('#password', PACIENTE_PASS);
      await page.click('button[type="submit"]');
      await page.waitForURL('**/paciente/**', { timeout: 10000 });

      // Esperar navegación interna a citas
      await page.goto('http://localhost:4200/paciente/citas', { waitUntil: 'networkidle' });
      await waitForCleanContent(page, `portal_paciente (${vp.name}px)`, '.patient-portal, .appointment-card, table, h1, h2');

      const outPath = path.join(OUT_DIR, `portal_paciente_${vp.name}.png`);
      await page.screenshot({ path: outPath, fullPage: true });
      console.log(`   📸 [GUARDADA]: ${path.basename(outPath)} (${vp.width}x${vp.height})`);

      await performLogout(page);
    }

    // ----------------------------------------------------------------
    // 3. PORTAL RECEPCIONISTA
    // ----------------------------------------------------------------
    console.log('\n>>> Procesando PORTAL RECEPCIONISTA (/recepcionista/citas)');
    for (const vp of viewports) {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await page.goto('http://localhost:4200/login', { waitUntil: 'networkidle' });
      await waitForCleanContent(page, `login recepcionista (${vp.name}px)`, '#identifier');

      await page.fill('#identifier', '90000002');
      await page.fill('#password', RECEP_PASS);
      await page.click('button[type="submit"]');
      await page.waitForURL('**/recepcionista/**', { timeout: 10000 });

      await page.goto('http://localhost:4200/recepcionista/citas', { waitUntil: 'networkidle' });
      await waitForCleanContent(page, `portal_recepcionista (${vp.name}px)`, '.role-portal, table, h1, h2');

      const outPath = path.join(OUT_DIR, `portal_recepcionista_${vp.name}.png`);
      await page.screenshot({ path: outPath, fullPage: true });
      console.log(`   📸 [GUARDADA]: ${path.basename(outPath)} (${vp.width}x${vp.height})`);

      await performLogout(page);
    }

    // ----------------------------------------------------------------
    // 4. PORTAL ADMINISTRADOR
    // ----------------------------------------------------------------
    console.log('\n>>> Procesando PORTAL ADMINISTRADOR (/administrador/doctores)');
    for (const vp of viewports) {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await page.goto('http://localhost:4200/login', { waitUntil: 'networkidle' });
      await waitForCleanContent(page, `login admin (${vp.name}px)`, '#identifier');

      await page.fill('#identifier', '90000001');
      await page.fill('#password', ADMIN_PASS);
      await page.click('button[type="submit"]');
      await page.waitForURL('**/administrador/**', { timeout: 10000 });

      await page.goto('http://localhost:4200/administrador/doctores', { waitUntil: 'networkidle' });
      await waitForCleanContent(page, `portal_administrador (${vp.name}px)`, '.role-portal, table, h1, h2');

      const outPath = path.join(OUT_DIR, `portal_administrador_${vp.name}.png`);
      await page.screenshot({ path: outPath, fullPage: true });
      console.log(`   📸 [GUARDADA]: ${path.basename(outPath)} (${vp.width}x${vp.height})`);

      await performLogout(page);
    }

    console.log('\n================================================================');
    console.log('   VERIFICACIÓN FINAL DE ARCHIVOS GENERADOS                    ');
    console.log('================================================================');
    const files = fs.readdirSync(OUT_DIR);
    console.log(`Total de capturas en ${OUT_DIR}: ${files.length} archivos`);
    files.forEach(f => {
      const stats = fs.statSync(path.join(OUT_DIR, f));
      console.log(` - ${f} (${(stats.size / 1024).toFixed(1)} KB)`);
    });

  } catch (err) {
    console.error('Error durante la captura:', err);
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
})();

