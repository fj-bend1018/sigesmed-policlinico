import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Appointment, AppointmentApiService, AvailableSlot, ComplementaryService, Specialty } from '../../core/appointments/appointment-api.service';
import { AccountProfile, DoctorRecord, ReceptionistRecord, ScheduleRecord, WorkspaceApiService } from '../../core/workspaces/workspace-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { UserRole } from '../../core/auth/auth.models';
import { DNI_PATTERN, hasValidEmail, hasValidOptionalLength, hasValidOptionalPeruMobile, isValidIsoDate } from '../../core/forms/field-rules';
import { doctorAvatarUrl } from '../../core/utils/doctor-avatar';

type PortalSection = 'appointments' | 'edit' | 'history' | 'profile' | 'doctors' | 'schedules' | 'receptionists'
  | 'doctorForm' | 'doctorEditForm' | 'scheduleForm' | 'scheduleEditForm' | 'receptionistForm' | 'receptionistEditForm';
interface PortalNavigationItem { key: PortalSection; label: string; icon: string; link: string; }

@Component({
  standalone: true,
  selector: 'app-role-portal',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './role-portal.component.html',
  styleUrl: './role-portal.component.scss'
})
export class RolePortalComponent implements OnInit {
  readonly role: UserRole;
  readonly weekdays = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
  readonly activeSection: PortalSection;
  readonly recordId: number;
  appointments: Appointment[] = [];
  appointment: Appointment | null = null;
  appointmentSlots: AvailableSlot[] = [];
  selectedSlotId = '';
  rescheduleDate = '';
  searchText = '';
  appointmentDate = '';
  profile: AccountProfile | null = null;

  doctorAvatar(name: string, index: number): string {
    return doctorAvatarUrl(name, index, this.doctors);
  }
  profileForm = { correo: '', celular: '', direccion: '' };
  doctors: DoctorRecord[] = [];
  specialties: Specialty[] = [];
  complementaryServices: ComplementaryService[] = [];
  schedules: ScheduleRecord[] = [];
  receptionists: ReceptionistRecord[] = [];
  doctorForm = { nombre: '', cmp: 'DEV-CMP-', especialidadIds: [] as number[] };
  scheduleForm: { tipoRecurso: 'DOCTOR' | 'SERVICIO'; doctorId: string; servicioId: string; fecha: string; hora: string } =
    { tipoRecurso: 'DOCTOR', doctorId: '', servicioId: '', fecha: '', hora: '' };
  receptionistForm = {
    nombre: '', apellidoPaterno: '', apellidoMaterno: '', dni: '', correo: '', celular: '', direccion: '', claveTemporal: ''
  };
  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';
  doctorSearch = '';
  doctorSpecialtyFilter = '';

  constructor(private readonly auth: AuthService, route: ActivatedRoute, private readonly api: WorkspaceApiService,
      private readonly appointmentApi: AppointmentApiService, private readonly router: Router) {
    this.role = auth.currentSession()?.rol ?? 'RECEPCIONISTA';
    this.activeSection = (route.snapshot.data['section'] as PortalSection | undefined)
      ?? (this.role === 'ADMINISTRADOR' ? 'doctors' : 'appointments');
    this.recordId = Number(route.snapshot.paramMap.get('id'));
  }

  ngOnInit(): void {
    switch (this.activeSection) {
      case 'appointments': case 'history': this.loadAppointments(); break;
      case 'profile': this.loadProfile(); break;
      case 'edit': this.loadAppointment(); break;
      case 'doctors': this.loadDoctors(); break;
      case 'doctorForm': this.loadDoctorForm(false); break;
      case 'doctorEditForm': this.loadDoctorForm(true); break;
      case 'schedules': this.loadSchedules(); break;
      case 'scheduleForm': this.loadScheduleForm(false); break;
      case 'scheduleEditForm': this.loadScheduleForm(true); break;
      case 'receptionists': this.loadReceptionists(); break;
      case 'receptionistForm': this.loadReceptionistForm(false); break;
      case 'receptionistEditForm': this.loadReceptionistForm(true); break;
    }
  }

