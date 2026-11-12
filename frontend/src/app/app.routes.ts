import { Routes } from '@angular/router';
import { roleGuard } from './core/auth/role.guard';

const patientPortal = () => import('./features/patient/patient-portal.component').then(m => m.PatientPortalComponent);
const staffPortal = () => import('./features/portal/role-portal.component').then(m => m.RolePortalComponent);
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
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'citas' },
      { path: 'disponibilidad', loadComponent: availability },
      { path: 'citas/:id/modificar', loadComponent: patientPortal, data: { section: 'reschedule' } },
      { path: 'citas', loadComponent: patientPortal, data: { section: 'appointments' } },
      { path: 'historial', loadComponent: patientPortal, data: { section: 'history' } },
      { path: 'canceladas', loadComponent: patientPortal, data: { section: 'cancelled' } },
      { path: 'perfil/editar', loadComponent: patientPortal, data: { section: 'editProfile' } },
      { path: 'perfil', loadComponent: patientPortal, data: { section: 'profile' } },
      { path: 'editarPerfil/:dni', redirectTo: 'perfil/editar' },
      { path: 'cita/modificar/:id', redirectTo: 'citas/:id/modificar' }
    ]
  },
  { path: 'citas/:id/confirmacion', loadComponent: () => import('./features/appointments/confirmation.component').then(m => m.ConfirmationComponent),
    canActivate: [roleGuard], data: { roles: ['PACIENTE'] } },
  { path: 'citas/:id/pago', loadComponent: paymentDemo,
    canActivate: [roleGuard], data: { roles: ['PACIENTE'] } },

  {
    path: 'recepcionista', canActivate: [roleGuard], data: { roles: ['RECEPCIONISTA'] },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'citas' },
      { path: 'citas/:id/modificar', loadComponent: staffPortal, data: { section: 'edit' } },
      { path: 'citas', loadComponent: staffPortal, data: { section: 'appointments' } },
      { path: 'historial', loadComponent: staffPortal, data: { section: 'history' } },
      { path: 'perfil', loadComponent: staffPortal, data: { section: 'profile' } },
      { path: 'cita/modificar/:id', redirectTo: 'citas/:id/modificar' },
      { path: 'cita/marcar/:id', redirectTo: 'citas' },
      { path: 'cita/cancelar/:id', redirectTo: 'citas' }
    ]
  },

  {
    path: 'administrador', canActivate: [roleGuard], data: { roles: ['ADMINISTRADOR'] },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'doctores' },
      { path: 'doctores/nuevo', loadComponent: staffPortal, data: { section: 'doctorForm' } },
      { path: 'doctores/:id/editar', loadComponent: staffPortal, data: { section: 'doctorEditForm' } },
      { path: 'doctores', loadComponent: staffPortal, data: { section: 'doctors' } },
      { path: 'horarios/nuevo', loadComponent: staffPortal, data: { section: 'scheduleForm' } },
      { path: 'horarios/:id/editar', loadComponent: staffPortal, data: { section: 'scheduleEditForm' } },
      { path: 'horarios', loadComponent: staffPortal, data: { section: 'schedules' } },
      { path: 'recepcionistas/nuevo', loadComponent: staffPortal, data: { section: 'receptionistForm' } },
      { path: 'recepcionistas/:id/editar', loadComponent: staffPortal, data: { section: 'receptionistEditForm' } },
      { path: 'recepcionistas', loadComponent: staffPortal, data: { section: 'receptionists' } },
      { path: 'perfil', loadComponent: staffPortal, data: { section: 'profile' } },
      { path: 'doctor/nuevo', redirectTo: 'doctores/nuevo' },
      { path: 'doctor/modificar', redirectTo: 'doctores' },
      { path: 'horario/nuevo', redirectTo: 'horarios/nuevo' },
      { path: 'horario/modificar/:id', redirectTo: 'horarios/:id/editar' },
      { path: 'horario/apertura/:id', redirectTo: 'horarios' },
      { path: 'horario/cierre/:id', redirectTo: 'horarios' },
      { path: 'recepcionista/nuevo', redirectTo: 'recepcionistas/nuevo' },
      { path: 'recepcionista/modificar/:id', redirectTo: 'recepcionistas/:id/editar' },
      { path: 'recepcionista/activar/:id', redirectTo: 'recepcionistas' },
      { path: 'recepcionista/desactivar/:id', redirectTo: 'recepcionistas' }
    ]
  },

  { path: 'portalPaciente', redirectTo: 'paciente/citas', pathMatch: 'full' },
  { path: 'pruebaportalpaciente', redirectTo: 'paciente/citas', pathMatch: 'full' },
  { path: 'portalRecepcionista', redirectTo: 'recepcionista/citas', pathMatch: 'full' },
  { path: 'portalAdministrador', redirectTo: 'administrador/doctores', pathMatch: 'full' },
  { path: 'prueba', redirectTo: 'pago', pathMatch: 'full' },
  { path: 'prueba2', redirectTo: 'paciente/disponibilidad', pathMatch: 'full' },
  { path: '**', redirectTo: 'error' }
];
