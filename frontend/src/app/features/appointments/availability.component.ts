import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { AppointmentApiService, AvailableSlot, ComplementaryService, Doctor, Specialty } from '../../core/appointments/appointment-api.service';
import { AppointmentDraft, AppointmentDraftService, ServiceAppointmentDraft } from '../../core/appointments/appointment-draft.service';
import { ErrorMessageService } from '../../core/http/error-message.service';
import { isValidIsoDate } from '../../core/forms/field-rules';

@Component({
  standalone: true, selector: 'app-availability', imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './availability.component.html', styleUrl: './availability.component.scss'
})
export class AvailabilityComponent implements OnInit {
  readonly minDate = this.localDate(new Date());
  readonly bookingTypes: Array<{ id: 'CONSULTA' | 'SERVICIO_COMPLEMENTARIO'; title: string; description: string }> = [
    { id: 'CONSULTA', title: 'Consulta médica', description: 'Elige especialidad, profesional y horario.' },
    { id: 'SERVICIO_COMPLEMENTARIO', title: 'Servicio complementario', description: 'Reserva laboratorio clínico o ecografías.' }
  ];
  specialties: Specialty[] = [];
  complementaryServices: ComplementaryService[] = [];
  doctors: Doctor[] = [];
  slots: AvailableSlot[] = [];
  specialtyId = '';
  serviceId = '';
  doctorId = '';
  bookingType: 'CONSULTA' | 'SERVICIO_COMPLEMENTARIO' = 'CONSULTA';
  date = this.localDate(new Date());
  activeStep = 1;
  selectedSlot: AvailableSlot | null = null;
  selectedSlotUnavailable = false;
  loadingSpecialties = false;
  specialtiesLoadError = false;
  loadingServices = false;
  servicesLoadError = false;
  loadingDoctors = false;
  doctorsLoadError = false;
  loadingSlots = false;
  reserving = false;
  submittedSearch = false;
  reservationError = '';
  private restoredSpecialtyName = '';
  private restoredServiceName = '';
  private restoredDoctorName = '';

  get selectedSpecialtyName(): string {
    return this.specialties.find(item => String(item.id) === this.specialtyId)?.nombre ?? this.restoredSpecialtyName;
  }
  get selectedDoctorName(): string {
    return this.doctors.find(item => String(item.id) === this.doctorId)?.nombre ?? this.restoredDoctorName;
  }
  get selectedServiceName(): string {
    return this.complementaryServices.find(item => String(item.id) === this.serviceId)?.nombre ?? this.restoredServiceName;
  }
  get selectedAttentionName(): string {
    return this.bookingType === 'CONSULTA' ? this.selectedSpecialtyName : this.selectedServiceName;
  }
  get totalSteps(): number { return this.bookingType === 'CONSULTA' ? 4 : 3; }
  get progressSteps(): number[] { return Array.from({ length: this.totalSteps }, (_, index) => index + 1); }
  get scheduleStep(): number { return this.bookingType === 'CONSULTA' ? 4 : 3; }
  stepLabel(step: number): string {
    if (this.bookingType === 'CONSULTA') return ['Especialidad', 'Médico', 'Fecha', 'Horario'][step - 1] ?? '';
    return ['Servicio', 'Fecha', 'Horario'][step - 1] ?? '';
  }
  get guestBooking(): boolean { return !this.auth.currentSession(); }
  get canContinueSelection(): boolean {
    if (this.bookingType === 'SERVICIO_COMPLEMENTARIO') return Boolean(this.serviceId);
    return Boolean(this.specialtyId) && !this.loadingDoctors && this.doctors.length > 0;
  }

  get canContinueDoctor(): boolean { return Boolean(this.doctorId); }

  specialtyDescription(name: string): string {
    const key = name.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    const descriptions: Record<string, string> = {
      'pediatria general': 'Crecimiento y salud infantil',
      'medicina familiar': 'Atención integral de la familia',
      'nutricion': 'Alimentación y desarrollo',
      'psicologia infantil': 'Bienestar emocional',
      'odontologia pediatrica': 'Salud bucal infantil',
      'otorrinolaringologia': 'Oído, nariz y garganta'
    };
    return descriptions[key] ?? 'Atención especializada para niñas, niños y sus familias.';
  }

