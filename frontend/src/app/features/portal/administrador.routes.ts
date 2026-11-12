import { Routes } from '@angular/router';

export const ADMINISTRADOR_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'doctores' },
  { path: 'doctores/nuevo', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'doctorForm' } },
  { path: 'doctores/:id/editar', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'doctorEditForm' } },
  { path: 'doctores', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'doctors' } },
  { path: 'horarios/nuevo', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'scheduleForm' } },
  { path: 'horarios/:id/editar', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'scheduleEditForm' } },
  { path: 'horarios', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'schedules' } },
  { path: 'recepcionistas/nuevo', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'receptionistForm' } },
  { path: 'recepcionistas/:id/editar', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'receptionistEditForm' } },
  { path: 'recepcionistas', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'receptionists' } },
  { path: 'perfil', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'profile' } },
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
];

