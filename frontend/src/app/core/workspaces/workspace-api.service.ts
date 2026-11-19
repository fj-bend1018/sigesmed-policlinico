import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Appointment } from '../appointments/appointment-api.service';

export interface AccountProfile {
  rol: 'PACIENTE' | 'RECEPCIONISTA' | 'ADMINISTRADOR' | 'DOCTOR';
  dni: string;
  nombre: string | null;
  apellidoPaterno: string | null;
  apellidoMaterno: string | null;
  correo: string | null;
  celular: string | null;
  direccion: string | null;
}

export interface DoctorRecord {
  id: number;
  nombre: string;
  cmp: string;
  estado: 'ACTIVO' | 'INACTIVO';
  especialidadIds: number[];
  especialidades: string[];
}

export interface ScheduleRecord {
  id: number;
  doctorId: number | null;
  doctor: string | null;
  servicioId: number | null;
  servicio: string | null;
  fecha: string;
  hora: string;
  disponible: boolean;
  estado: 'DISPONIBLE' | 'OCUPADO';
}

export interface RecurringScheduleRequest {
  doctorId: number;
  diasSemana: string[];
  horaInicio: string;
  horaFin: string;
  fechaDesde: string;
  fechaHasta: string;
  diasDescanso: string[];
}

export interface RecurringScheduleResponse {
  doctorId: number;
  doctor: string;
  creados: number;
  horarios: ScheduleRecord[];
}

export interface ReceptionistRecord {
  id: number;
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno: string | null;
  dni: string;
  correo: string;
  celular: string | null;
  direccion: string | null;
  estado: 'ACTIVO' | 'INACTIVO';
}

export interface PaymentReceipt {
  pagoId: number;
  citaId: number;
  codigoComprobante: string;
  metodo: string;
  monto: number;
  estadoPago: string;
  estadoCita: string;
  paciente: string;
  pacienteDni: string;
  atencion: string;
  doctor: string | null;
  fecha: string;
  hora: string;
  registradoPorDni: string;
}

export type AppointmentView = 'PROXIMAS' | 'HISTORIAL' | 'CANCELADAS';