  specialtyIconKey(name: string): string {
    const key = name.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    if (key.includes('pediatr')) return 'child';
    if (key.includes('familiar')) return 'family';
    if (key.includes('nutric')) return 'nutrition';
    if (key.includes('psicolog')) return 'psychology';
    if (key.includes('odontolog')) return 'dental';
    if (key.includes('otorrino')) return 'ear';
    return 'medical';
  }

  serviceDescription(name: string): string {
    const normalized = name.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    if (normalized.includes('laboratorio')) return 'Análisis clínicos de apoyo al diagnóstico.';
    if (normalized.includes('ecograf')) return 'Estudios de imágenes para evaluación clínica.';
    return 'Servicio complementario del policlínico.';
  }

  constructor(private readonly api: AppointmentApiService, private readonly router: Router,
      private readonly auth: AuthService, private readonly drafts: AppointmentDraftService,
      private readonly errors: ErrorMessageService) { }

  ngOnInit(): void {
    this.errors.clear();
    const session = this.auth.currentSession();
    if (session && session.rol !== 'PACIENTE') {
      void this.router.navigate([`/${session.portal}`]);
      return;
    }

    const draft = this.drafts.read();
    if (draft) {
      this.bookingType = draft.tipoAtencion === 'SERVICIO_COMPLEMENTARIO' ? 'SERVICIO_COMPLEMENTARIO' : 'CONSULTA';
      if (draft.tipoAtencion === 'SERVICIO_COMPLEMENTARIO') {
        this.serviceId = String(draft.serviceId);
        this.restoredServiceName = draft.serviceName;
        this.activeStep = 3;
      } else {
        this.specialtyId = String(draft.specialtyId);
        this.doctorId = String(draft.doctorId);
        this.restoredSpecialtyName = draft.specialtyName;
        this.restoredDoctorName = draft.doctorName;
        this.activeStep = 4;
      }
      this.date = draft.date;
      this.selectedSlot = this.asUnavailableSlot(draft);
      this.selectedSlotUnavailable = true;
      this.submittedSearch = true;
    }

    this.loadSpecialties(draft?.tipoAtencion !== 'SERVICIO_COMPLEMENTARIO' ? draft : null);
    this.loadComplementaryServices(draft?.tipoAtencion === 'SERVICIO_COMPLEMENTARIO' ? draft : null);
  }

  retrySpecialties(): void {
    this.reservationError = '';
    this.loadSpecialties(this.drafts.read());
  }

  retryServices(): void {
    this.reservationError = '';
    const draft = this.drafts.read();
    this.loadComplementaryServices(draft?.tipoAtencion === 'SERVICIO_COMPLEMENTARIO' ? draft : null);
  }

  onBookingTypeChange(value: 'CONSULTA' | 'SERVICIO_COMPLEMENTARIO'): void {
    if (this.bookingType === value) return;
    this.clearDraft();
    this.bookingType = value;
    this.activeStep = 1;
    this.specialtyId = '';
    this.serviceId = '';
    this.doctorId = '';
    this.restoredSpecialtyName = '';
    this.restoredServiceName = '';
    this.restoredDoctorName = '';
    this.doctors = [];
    this.clearSlots();
  }

  retryDoctors(): void {
    if (!this.specialtyId) return;
    this.doctorsLoadError = false;
    this.loadingDoctors = true;
    this.api.doctors(Number(this.specialtyId)).subscribe({
      next: items => { this.doctors = items; this.loadingDoctors = false; },
      error: () => {
        this.loadingDoctors = false;
        this.doctorsLoadError = true;
        this.errors.clear();
      }
    });
  }

  onSpecialtyChange(value: string): void {
    this.clearDraft();
    this.activeStep = 1;
    this.specialtyId = value;
    this.restoredSpecialtyName = '';
    this.restoredDoctorName = '';
    this.doctorId = '';
    this.doctors = [];
    this.doctorsLoadError = false;
    this.clearSlots();
    if (!value) return;
    this.loadingDoctors = true;
    this.api.doctors(Number(value)).subscribe({
      next: items => { this.doctors = items; this.loadingDoctors = false; },
      error: () => {
        this.loadingDoctors = false;
        this.doctorsLoadError = true;
        this.errors.clear();
      }
    });
  }

