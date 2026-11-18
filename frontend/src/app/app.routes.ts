import { Routes } from '@angular/router';
import { roleGuard } from './core/auth/role.guard';

const legacyWorkflow = () => import('./features/legacy/legacy-workflow.component').then(m => m.LegacyWorkflowComponent);
const publicHome = () => import('./features/home/home.component').then(m => m.HomeComponent);
const availability = () => import('./features/appointments/availability.component').then(m => m.AvailabilityComponent);
const paymentDemo = () => import('./features/appointments/payment-demo.component').then(m => m.PaymentDemoComponent);

export const routes: Routes = [
  { path: '', pathMatch: 'full', loadComponent: publicHome },
  { path: 'index', redirectTo: '', pathMatch: 'full' },
  { path: 'index.html', redirectTo: '', pathMatch: 'full' },
  { path: 'login.html', redirectTo: 'login', pathMatch: 'full' },
  { path: 'formulario-dni.html', redirectTo: 'formulario-dni', pathMatch: 'full' },
  { path: 'formulario-cita.html', redirectTo: 'formulario-cita', pathMatch: 'full' },
  { path: 'confirmacion.html', redirectTo: 'confirmacion', pathMatch: 'full' },
  { path: 'pago.html', redirectTo: 'pago', pathMatch: 'full' },
  { path: 'horario.html', redirectTo: 'horario', pathMatch: 'full' },
  { path: 'error.html', redirectTo: 'error', pathMatch: 'full' },
  { path: 'portalPaciente.html', redirectTo: 'portalPaciente', pathMatch: 'full' },
  { path: 'portalRecepcionista.html', redirectTo: 'portalRecepcionista', pathMatch: 'full' },
  { path: 'portalAdministrador.html', redirectTo: 'portalAdministrador', pathMatch: 'full' },
  { path: 'pruebaportalpaciente.html', redirectTo: 'pruebaportalpaciente', pathMatch: 'full' },
  { path: 'prueba.html', redirectTo: 'prueba', pathMatch: 'full' },
  { path: 'prueba2.html', redirectTo: 'prueba2', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },
  { path: 'registro', loadComponent: () => import('./features/auth/register.component').then(m => m.RegisterComponent) },
  { path: 'recuperar', loadComponent: () => import('./features/auth/recover.component').then(m => m.RecoverComponent) },
  { path: 'restablecer', loadComponent: () => import('./features/auth/reset.component').then(m => m.ResetComponent) },
  { path: 'reservar', loadComponent: availability },

  { path: 'formulario-dni', loadComponent: legacyWorkflow, data: { view: 'identification' } },
  { path: 'formulario-cita', loadComponent: legacyWorkflow, data: { view: 'appointment' } },
  { path: 'pago', loadComponent: legacyWorkflow, data: { view: 'payment' } },
  { path: 'error', loadComponent: legacyWorkflow, data: { view: 'error' } },
  { path: 'horario', redirectTo: 'paciente/disponibilidad', pathMatch: 'full' },
  { path: 'confirmacion', loadComponent: legacyWorkflow, data: { view: 'confirmation' } },
  { path: 'confirmacion/:id', redirectTo: 'citas/:id/confirmacion', pathMatch: 'full' },

  {
    path: 'paciente', canActivate: [roleGuard], data: { roles: ['PACIENTE'] },
    loadChildren: () => import('./features/patient/patient.routes').then(m => m.PATIENT_ROUTES)
  },
  { path: 'citas/:id/confirmacion', loadComponent: () => import('./features/appointments/confirmation.component').then(m => m.ConfirmationComponent),
    canActivate: [roleGuard], data: { roles: ['PACIENTE'] } },
  { path: 'citas/:id/pago', loadComponent: paymentDemo,
    canActivate: [roleGuard], data: { roles: ['PACIENTE'] } },

  {
    path: 'recepcionista', canActivate: [roleGuard], data: { roles: ['RECEPCIONISTA'] },
    loadChildren: () => import('./features/portal/recepcionista.routes').then(m => m.RECEPCIONISTA_ROUTES)
  },

  {
    path: 'administrador', canActivate: [roleGuard], data: { roles: ['ADMINISTRADOR'] },
    loadChildren: () => import('./features/portal/administrador.routes').then(m => m.ADMINISTRADOR_ROUTES)
  },

  {
    path: 'doctor', canActivate: [roleGuard], data: { roles: ['DOCTOR'] },
    loadChildren: () => import('./features/doctor/doctor.routes').then(m => m.DOCTOR_ROUTES)
  },

  { path: 'portalPaciente', redirectTo: 'paciente/citas', pathMatch: 'full' },
  { path: 'pruebaportalpaciente', redirectTo: 'paciente/citas', pathMatch: 'full' },
  { path: 'portalRecepcionista', redirectTo: 'recepcionista/citas', pathMatch: 'full' },
  { path: 'portalAdministrador', redirectTo: 'administrador/doctores', pathMatch: 'full' },
  { path: 'prueba', redirectTo: 'pago', pathMatch: 'full' },
  { path: 'prueba2', redirectTo: 'paciente/disponibilidad', pathMatch: 'full' },
  { path: '**', redirectTo: 'error' }
];
