import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Appointment, AppointmentApiService, AvailableSlot } from '../../core/appointments/appointment-api.service';
import { AccountProfile, AppointmentView, WorkspaceApiService } from '../../core/workspaces/workspace-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { hasValidEmail, hasValidOptionalLength, hasValidOptionalPeruMobile, isValidIsoDate } from '../../core/forms/field-rules';

type PatientSection = 'appointments' | 'history' | 'cancelled' | 'profile' | 'editProfile' | 'reschedule';

@Component({
  standalone: true,
  selector: 'app-patient-portal',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './patient-portal.component.html',
  styleUrls: ['./patient-portal.component.scss']
})
export class PatientPortalComponent implements OnInit {
  readonly section: PatientSection;
  readonly fallbackDni: string;
  readonly appointmentId: number;
  readonly sections: Array<{ key: PatientSection; label: string; icon: string; link: string }> = [
    { key: 'appointments', label: 'Mis citas', icon: '▣', link: '/paciente/citas' },
    { key: 'history', label: 'Historial', icon: '▤', link: '/paciente/historial' },
    { key: 'cancelled', label: 'Citas canceladas', icon: '×', link: '/paciente/canceladas' },
    { key: 'profile', label: 'Mi perfil', icon: '○', link: '/paciente/perfil' },
    { key: 'editProfile', label: 'Editar perfil', icon: '✎', link: '/paciente/perfil/editar' }
  ];

  appointments: Appointment[] = [];
  profile: AccountProfile | null = null;
  profileForm = { correo: '', celular: '', direccion: '' };
  appointment: Appointment | null = null;
  slots: AvailableSlot[] = [];
  selectedSlotId = '';
  rescheduleDate = '';
  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  constructor(route: ActivatedRoute, auth: AuthService, private readonly workspaceApi: WorkspaceApiService,
      private readonly appointmentApi: AppointmentApiService) {
    this.section = (route.snapshot.data['section'] as PatientSection | undefined) ?? 'appointments';
    this.fallbackDni = auth.currentSession()?.dni ?? '';
    this.appointmentId = Number(route.snapshot.paramMap.get('id'));
  }

  ngOnInit(): void {
    if (this.section === 'appointments' || this.section === 'history' || this.section === 'cancelled') {
      this.loadAppointments();
    } else if (this.section === 'profile' || this.section === 'editProfile') {
      this.loadProfile();
    } else {
      this.loadAppointmentForReschedule();
    }
  }

  get title(): string {
    const titles: Record<PatientSection, string> = {
      appointments: 'Mis citas', history: 'Historial de citas', cancelled: 'Citas canceladas',
      profile: 'Mi perfil', editProfile: 'Editar perfil', reschedule: 'Modificar cita'
    };
    return titles[this.section];
  }

  get description(): string {
    const descriptions: Record<PatientSection, string> = {
      appointments: 'Consulta y organiza tus próximas atenciones.',
      history: 'Revisa las atenciones registradas en tu cuenta.',
      cancelled: 'Consulta las citas que fueron canceladas.',
      profile: 'Revisa los datos asociados a tu cuenta.',
      editProfile: 'Actualiza la información de contacto de tu cuenta.',
      reschedule: 'Elige otro horario para tu cita reservada.'
    };
    return descriptions[this.section];
  }

  get dni(): string { return this.profile?.dni ?? this.fallbackDni; }
  get minDate(): string { return this.localDate(new Date()); }

  normalizeCellularInput(value: string): string { return value.replace(/\D/g, '').slice(0, 9); }
  normalizeCellularInputEvent(event: Event): string {
    const input = event.target as HTMLInputElement;
    const value = this.normalizeCellularInput(input.value);
    if (input.value !== value) {
      const caret = input.selectionStart;
      input.value = value;
      if (caret !== null) input.setSelectionRange(Math.min(caret, value.length), Math.min(caret, value.length));
    }
    return value;
  }
  isInvalidCellular(value: string): boolean { return value.length > 0 && !/^[0-9]{9}$/.test(value); }
  selectInvalidCellular(event: FocusEvent, value: string): void {
    if (this.isInvalidCellular(value)) (event.target as HTMLInputElement).select();
  }

  get currentView(): AppointmentView {
    if (this.section === 'history') return 'HISTORIAL';
    if (this.section === 'cancelled') return 'CANCELADAS';
    return 'PROXIMAS';
  }

  loadAppointments(): void {
    this.loading = true;
    this.errorMessage = '';
    this.workspaceApi.myAppointments(this.currentView).subscribe({
      next: items => { this.appointments = items; this.loading = false; },
      error: error => { this.errorMessage = this.messageFrom(error, 'No se pudieron cargar tus citas.'); this.loading = false; }
    });
  }

  loadProfile(): void {
    this.loading = true;
    this.errorMessage = '';
    this.workspaceApi.profile().subscribe({
      next: profile => {
        this.profile = profile;
        this.profileForm = {
          correo: profile.correo ?? '', celular: profile.celular ?? '', direccion: profile.direccion ?? ''
        };
        this.loading = false;
      },
      error: error => { this.errorMessage = this.messageFrom(error, 'No se pudo cargar tu perfil.'); this.loading = false; }
    });
  }