  get isAdmin(): boolean { return this.role === 'ADMINISTRADOR'; }
  get roleLabel(): string { return this.isAdmin ? 'Administrador' : 'Recepcionista'; }
  get portalTitle(): string { return this.isAdmin ? 'Portal de administración' : 'Portal de recepción'; }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login'])
    });
  }
  get minDate(): string { return this.localDate(new Date()); }
  get activeDoctors(): DoctorRecord[] { return this.doctors.filter(doctor => doctor.estado === 'ACTIVO'); }

  get filteredDoctors(): DoctorRecord[] {
    const q = this.doctorSearch.trim().toLowerCase();
    return this.doctors.filter(doc => {
      const matchesQuery = !q || doc.nombre.toLowerCase().includes(q) || doc.cmp.toLowerCase().includes(q);
      const matchesSpec = !this.doctorSpecialtyFilter || doc.especialidades.includes(this.doctorSpecialtyFilter);
      return matchesQuery && matchesSpec;
    });
  }

  get specialtyFilterOptions(): string[] {
    const set = new Set<string>();
    this.doctors.forEach(d => d.especialidades.forEach(s => set.add(s)));
    return Array.from(set).sort((a, b) => a.localeCompare(b, 'es'));
  }

  doctorSpecialtyStats: Array<{ label: string; count: number; pct: number; barPct: number; color: string; dashArray: string; dashOffset: number }> = [];
  scheduleTimeBuckets: Array<{ label: string; count: number; pct: number; peak: boolean }> = [];
  receptionTimeBuckets: Array<{ label: string; count: number; pct: number; peak: boolean }> = [];
  receptionStatusDonut: Array<{ label: string; count: number; color: string; dashArray: string; dashOffset: number }> = [];

  private recomputeDoctorAnalytics(): void {
    const palette = ['#06331E', '#0A4D2E', '#147A49', '#D4AF37', '#2E8B57', '#6B9E78'];
    const counts = new Map<string, number>();
    this.doctors.forEach(d => {
      d.especialidades.forEach(spec => counts.set(spec, (counts.get(spec) ?? 0) + 1));
    });
    const entries = Array.from(counts.entries()).sort((a, b) => b[1] - a[1]);
    const total = entries.reduce((acc, [, c]) => acc + c, 0) || 1;
    const maxCount = entries.reduce((m, [, c]) => Math.max(m, c), 1);
    const circumference = 2 * Math.PI * 38;
    let offset = 0;
    this.doctorSpecialtyStats = entries.map(([label, count], idx) => {
      const pct = Math.round((count / total) * 100);
      const arc = (count / total) * circumference;
      const item = {
        label,
        count,
        pct,
        barPct: Math.max(18, Math.round((count / maxCount) * 100)),
        color: palette[idx % palette.length],
        dashArray: `${arc.toFixed(2)} ${(circumference - arc).toFixed(2)}`,
        dashOffset: -offset
      };
      offset += arc;
      return item;
    });
  }

  get scheduleStats(): { total: number; available: number; reserved: number; pctAvailable: number } {
    const total = this.schedules.length;
    const available = this.schedules.filter(s => s.disponible).length;
    const reserved = total - available;
    const pctAvailable = total ? Math.round((available / total) * 100) : 0;
    return { total, available, reserved, pctAvailable };
  }

  private recomputeScheduleAnalytics(): void {
    this.scheduleTimeBuckets = this.buildTimeBuckets(this.schedules.map(s => s.hora));
  }

  get receptionStats(): { total: number; pending: number; attended: number; cancelledOrMissed: number } {
    const total = this.appointments.length;
    const pending = this.appointments.filter(a => a.estado === 'RESERVADA' || a.estado === 'CONFIRMADA').length;
    const attended = this.appointments.filter(a => a.estado === 'ATENDIDA').length;
    const cancelledOrMissed = this.appointments.filter(a => a.estado === 'CANCELADA' || a.estado === 'NO_ASISTIO').length;
    return { total, pending, attended, cancelledOrMissed };
  }

  private recomputeReceptionAnalytics(): void {
    this.receptionTimeBuckets = this.buildTimeBuckets(this.appointments.map(a => a.hora));
    const defs = [
      { key: 'RESERVADA', label: 'Reservadas', color: '#147A49' },
      { key: 'CONFIRMADA', label: 'Confirmadas', color: '#0A4D2E' },
      { key: 'ATENDIDA', label: 'Atendidas', color: '#D4AF37' },
      { key: 'NO_ASISTIO', label: 'No asistió', color: '#C98A2C' },
      { key: 'CANCELADA', label: 'Canceladas', color: '#B84A4A' }
    ];
    const items = defs.map(d => ({
      ...d,
      count: this.appointments.filter(a => a.estado === d.key).length
    })).filter(d => d.count > 0);
    const total = items.reduce((acc, i) => acc + i.count, 0) || 1;
    const circumference = 2 * Math.PI * 38;
    let offset = 0;
    this.receptionStatusDonut = items.map(i => {
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

  private buildTimeBuckets(hours: string[]): Array<{ label: string; count: number; pct: number; peak: boolean }> {
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
      count: hours.filter(h => {
        const hh = Number((h || '').slice(0, 2));
        return hh >= r.min && hh < r.max;
      }).length
    }));
    const max = counts.reduce((m, c) => Math.max(m, c.count), 1);
    return counts.map(c => ({
      label: c.label,
      count: c.count,
      pct: c.count === 0 ? 10 : Math.max(18, Math.round((c.count / max) * 100)),
      peak: c.count > 0 && c.count === max
    }));
  }
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
  normalizeDniInput(value: string): string { return (value ?? '').replace(/\D/g, '').slice(0, 8); }
  normalizeDniInputEvent(event: Event): string {
    const input = event.target as HTMLInputElement;
    const value = this.normalizeDniInput(input.value);
    if (input.value !== value) {
      const caret = input.selectionStart;
      input.value = value;
      if (caret !== null) input.setSelectionRange(Math.min(caret, value.length), Math.min(caret, value.length));
    }
    return value;
  }
  normalizeCmpInput(value: string): string {
    const digits = (value ?? '').replace(/\D+/g, '').slice(0, 8);
    return `DEV-CMP-${digits}`;
  }

  normalizeCmpInputEvent(event: Event): string {
    const input = event.target as HTMLInputElement;
    const value = this.normalizeCmpInput(input.value);
    if (input.value !== value) {
      input.value = value;
      input.setSelectionRange(value.length, value.length);
    }
    return value;
  }

  normalizeSearchInput(value: string): string {
    const raw = (value ?? '').slice(0, 100);
    return /^\d+$/.test(raw.trim()) ? raw.trim().slice(0, 8) : raw;
  }
  normalizeSearchInputEvent(event: Event): string {
    const input = event.target as HTMLInputElement;
    const value = this.normalizeSearchInput(input.value);
    if (input.value !== value) {
      const caret = input.selectionStart;
      input.value = value;
      if (caret !== null) input.setSelectionRange(Math.min(caret, value.length), Math.min(caret, value.length));
    }
    return value;
  }
  get visibleAppointments(): Appointment[] {
    if (this.activeSection === 'history') {
      return this.appointments.filter(item => ['ATENDIDA', 'NO_ASISTIO', 'CANCELADA'].includes(item.estado));
    }
    return this.appointments.filter(item => ['RESERVADA', 'CONFIRMADA'].includes(item.estado));
  }

  get navigation(): PortalNavigationItem[] {
    return this.isAdmin
      ? [
          { key: 'doctors', label: 'Médicos', icon: '✚', link: '/administrador/doctores' },
          { key: 'schedules', label: 'Horarios', icon: '▣', link: '/administrador/horarios' },
          { key: 'receptionists', label: 'Recepcionistas', icon: '♙', link: '/administrador/recepcionistas' },
          { key: 'profile', label: 'Perfil', icon: '○', link: '/administrador/perfil' }
        ]
      : [
          { key: 'appointments', label: 'Citas', icon: '▣', link: '/recepcionista/citas' },
          { key: 'history', label: 'Historial total', icon: '▤', link: '/recepcionista/historial' },
          { key: 'profile', label: 'Perfil', icon: '○', link: '/recepcionista/perfil' }
        ];
  }

  get sectionTitle(): string {
    const labels: Record<PortalSection, string> = {
      appointments: 'Gestión de citas', edit: 'Modificar cita', history: 'Historial total', profile: 'Perfil',
      doctors: 'Gestión de médicos', schedules: 'Gestión de horarios', receptionists: 'Gestión de recepcionistas',
      doctorForm: 'Registrar doctor', doctorEditForm: 'Editar doctor', scheduleForm: 'Registrar horario',
      scheduleEditForm: 'Modificar horario', receptionistForm: 'Registrar recepcionista', receptionistEditForm: 'Editar recepcionista'
    };
    return labels[this.activeSection];
  }

  get sectionDescription(): string {
    const descriptions: Record<PortalSection, string> = {
      appointments: 'Consulta la agenda y localiza una cita.', edit: 'Selecciona un horario para reprogramar la cita.',
      history: 'Consulta el registro de citas atendidas e inasistencias.', profile: 'Información de la cuenta de acceso.',
      doctors: 'Administra el registro de médicos y sus especialidades.',
      schedules: 'Consulta y organiza los horarios de atención.',
      receptionists: 'Administra las cuentas del personal de recepción.',
      doctorForm: 'Datos del médico y sus especialidades.', doctorEditForm: 'Actualiza los datos del médico seleccionado.',
      scheduleForm: 'Asigna un horario de atención a un médico.', scheduleEditForm: 'Actualiza la fecha y hora del horario seleccionado.',
      receptionistForm: 'Registra los datos de una cuenta de recepción.', receptionistEditForm: 'Actualiza los datos de la cuenta seleccionada.'
    };
    return descriptions[this.activeSection];
  }

  loadAppointments(): void {
    this.loading = true; this.errorMessage = '';
    this.api.receptionAppointments(undefined, this.appointmentDate || undefined, this.searchText || undefined).subscribe({
      next: items => { this.appointments = items; this.recomputeReceptionAnalytics(); this.loading = false; },
      error: error => { this.fail(error, 'No se pudo cargar la agenda.'); }
    });
  }

  loadProfile(): void {
    this.loading = true; this.errorMessage = '';
    this.api.profile().subscribe({
      next: profile => {
        this.profile = profile;
        this.profileForm = { correo: profile.correo ?? '', celular: profile.celular ?? '', direccion: profile.direccion ?? '' };
        this.loading = false;
      }, error: error => this.fail(error, 'No se pudo cargar el perfil.')
    });
  }

  saveProfile(): void {
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
    this.saving = true; this.errorMessage = ''; this.successMessage = '';
    this.api.updateProfile(this.profileForm).subscribe({
      next: profile => { this.profile = profile; this.successMessage = 'Los datos se actualizaron.'; this.saving = false; },
      error: error => this.fail(error, 'No se pudo actualizar el perfil.')
    });
  }

  cancel(appointment: Appointment): void {
    if (!window.confirm('¿Deseas cancelar esta cita?')) return;
    this.errorMessage = ''; this.successMessage = '';
    this.api.cancelReceptionAppointment(appointment.id, '').subscribe({
      next: () => { this.successMessage = 'La cita se canceló.'; this.loadAppointments(); },
      error: error => this.showFailure(error, 'No se pudo cancelar la cita.')
    });
  }

  closeAppointment(appointment: Appointment, state: 'ATENDIDA' | 'NO_ASISTIO'): void {
    const label = state === 'ATENDIDA' ? 'marcar como atendida' : 'marcar como inasistencia';
    if (!window.confirm(`¿Deseas ${label} esta cita?`)) return;
    this.errorMessage = ''; this.successMessage = '';
    this.api.changeAppointmentState(appointment.id, state).subscribe({
      next: () => { this.successMessage = 'El estado de la cita se actualizó.'; this.loadAppointments(); },
      error: error => this.showFailure(error, 'No se pudo actualizar el estado.')
    });
  }

  private loadAppointment(): void {
    if (!this.validRecordId()) return;
    this.loading = true; this.errorMessage = '';
    this.api.receptionAppointment(this.recordId).subscribe({
      next: item => { this.appointment = item; this.rescheduleDate = this.tomorrow(); this.loading = false; this.loadAppointmentSlots(); },
      error: error => this.fail(error, 'No se pudo consultar la cita.')
    });
  }

  loadAppointmentSlots(): void {
    if (!this.appointment || !this.rescheduleDate) return;
    if (!isValidIsoDate(this.rescheduleDate) || this.rescheduleDate < this.minDate) {
      this.appointmentSlots = [];
      this.selectedSlotId = '';
      this.errorMessage = 'Elige una fecha futura para reprogramar la cita.';
      return;
    }
    this.loading = true; this.errorMessage = '';
    const request = this.appointment.servicioId
      ? this.appointmentApi.serviceSlots(this.appointment.servicioId, this.rescheduleDate)
      : this.appointment.doctorId
        ? this.appointmentApi.slots(this.appointment.doctorId, this.rescheduleDate)
        : null;
    if (!request) {
      this.loading = false;
      this.errorMessage = 'La cita no tiene un servicio o profesional asociado para reprogramarla.';
      return;
    }
    request.subscribe({
      next: slots => { this.appointmentSlots = slots; this.selectedSlotId = ''; this.loading = false; },
      error: error => { this.appointmentSlots = []; this.fail(error, 'No se pudieron cargar horarios disponibles.'); }
    });
  }

  rescheduleAppointment(): void {
    if (!this.appointment || !this.selectedSlotId || this.saving) return;
    if (!isValidIsoDate(this.rescheduleDate) || this.rescheduleDate < this.minDate || !Number.isSafeInteger(Number(this.selectedSlotId))
        || Number(this.selectedSlotId) <= 0) {
      this.errorMessage = 'Selecciona una fecha futura y un horario disponible.';
      return;
    }
    this.saving = true; this.errorMessage = ''; this.successMessage = '';
    this.api.rescheduleReceptionAppointment(this.appointment.id, Number(this.selectedSlotId)).subscribe({
      next: () => { this.successMessage = 'La cita se reprogramó.'; this.saving = false; },
      error: error => this.fail(error, 'No se pudo reprogramar la cita.')
    });
  }

  private loadDoctors(): void {
    this.loading = true; this.errorMessage = '';
    this.api.doctors().subscribe({ next: items => { this.doctors = items; this.recomputeDoctorAnalytics(); this.loading = false; },
      error: error => this.fail(error, 'No se pudo cargar el directorio de médicos.') });
  }

  private loadDoctorForm(edit: boolean): void {
    this.loading = true; this.errorMessage = '';
    this.appointmentApi.specialties().subscribe({
      next: items => {
        this.specialties = items;
        if (!edit) { this.loading = false; return; }
        if (!this.validRecordId()) { this.loading = false; return; }
        this.api.doctor(this.recordId).subscribe({
          next: doctor => {
            this.doctorForm = { nombre: doctor.nombre, cmp: this.normalizeCmpInput(doctor.cmp), especialidadIds: [...doctor.especialidadIds] };
            this.loading = false;
          }, error: error => this.fail(error, 'No se pudo cargar el médico.')
        });
      }, error: error => this.fail(error, 'No se pudieron cargar las especialidades.')
    });
  }

  saveDoctor(): void {
    const payload = { nombre: this.doctorForm.nombre.trim(), cmp: this.normalizeCmpInput(this.doctorForm.cmp), especialidadIds: this.doctorForm.especialidadIds };
    if (this.saving) return;
    if (!payload.nombre || payload.nombre.length > 160 || !/^DEV-CMP-\d{1,8}$/.test(payload.cmp)
        || payload.especialidadIds.length === 0 || payload.especialidadIds.some(id => !Number.isSafeInteger(id) || id <= 0)) {
      this.errorMessage = 'Ingresa un nombre de hasta 160 caracteres, un CMP con formato DEV-CMP- seguido de números y al menos una especialidad.';
      return;
    }
    this.saving = true; this.errorMessage = '';
    const request = this.activeSection === 'doctorEditForm' && this.validRecordId()
      ? this.api.updateDoctor(this.recordId, payload) : this.api.createDoctor(payload);
    request.subscribe({ next: () => { this.saving = false; void this.router.navigate(['/administrador/doctores']); },
      error: error => this.fail(error, 'No se pudo guardar el médico.') });
  }

  toggleDoctor(doctor: DoctorRecord): void {
    const estado = doctor.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    this.api.setDoctorState(doctor.id, estado).subscribe({
      next: updated => { doctor.estado = updated.estado; }, error: error => this.showFailure(error, 'No se pudo cambiar el estado del médico.')
    });
  }

  private loadSchedules(): void {
    this.loading = true; this.errorMessage = '';
    this.api.schedules().subscribe({ next: items => { this.schedules = items; this.recomputeScheduleAnalytics(); this.loading = false; },
      error: error => this.fail(error, 'No se pudieron cargar los horarios.') });
  }

  private loadScheduleForm(edit: boolean): void {
    this.loading = true; this.errorMessage = '';
    forkJoin({ doctors: this.api.doctors(), services: this.appointmentApi.complementaryServices() }).subscribe({
      next: ({ doctors, services }) => {
        this.doctors = doctors;
        this.complementaryServices = services;
        if (!edit) { this.scheduleForm.fecha = this.tomorrow(); this.loading = false; return; }
        if (!this.validRecordId()) { this.loading = false; return; }
        this.api.schedule(this.recordId).subscribe({
          next: schedule => {
            this.scheduleForm = {
              tipoRecurso: schedule.servicioId ? 'SERVICIO' : 'DOCTOR',
              doctorId: schedule.doctorId ? String(schedule.doctorId) : '',
              servicioId: schedule.servicioId ? String(schedule.servicioId) : '',
              fecha: schedule.fecha,
              hora: schedule.hora.slice(0, 5)
            };
            this.loading = false;
          }, error: error => this.fail(error, 'No se pudo cargar el horario.')
        });
      }, error: error => this.fail(error, 'No se pudieron cargar los médicos y servicios.')
    });
  }

  saveSchedule(): void {
    const doctorId = this.scheduleForm.tipoRecurso === 'DOCTOR' ? Number(this.scheduleForm.doctorId) : null;
    const servicioId = this.scheduleForm.tipoRecurso === 'SERVICIO' ? Number(this.scheduleForm.servicioId) : null;
    const payload = { doctorId, servicioId, fecha: this.scheduleForm.fecha, hora: this.scheduleForm.hora };
    if (this.saving) return;
    const validResource = doctorId !== null
      ? Number.isSafeInteger(doctorId) && doctorId > 0
      : servicioId !== null && Number.isSafeInteger(servicioId) && servicioId > 0;
    if (!validResource || !isValidIsoDate(payload.fecha)
        || payload.fecha < this.minDate || !/^([01]\d|2[0-3]):[0-5]\d$/.test(payload.hora)
        || (payload.fecha === this.minDate && payload.hora <= this.localTime(new Date()))) {
      this.errorMessage = 'Selecciona un médico o servicio y una fecha y hora futuras válidas.';
      return;
    }
    this.saving = true; this.errorMessage = '';
    const request = this.activeSection === 'scheduleEditForm' && this.validRecordId()
      ? this.api.updateSchedule(this.recordId, payload) : this.api.createSchedule(payload);
    request.subscribe({ next: () => { this.saving = false; void this.router.navigate(['/administrador/horarios']); },
      error: error => this.fail(error, 'No se pudo guardar el horario.') });
  }

  deleteSchedule(schedule: ScheduleRecord): void {
    if (!window.confirm('¿Eliminar este horario disponible?')) return;
    this.api.deleteSchedule(schedule.id).subscribe({
      next: () => { this.schedules = this.schedules.filter(item => item.id !== schedule.id); },
      error: error => this.showFailure(error, 'No se pudo eliminar el horario.')
    });
  }

  private loadReceptionists(): void {
    this.loading = true; this.errorMessage = '';
    this.api.receptionists().subscribe({ next: items => { this.receptionists = items; this.loading = false; },
      error: error => this.fail(error, 'No se pudo cargar el personal de recepción.') });
  }

  private loadReceptionistForm(edit: boolean): void {
    if (!edit) return;
    if (!this.validRecordId()) return;
    this.loading = true; this.errorMessage = '';
    this.api.receptionist(this.recordId).subscribe({
      next: item => {
        this.receptionistForm = { nombre: item.nombre, apellidoPaterno: item.apellidoPaterno,
          apellidoMaterno: item.apellidoMaterno ?? '', dni: item.dni, correo: item.correo,
          celular: item.celular ?? '', direccion: item.direccion ?? '', claveTemporal: '' };
        this.loading = false;
      }, error: error => this.fail(error, 'No se pudo cargar la cuenta de recepción.')
    });
  }

  saveReceptionist(): void {
    const common = {
      nombre: this.receptionistForm.nombre.trim(), apellidoPaterno: this.receptionistForm.apellidoPaterno.trim(),
      apellidoMaterno: this.receptionistForm.apellidoMaterno.trim(), correo: this.receptionistForm.correo.trim(),
      celular: this.receptionistForm.celular.trim(), direccion: this.receptionistForm.direccion.trim()
    };
    if (this.saving) return;
    const isCreate = this.activeSection !== 'receptionistEditForm';
    if (!common.nombre || common.nombre.length > 100 || !common.apellidoPaterno || common.apellidoPaterno.length > 100
        || common.apellidoMaterno.length > 100 || !hasValidEmail(common.correo)
        || !hasValidOptionalPeruMobile(common.celular) || !hasValidOptionalLength(common.direccion, 300)
        || (isCreate && !DNI_PATTERN.test(this.receptionistForm.dni.trim()))
        || (isCreate && (this.receptionistForm.claveTemporal.length < 12 || this.receptionistForm.claveTemporal.length > 18))) {
      this.errorMessage = 'Revisa los datos: nombres hasta 100 caracteres, DNI de 8 dígitos, correo válido, celular de 9 dígitos si se indica, dirección hasta 300 caracteres y contraseña temporal de 12 a 18 caracteres.';
      return;
    }
    this.saving = true; this.errorMessage = '';
    if (this.activeSection === 'receptionistEditForm' && this.validRecordId()) {
      this.api.updateReceptionist(this.recordId, common).subscribe({
        next: () => { this.saving = false; void this.router.navigate(['/administrador/recepcionistas']); },
        error: error => this.fail(error, 'No se pudo actualizar la cuenta.')
      });
      return;
    }
    this.api.createReceptionist({ ...common, dni: this.receptionistForm.dni.trim(), claveTemporal: this.receptionistForm.claveTemporal })
      .subscribe({ next: () => { this.saving = false; void this.router.navigate(['/administrador/recepcionistas']); },
        error: error => this.fail(error, 'No se pudo crear la cuenta.') });
  }

  toggleReceptionist(person: ReceptionistRecord): void {
    const estado = person.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    this.api.setReceptionistState(person.id, estado).subscribe({
      next: updated => { person.estado = updated.estado; }, error: error => this.showFailure(error, 'No se pudo cambiar el estado de la cuenta.')
    });
  }

  specialtyLabel(appointment: Appointment): string {
    return appointment.servicio ?? appointment.especialidades.join(', ');
  }
  statusLabel(status: string): string {
    const labels: Record<string, string> = { RESERVADA: 'Reservada', CONFIRMADA: 'Confirmada', ATENDIDA: 'Atendida', CANCELADA: 'Cancelada', NO_ASISTIO: 'No asistió' };
    return labels[status] ?? status;
  }

  private validRecordId(): boolean {
    if (Number.isSafeInteger(this.recordId) && this.recordId > 0) return true;
    this.errorMessage = 'El identificador indicado no es válido.';
    return false;
  }

  private fail(error: unknown, fallback: string): void {
    this.showFailure(error, fallback);
    this.loading = false;
    this.saving = false;
  }

  private showFailure(error: unknown, fallback: string): void {
    const err = error as { status?: number; error?: { message?: string } };
    if (err?.status === 0) {
      this.errorMessage = 'No se pudo conectar con el servidor. Verifica tu conexión o que SIGESMED esté en línea.';
    } else if (err?.status === 401) {
      this.errorMessage = 'Tu sesión ha expirado. Por favor, vuelve a iniciar sesión.';
    } else if (err?.status === 403) {
      this.errorMessage = 'Acceso denegado: no tienes permisos suficientes para realizar esta acción.';
    } else if (err?.status === 409) {
      this.errorMessage = err.error?.message || 'Existe un conflicto con la solicitud (horario ocupado o con citas activas).';
    } else {
      this.errorMessage = err?.error?.message ?? fallback;
    }
  }

  private localDate(value: Date): string {
    return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
  }

  private localTime(value: Date): string {
    return `${String(value.getHours()).padStart(2, '0')}:${String(value.getMinutes()).padStart(2, '0')}`;
  }

  private tomorrow(): string {
    const date = new Date();
    date.setDate(date.getDate() + 1);
    return this.localDate(date);
  }
}