  onServiceChange(value: string): void {
    this.clearDraft();
    this.activeStep = 1;
    this.serviceId = value;
    this.restoredServiceName = '';
    this.clearSlots();
  }

  onDoctorChange(value: string): void {
    this.clearDraft();
    this.activeStep = 2;
    this.restoredDoctorName = '';
    this.doctorId = value;
    this.clearSlots();
  }

  onDateChange(value: string): void {
    this.clearDraft();
    this.activeStep = this.bookingType === 'CONSULTA' ? 3 : 2;
    this.date = value;
    this.clearSlots();
  }

  search(): void {
    if (this.bookingType === 'CONSULTA') {
      if (!Number.isSafeInteger(Number(this.specialtyId)) || Number(this.specialtyId) <= 0
          || !Number.isSafeInteger(Number(this.doctorId)) || Number(this.doctorId) <= 0) {
        this.reservationError = 'Selecciona una especialidad y un médico válidos.';
        return;
      }
    } else if (!Number.isSafeInteger(Number(this.serviceId)) || Number(this.serviceId) <= 0) {
      this.reservationError = 'Selecciona un servicio complementario.';
      return;
    }
    if (!isValidIsoDate(this.date) || this.date < this.minDate) {
      this.reservationError = 'Selecciona una fecha válida desde hoy en adelante.';
      return;
    }
    this.clearDraft();
    this.submittedSearch = true;
    this.activeStep = this.scheduleStep;
    this.reservationError = '';
    this.loadingSlots = true;
    this.fetchSlots().subscribe({
      next: items => { this.slots = items; this.loadingSlots = false; this.activeStep = this.scheduleStep; },
      error: () => {
        this.slots = [];
        this.loadingSlots = false;
        this.activeStep = this.scheduleStep;
        this.reservationError = 'No pudimos consultar los horarios. Revisa tu conexión e inténtalo de nuevo.';
        this.errors.clear();
      }
    });
  }

  reserve(): void {
    if (!this.selectedSlot || this.selectedSlotUnavailable || this.reserving) return;
    this.reserving = true;
    this.reservationError = '';
    const specialtyId = this.bookingType === 'CONSULTA' ? Number(this.specialtyId) : null;
    const serviceId = this.bookingType === 'SERVICIO_COMPLEMENTARIO' ? Number(this.serviceId) : null;
    this.api.reserve(this.selectedSlot.id, specialtyId, serviceId).subscribe({
      next: appointment => {
        this.drafts.clear();
        void this.router.navigate(['/citas', appointment.id, 'pago']);
      },
      error: (error: unknown) => {
        this.reserving = false;
        const response = error instanceof HttpErrorResponse ? error : null;
        const conflict = response?.status === 409;
        const apiMessage = typeof response?.error?.message === 'string' ? response.error.message : '';
        const attemptedSlot = this.selectedSlot;
        const patientTimeConflict = apiMessage.toLowerCase().includes('ya tienes una cita activa en esa fecha y hora');
        if (patientTimeConflict && attemptedSlot) {
          this.slots = this.slots.filter(slot => slot.id !== attemptedSlot.id);
          this.selectedSlot = null;
          this.selectedSlotUnavailable = false;
          this.drafts.clear();
          this.reservationError = 'Ya tienes otra cita activa en esa fecha y hora. Elige un horario distinto.';
        } else if (apiMessage) {
          this.reservationError = `No se pudo confirmar la cita: ${apiMessage}`;
        } else if (response?.status === 401 || response?.status === 403) {
          this.reservationError = 'Tu sesión no está activa o no tiene permiso para reservar. Inicia sesión nuevamente.';
        } else if (conflict) {
          this.reservationError = 'El servidor rechazó la reserva por un conflicto. Actualiza los horarios y selecciona otro.';
        } else {
          this.reservationError = 'No pudimos confirmar la cita. Comprueba la conexión y vuelve a consultar los horarios.';
        }
        this.errors.clear();
        if (conflict && attemptedSlot && !patientTimeConflict) {
          this.refreshAfterConflict(attemptedSlot, this.reservationError);
        }
      }
    });
  }

