import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Appointment } from '../../core/appointments/appointment-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { doctorAvatarUrl } from '../../core/utils/doctor-avatar';
import { AccountProfile, ScheduleRecord, WorkspaceApiService } from '../../core/workspaces/workspace-api.service';

type DoctorSection = 'agenda' | 'schedules' | 'history' | 'profile';

@Component({
  standalone: true,
  selector: 'app-doctor-portal',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './doctor-portal.component.html',
  styleUrl: './doctor-portal.component.scss'
})
export class DoctorPortalComponent implements OnInit {
  readonly activeSection: DoctorSection;
  readonly navigation: Array<{ key: DoctorSection; label: string; icon: string; link: string }> = [
    { key: 'agenda', label: 'Mi agenda del día', icon: '▣', link: '/doctor/agenda' },
    { key: 'schedules', label: 'Mis horarios', icon: '◷', link: '/doctor/horarios' },
    { key: 'history', label: 'Historial de pacientes', icon: '▤', link: '/doctor/historial' },
    { key: 'profile', label: 'Mi perfil', icon: '○', link: '/doctor/perfil' }
  ];

  summary: { id: number; nombre: string; cmp: string; correo: string | null; imagen: string | null; especialidades: string[] } | null = null;
  profile: AccountProfile | null = null;
  profileForm = { correo: '' };
  savingProfile = false;
  appointments: Appointment[] = [];
  schedules: ScheduleRecord[] = [];
  timeBuckets: Array<{ label: string; count: number; pct: number; peak: boolean }> = [];
  statusDonut: Array<{ label: string; count: number; color: string; dashArray: string; dashOffset: number }> = [];
  searchText = '';
  loading = true;
  errorMessage = '';
  successMessage = '';

  constructor(
    private readonly auth: AuthService,
    private readonly api: WorkspaceApiService,
    private readonly router: Router,
    route: ActivatedRoute
  ) {
    this.activeSection = (route.snapshot.data['section'] as DoctorSection | undefined) ?? 'agenda';
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.errorMessage = '';
    forkJoin({
      summary: this.api.doctorSummary(),
      profile: this.api.profile(),
      appointments: this.api.doctorAppointments(),
      schedules: this.api.doctorSchedules()
    }).subscribe({
      next: ({ summary, profile, appointments, schedules }) => {
        this.summary = summary;
        this.profile = profile;
        this.profileForm = { correo: profile.correo ?? summary.correo ?? '' };
        this.appointments = appointments;
        this.schedules = schedules;
        this.recomputeAnalytics();
        this.loading = false;
      },
      error: (err: { error?: { message?: string } }) => {
        this.errorMessage = err?.error?.message ?? 'No se pudo cargar tu agenda médica.';
        this.loading = false;
      }
    });
  }

  get avatarUrl(): string {
    return this.summary?.imagen || doctorAvatarUrl(this.summary?.nombre, 0);
  }

  get activeAppointments(): Appointment[] {
    const q = this.searchText.trim().toLowerCase();
    return this.appointments.filter(a => {
      const isPending = a.estado === 'RESERVADA' || a.estado === 'CONFIRMADA';
      const matches = !q || (a.paciente ?? '').toLowerCase().includes(q) || (a.pacienteDni ?? '').includes(q);
      return isPending && matches;
    });
  }

  get historyAppointments(): Appointment[] {
    const q = this.searchText.trim().toLowerCase();
    return this.appointments.filter(a => {
      const isClosed = a.estado === 'ATENDIDA' || a.estado === 'NO_ASISTIO' || a.estado === 'CANCELADA';
      const matches = !q || (a.paciente ?? '').toLowerCase().includes(q) || (a.pacienteDni ?? '').includes(q);
      return isClosed && matches;
    });
  }

  get nextAppointmentTime(): string {
    const next = this.activeAppointments[0];
    return next ? `${next.fecha} · ${next.hora.slice(0, 5)}` : 'Sin próximas';
  }