@Injectable({ providedIn: 'root' })
export class WorkspaceApiService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) { }

  profile(): Observable<AccountProfile> {
    return this.http.get<AccountProfile>(`${this.baseUrl}/cuenta/me`);
  }

  updateProfile(data: Pick<AccountProfile, 'correo' | 'celular' | 'direccion'>): Observable<AccountProfile> {
    return this.http.put<AccountProfile>(`${this.baseUrl}/cuenta/me`, data);
  }

  myAppointments(view: AppointmentView): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.baseUrl}/citas/mias`, { params: { vista: view } });
  }

  cancelMyAppointment(id: number, motivo: string): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.baseUrl}/citas/${id}/cancelacion`, { motivo });
  }

  rescheduleMyAppointment(id: number, horarioId: number): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.baseUrl}/citas/${id}/horario`, { horarioId });
  }

  receptionAppointments(estado?: string, fecha?: string, buscar?: string): Observable<Appointment[]> {
    let params = new HttpParams();
    if (estado) params = params.set('estado', estado);
    if (fecha) params = params.set('fecha', fecha);
    if (buscar) params = params.set('buscar', buscar);
    return this.http.get<Appointment[]>(`${this.baseUrl}/recepcionista/citas`, { params });
  }

  receptionAppointment(id: number): Observable<Appointment> {
    return this.http.get<Appointment>(`${this.baseUrl}/recepcionista/citas/${id}`);
  }

  cancelReceptionAppointment(id: number, motivo: string): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.baseUrl}/recepcionista/citas/${id}/cancelacion`, { motivo });
  }

  rescheduleReceptionAppointment(id: number, horarioId: number): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.baseUrl}/recepcionista/citas/${id}/horario`, { horarioId });
  }

  changeAppointmentState(id: number, estado: 'ATENDIDA' | 'NO_ASISTIO' | 'CANCELADA'): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.baseUrl}/recepcionista/citas/${id}/estado`, { estado });
  }

  registerReceptionPayment(id: number, data: { metodo: string; monto: number }): Observable<PaymentReceipt> {
    return this.http.post<PaymentReceipt>(`${this.baseUrl}/recepcionista/citas/${id}/pago`, data);
  }

  doctors(): Observable<DoctorRecord[]> {
    return this.http.get<DoctorRecord[]>(`${this.baseUrl}/administrador/doctores`);
  }

  doctor(id: number): Observable<DoctorRecord> {
    return this.http.get<DoctorRecord>(`${this.baseUrl}/administrador/doctores/${id}`);
  }

  createDoctor(data: Pick<DoctorRecord, 'nombre' | 'cmp' | 'especialidadIds'>): Observable<DoctorRecord> {
    return this.http.post<DoctorRecord>(`${this.baseUrl}/administrador/doctores`, data);
  }

  updateDoctor(id: number, data: Pick<DoctorRecord, 'nombre' | 'cmp' | 'especialidadIds'>): Observable<DoctorRecord> {
    return this.http.put<DoctorRecord>(`${this.baseUrl}/administrador/doctores/${id}`, data);
  }

  setDoctorState(id: number, estado: 'ACTIVO' | 'INACTIVO'): Observable<DoctorRecord> {
    return this.http.patch<DoctorRecord>(`${this.baseUrl}/administrador/doctores/${id}/estado`, { estado });
  }

  schedules(doctorId?: number, desde?: string, hasta?: string, servicioId?: number): Observable<ScheduleRecord[]> {
    let params = new HttpParams();
    if (doctorId) params = params.set('doctorId', doctorId);
    if (servicioId) params = params.set('servicioId', servicioId);
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    return this.http.get<ScheduleRecord[]>(`${this.baseUrl}/administrador/horarios`, { params });
  }

  schedule(id: number): Observable<ScheduleRecord> {
    return this.http.get<ScheduleRecord>(`${this.baseUrl}/administrador/horarios/${id}`);
  }

  createSchedule(data: Pick<ScheduleRecord, 'doctorId' | 'servicioId' | 'fecha' | 'hora'>): Observable<ScheduleRecord> {
    return this.http.post<ScheduleRecord>(`${this.baseUrl}/administrador/horarios`, data);
  }

  createRecurringSchedules(data: RecurringScheduleRequest): Observable<RecurringScheduleResponse> {
    return this.http.post<RecurringScheduleResponse>(`${this.baseUrl}/administrador/horarios/recurrentes`, data);
  }

  updateSchedule(id: number, data: Pick<ScheduleRecord, 'doctorId' | 'servicioId' | 'fecha' | 'hora'>): Observable<ScheduleRecord> {
    return this.http.put<ScheduleRecord>(`${this.baseUrl}/administrador/horarios/${id}`, data);
  }

  deleteSchedule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/administrador/horarios/${id}`);
  }

  receptionists(): Observable<ReceptionistRecord[]> {
    return this.http.get<ReceptionistRecord[]>(`${this.baseUrl}/administrador/recepcionistas`);
  }

  receptionist(id: number): Observable<ReceptionistRecord> {
    return this.http.get<ReceptionistRecord>(`${this.baseUrl}/administrador/recepcionistas/${id}`);
  }

  createReceptionist(data: {
    nombre: string; apellidoPaterno: string; apellidoMaterno?: string; dni: string; correo: string;
    celular?: string; direccion?: string; claveTemporal: string;
  }): Observable<ReceptionistRecord> {
    return this.http.post<ReceptionistRecord>(`${this.baseUrl}/administrador/recepcionistas`, data);
  }

  updateReceptionist(id: number, data: {
    nombre: string; apellidoPaterno: string; apellidoMaterno?: string; correo: string;
    celular?: string; direccion?: string;
  }): Observable<ReceptionistRecord> {
    return this.http.put<ReceptionistRecord>(`${this.baseUrl}/administrador/recepcionistas/${id}`, data);
  }

  setReceptionistState(id: number, estado: 'ACTIVO' | 'INACTIVO'): Observable<ReceptionistRecord> {
    return this.http.patch<ReceptionistRecord>(`${this.baseUrl}/administrador/recepcionistas/${id}/estado`, { estado });
  }

  doctorSummary(): Observable<{ id: number; nombre: string; cmp: string; correo: string | null; imagen: string | null; especialidades: string[] }> {
    return this.http.get<{ id: number; nombre: string; cmp: string; correo: string | null; imagen: string | null; especialidades: string[] }>(`${this.baseUrl}/doctor/resumen`);
  }

  doctorAppointments(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.baseUrl}/doctor/citas`);
  }

  doctorSchedules(): Observable<ScheduleRecord[]> {
    return this.http.get<ScheduleRecord[]>(`${this.baseUrl}/doctor/horarios`);
  }

  changeDoctorAppointmentStatus(id: number, estado: 'ATENDIDA' | 'NO_ASISTIO'): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.baseUrl}/doctor/citas/${id}/estado`, { estado });
  }
}
