import { Routes } from '@angular/router';

export const RECEPCIONISTA_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'citas' },
  { path: 'citas/:id/modificar', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'edit' } },
  { path: 'citas', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'appointments' } },
  { path: 'historial', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'history' } },
  { path: 'perfil', loadComponent: () => import('./role-portal.component').then(m => m.RolePortalComponent), data: { section: 'profile' } },
  { path: 'cita/modificar/:id', redirectTo: 'citas/:id/modificar' },
  { path: 'cita/marcar/:id', redirectTo: 'citas' },
  { path: 'cita/cancelar/:id', redirectTo: 'citas' }
];
