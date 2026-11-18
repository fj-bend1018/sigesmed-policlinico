import { Routes } from '@angular/router';
import { DoctorPortalComponent } from './doctor-portal.component';

export const DOCTOR_ROUTES: Routes = [
  { path: '', redirectTo: 'agenda', pathMatch: 'full' },
  { path: 'agenda', component: DoctorPortalComponent, data: { section: 'agenda' } },
  { path: 'horarios', component: DoctorPortalComponent, data: { section: 'schedules' } },
  { path: 'historial', component: DoctorPortalComponent, data: { section: 'history' } },
  { path: 'perfil', component: DoctorPortalComponent, data: { section: 'profile' } }
];