  selectSlot(slot: AvailableSlot): void {
    this.selectedSlot = slot;
    this.selectedSlotUnavailable = false;
    this.reservationError = '';
    this.drafts.save(this.toDraft(slot));
  }

  selectStep(step: number): void {
    if (step < this.activeStep) this.activeStep = step;
  }

  continueFromSpecialty(): void {
    if (this.canContinueSelection) this.activeStep = 2;
  }

  continueFromDoctor(): void {
    if (this.canContinueDoctor) this.activeStep = 3;
  }

  continueToAccount(): void {
    if (!this.selectedSlot || this.selectedSlotUnavailable || this.reserving) return;
    if (!this.guestBooking) {
      this.reserve();
      return;
    }
    this.drafts.save(this.toDraft(this.selectedSlot));
    void this.router.navigate(['/login'], { queryParams: { returnUrl: '/paciente/disponibilidad' } });
  }

  private restoreDraft(draft: AppointmentDraft): void {
    if (draft.tipoAtencion === 'SERVICIO_COMPLEMENTARIO') {
      this.restoreServiceDraft(draft);
      return;
    }
    if (!this.specialties.some(item => item.id === draft.specialtyId)) {
      this.reservationError = 'La especialidad elegida ya no está disponible. Selecciona otra para continuar.';
      return;
    }

    this.loadingDoctors = true;
    this.doctorsLoadError = false;
    this.activeStep = 4;
    this.api.doctors(draft.specialtyId).subscribe({
      next: doctors => {
        this.doctors = doctors;
        this.loadingDoctors = false;
        if (!doctors.some(item => item.id === draft.doctorId)) {
          this.reservationError = 'El médico elegido ya no tiene horarios disponibles. Conservamos la especialidad y la fecha para que elijas otro.';
          return;
        }
        this.doctorId = String(draft.doctorId);
        this.restoredDoctorName = doctors.find(item => item.id === draft.doctorId)?.nombre ?? draft.doctorName;
        this.loadingSlots = true;
        this.api.slots(draft.doctorId, draft.date).subscribe({
          next: slots => this.applyRestoredSlots(draft, slots),
          error: () => {
            this.slots = [];
            this.loadingSlots = false;
            this.errors.clear();
            this.reservationError = 'No pudimos actualizar los horarios. Vuelve a consultarlos antes de confirmar.';
          }
        });
      },
      error: () => {
        this.loadingDoctors = false;
        this.errors.clear();
        this.reservationError = 'No pudimos actualizar los médicos. Revisa tu conexión e inténtalo de nuevo.';
      }
    });
  }

  private applyRestoredSlots(draft: AppointmentDraft, slots: AvailableSlot[]): void {
    this.slots = slots;
    this.loadingSlots = false;
    const currentSlot = slots.find(slot => slot.id === draft.slotId && slot.fecha === draft.date
      && (draft.tipoAtencion === 'SERVICIO_COMPLEMENTARIO'
        ? slot.servicioId === draft.serviceId
        : slot.doctorId === draft.doctorId));
    if (currentSlot) {
      this.selectedSlot = currentSlot;
      this.selectedSlotUnavailable = false;
      this.reservationError = '';
      return;
    }
    this.selectedSlot = this.asUnavailableSlot(draft);
    this.selectedSlotUnavailable = true;
    this.reservationError = 'El horario guardado ya no está disponible para tu cuenta. Puede coincidir con otra cita activa; selecciona otro horario.';
  }

  private refreshAfterConflict(previousSlot: AvailableSlot, reason: string): void {
    this.loadingSlots = true;
    this.fetchSlots().subscribe({
      next: slots => {
        this.slots = slots;
        this.loadingSlots = false;
        const currentSlot = slots.find(slot => slot.id === previousSlot.id);
        if (currentSlot) {
          this.selectedSlot = currentSlot;
          this.selectedSlotUnavailable = false;
          this.reservationError = `${reason} El horario aún aparece disponible; revisa la causa antes de volver a intentar.`;
        } else {
          this.selectedSlot = null;
          this.selectedSlotUnavailable = false;
          this.reservationError = `${reason} El horario ya no está disponible; selecciona otro.`;
        }
      },
      error: () => {
        this.loadingSlots = false;
        this.selectedSlot = previousSlot;
        this.selectedSlotUnavailable = true;
        this.errors.clear();
      }
    });
  }