  saveProfile(): void {
    if (this.saving) return;
    this.profileForm.correo = this.profileForm.correo.trim();
    this.profileForm.celular = this.profileForm.celular.trim();
    this.profileForm.direccion = this.profileForm.direccion.trim();
    if (!hasValidEmail(this.profileForm.correo)) {
      this.errorMessage = 'Ingresa un correo válido de hasta 254 caracteres.';
      return;
    }
    if (!hasValidOptionalPeruMobile(this.profileForm.celular)) {
      this.errorMessage = 'El celular debe contener exactamente 9 dígitos.';
      return;
    }
    if (!hasValidOptionalLength(this.profileForm.direccion, 300)) {
      this.errorMessage = 'La dirección admite hasta 300 caracteres.';
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.workspaceApi.updateProfile(this.profileForm).subscribe({
      next: profile => {
        this.profile = profile;
        this.profileForm = { correo: profile.correo ?? '', celular: profile.celular ?? '', direccion: profile.direccion ?? '' };
        this.successMessage = 'Tus datos se actualizaron.';
        this.saving = false;
      },
      error: error => { this.errorMessage = this.messageFrom(error, 'No se pudo actualizar tu perfil.'); this.saving = false; }
    });
  }

  cancelAppointment(appointment: Appointment): void {
    if (appointment.estado !== 'RESERVADA' || !window.confirm('¿Deseas cancelar esta cita?')) return;
    this.errorMessage = '';
    this.successMessage = '';
    this.workspaceApi.cancelMyAppointment(appointment.id, '').subscribe({
      next: () => { this.successMessage = 'La cita se canceló y su horario quedó disponible.'; this.loadAppointments(); },
      error: error => { this.errorMessage = this.messageFrom(error, 'No se pudo cancelar la cita.'); }
    });
  }

  private loadAppointmentForReschedule(): void {
    if (!Number.isSafeInteger(this.appointmentId) || this.appointmentId <= 0) {
      this.errorMessage = 'La cita indicada no es válida.';
      return;
    }
    this.loading = true;
    this.appointmentApi.appointment(this.appointmentId).subscribe({
      next: appointment => {
        this.appointment = appointment;
        this.rescheduleDate = this.tomorrow();
        this.loading = false;
        this.loadSlots();
      },
      error: error => { this.errorMessage = this.messageFrom(error, 'No se pudo consultar la cita.'); this.loading = false; }
    });
  }

  loadSlots(): void {
    if (!this.appointment || !this.rescheduleDate) return;
    if (!isValidIsoDate(this.rescheduleDate) || this.rescheduleDate < this.minDate) {
      this.slots = [];
      this.selectedSlotId = '';
      this.errorMessage = 'Elige una fecha futura para reprogramar la cita.';
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    const request = this.appointment.servicioId
      ? this.appointmentApi.serviceSlots(this.appointment.servicioId, this.rescheduleDate)
      : this.appointment.doctorId
        ? this.appointmentApi.slots(this.appointment.doctorId, this.rescheduleDate)
        : null;
    if (!request) {
      this.loading = false;
      this.errorMessage = 'Esta cita no tiene un servicio o profesional asociado para reprogramarla.';
      return;
    }
    request.subscribe({
      next: slots => { this.slots = slots; this.selectedSlotId = ''; this.loading = false; },
      error: error => { this.slots = []; this.errorMessage = this.messageFrom(error, 'No se pudieron cargar los horarios.'); this.loading = false; }
    });
  }

  reschedule(): void {
    if (!this.appointment || !this.selectedSlotId || this.saving) return;
    if (!isValidIsoDate(this.rescheduleDate) || this.rescheduleDate < this.minDate || !Number.isSafeInteger(Number(this.selectedSlotId))
        || Number(this.selectedSlotId) <= 0) {
      this.errorMessage = 'Selecciona una fecha futura y un horario disponible.';
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.workspaceApi.rescheduleMyAppointment(this.appointment.id, Number(this.selectedSlotId)).subscribe({
      next: () => { this.successMessage = 'La cita se reprogramó.'; this.saving = false; },
      error: error => { this.errorMessage = this.messageFrom(error, 'No se pudo reprogramar la cita.'); this.saving = false; }
    });
  }

  specialties(appointment: Appointment): string { return appointment.servicio ?? appointment.especialidades.join(', '); }
  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      RESERVADA: 'Reservada', CONFIRMADA: 'Confirmada', ATENDIDA: 'Atendida',
      CANCELADA: 'Cancelada', NO_ASISTIO: 'No asistió'
    };
    return labels[status] ?? status;
  }

  private tomorrow(): string {
    const value = new Date();
    value.setDate(value.getDate() + 1);
    return this.localDate(value);
  }

  private localDate(value: Date): string {
    return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
  }

  private messageFrom(error: unknown, fallback: string): string {
    const err = error as { status?: number; error?: { message?: string } };
    if (err?.status === 0) {
      return 'No se pudo conectar con el servidor. Verifica tu conexión a internet o que el sistema esté disponible.';
    }
    if (err?.status === 401) {
      return 'Tu sesión ha expirado. Por favor, vuelve a iniciar sesión.';
    }
    if (err?.status === 403) {
      return 'Acceso denegado: no tienes permisos para realizar esta operación.';
    }
    if (err?.status === 409) {
      return err.error?.message || 'Existe un conflicto con la información enviada (el horario ya no está disponible).';
    }
    return err?.error?.message ?? fallback;
  }
}
