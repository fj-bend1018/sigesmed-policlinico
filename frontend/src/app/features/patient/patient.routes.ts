import { Routes } from '@angular/router';

export const PATIENT_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'citas' },
  { path: 'disponibilidad', loadComponent: () => import('../appointments/availability.component').then(m => m.AvailabilityComponent) },
  { path: 'citas/:id/modificar', loadComponent: () => import('./patient-portal.component').then(m => m.PatientPortalComponent), data: { section: 'reschedule' } },
  { path: 'citas', loadComponent: () => import('./patient-portal.component').then(m => m.PatientPortalComponent), data: { section: 'appointments' } },
  { path: 'historial', loadComponent: () => import('./patient-portal.component').then(m => m.PatientPortalComponent), data: { section: 'history' } },
  { path: 'canceladas', loadComponent: () => import('./patient-portal.component').then(m => m.PatientPortalComponent), data: { section: 'cancelled' } },
  { path: 'perfil/editar', loadComponent: () => import('./patient-portal.component').then(m => m.PatientPortalComponent), data: { section: 'editProfile' } },
  { path: 'perfil', loadComponent: () => import('./patient-portal.component').then(m => m.PatientPortalComponent), data: { section: 'profile' } },
  { path: 'editarPerfil/:dni', redirectTo: 'perfil/editar' },
  { path: 'cita/modificar/:id', redirectTo: 'citas/:id/modificar' }
];