  private toDraft(slot: AvailableSlot): AppointmentDraft {
    if (this.bookingType === 'SERVICIO_COMPLEMENTARIO') {
      return {
        tipoAtencion: 'SERVICIO_COMPLEMENTARIO', serviceId: Number(this.serviceId),
        serviceName: this.selectedServiceName, date: this.date, slotId: slot.id, time: slot.hora
      };
    }
    return {
      tipoAtencion: 'CONSULTA',
      specialtyId: Number(this.specialtyId),
      specialtyName: this.selectedSpecialtyName,
      doctorId: Number(this.doctorId),
      doctorName: this.selectedDoctorName,
      date: this.date,
      slotId: slot.id,
      time: slot.hora
    };
  }

  private asUnavailableSlot(draft: AppointmentDraft): AvailableSlot {
    if (draft.tipoAtencion === 'SERVICIO_COMPLEMENTARIO') {
      return { id: draft.slotId, doctorId: null, doctorNombre: null, servicioId: draft.serviceId,
        servicioNombre: draft.serviceName, fecha: draft.date, hora: draft.time };
    }
    return { id: draft.slotId, doctorId: draft.doctorId, doctorNombre: draft.doctorName,
      fecha: draft.date, hora: draft.time };
  }

  private clearDraft(): void {
    this.drafts.clear();
    this.selectedSlot = null;
    this.selectedSlotUnavailable = false;
    this.reservationError = '';
  }

  private clearSlots(): void {
    this.slots = [];
    this.selectedSlot = null;
    this.submittedSearch = false;
  }

  private loadSpecialties(draft: AppointmentDraft | null): void {
    this.loadingSpecialties = true;
    this.specialtiesLoadError = false;
    this.api.specialties().subscribe({
      next: items => {
        this.specialties = items;
        this.loadingSpecialties = false;
        if (draft && draft.tipoAtencion !== 'SERVICIO_COMPLEMENTARIO') this.restoreDraft(draft);
      },
      error: () => {
        this.loadingSpecialties = false;
        this.specialtiesLoadError = true;
        this.errors.clear();
        if (draft && draft.tipoAtencion !== 'SERVICIO_COMPLEMENTARIO') {
          this.reservationError = 'No pudimos recuperar la especialidad guardada. Inténtalo de nuevo.';
        }
      }
    });
  }

  private loadComplementaryServices(draft: ServiceAppointmentDraft | null): void {
    this.loadingServices = true;
    this.servicesLoadError = false;
    this.api.complementaryServices().subscribe({
      next: items => {
        this.complementaryServices = items;
        this.loadingServices = false;
        if (draft) this.restoreDraft(draft);
      },
      error: () => {
        this.loadingServices = false;
        this.servicesLoadError = true;
        this.errors.clear();
        if (draft) this.reservationError = 'No pudimos recuperar el servicio guardado. Inténtalo de nuevo.';
      }
    });
  }

  private restoreServiceDraft(draft: ServiceAppointmentDraft): void {
    const service = this.complementaryServices.find(item => item.id === draft.serviceId);
    if (!service) {
      this.reservationError = 'El servicio elegido ya no está disponible. Selecciona otra opción.';
      return;
    }
    this.serviceId = String(service.id);
    this.restoredServiceName = service.nombre;
    this.date = draft.date;
    this.activeStep = this.scheduleStep;
    this.loadingSlots = true;
    this.api.serviceSlots(service.id, draft.date).subscribe({
      next: slots => this.applyRestoredSlots(draft, slots),
      error: () => {
        this.slots = [];
        this.loadingSlots = false;
        this.errors.clear();
        this.reservationError = 'No pudimos actualizar los horarios del servicio. Vuelve a consultarlos antes de confirmar.';
      }
    });
  }

  private fetchSlots(): Observable<AvailableSlot[]> {
    return this.bookingType === 'CONSULTA'
      ? this.api.slots(Number(this.doctorId), this.date)
      : this.api.serviceSlots(Number(this.serviceId), this.date);
  }

  private localDate(value: Date): string {
    return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
  }
}