  saveProfile(): void {
    const correo = this.profileForm.correo.trim();
    if (!correo || correo.length > 254 || this.savingProfile) return;
    this.savingProfile = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.api.updateProfile({ correo, celular: null, direccion: null }).subscribe({
      next: updated => {
        this.profile = updated;
        if (this.summary) {
          this.summary = { ...this.summary, correo: updated.correo };
        }
        this.profileForm = { correo: updated.correo ?? '' };
        this.successMessage = 'Tu perfil se actualizó correctamente.';
        this.savingProfile = false;
      },
      error: (err: { error?: { message?: string } }) => {
        this.errorMessage = err?.error?.message ?? 'No se pudo actualizar tu perfil.';
        this.savingProfile = false;
      }
    });
  }

  normalizeSearchInput(value: string): string {
    const raw = (value ?? '').slice(0, 100);
    return /^\d+$/.test(raw.trim()) ? raw.trim().slice(0, 8) : raw;
  }

  normalizeSearchInputEvent(event: Event): string {
    const input = event.target as HTMLInputElement | null;
    if (!input) return '';
    const normalized = this.normalizeSearchInput(input.value);
    if (input.value !== normalized) {
      input.value = normalized;
    }
    return normalized;
  }

  get stats(): { total: number; pending: number; attended: number; other: number } {
    const total = this.appointments.length;
    const pending = this.appointments.filter(a => a.estado === 'RESERVADA' || a.estado === 'CONFIRMADA').length;
    const attended = this.appointments.filter(a => a.estado === 'ATENDIDA').length;
    const other = total - pending - attended;
    return { total, pending, attended, other };
  }

  private recomputeAnalytics(): void {
    const ranges = [
      { label: '08-10', min: 8, max: 10 },
      { label: '10-12', min: 10, max: 12 },
      { label: '12-14', min: 12, max: 14 },
      { label: '14-16', min: 14, max: 16 },
      { label: '16-18', min: 16, max: 18 },
      { label: '18-20', min: 18, max: 21 }
    ];
    const counts = ranges.map(r => ({
      label: r.label,
      count: this.appointments.filter(a => {
        const hh = Number((a.hora || '').slice(0, 2));
        return hh >= r.min && hh < r.max;
      }).length
    }));
    const max = counts.reduce((m, c) => Math.max(m, c.count), 1);
    this.timeBuckets = counts.map(c => ({
      label: c.label,
      count: c.count,
      pct: c.count === 0 ? 12 : Math.max(18, Math.round((c.count / max) * 100)),
      peak: c.count > 0 && c.count === max
    }));

    const defs = [
      { key: 'ATENDIDA', label: 'Atendidas', color: '#0A4D2E' },
      { key: 'CONFIRMADA', label: 'Confirmadas', color: '#147A49' },
      { key: 'RESERVADA', label: 'En espera', color: '#D4AF37' },
      { key: 'NO_ASISTIO', label: 'No asistió', color: '#C98A2C' },
      { key: 'CANCELADA', label: 'Canceladas', color: '#B84A4A' }
    ];
    const items = defs.map(d => ({ ...d, count: this.appointments.filter(a => a.estado === d.key).length }))
      .filter(i => i.count > 0);
    const total = items.reduce((acc, i) => acc + i.count, 0) || 1;
    const circumference = 2 * Math.PI * 38;
    let offset = 0;
    this.statusDonut = items.map(i => {
      const arc = (i.count / total) * circumference;
      const seg = {
        label: i.label,
        count: i.count,
        color: i.color,
        dashArray: `${arc.toFixed(2)} ${(circumference - arc).toFixed(2)}`,
        dashOffset: -offset
      };
      offset += arc;
      return seg;
    });
  }

  markStatus(appointment: Appointment, estado: 'ATENDIDA' | 'NO_ASISTIO'): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.api.changeDoctorAppointmentStatus(appointment.id, estado).subscribe({
      next: updated => {
        appointment.estado = updated.estado;
        this.recomputeAnalytics();
        this.successMessage = estado === 'ATENDIDA'
          ? 'Consulta marcada como atendida.'
          : 'Inasistencia registrada.';
      },
      error: (err: { error?: { message?: string } }) => {
        this.errorMessage = err?.error?.message ?? 'No se pudo actualizar el estado de la cita.';
      }
    });
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      RESERVADA: 'En espera',
      CONFIRMADA: 'Confirmada',
      ATENDIDA: 'Atendida',
      NO_ASISTIO: 'No asistió',
      CANCELADA: 'Cancelada'
    };
    return labels[status] ?? status;
  }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login'])
    });
  }
}
