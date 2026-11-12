import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Specialty { id: number; nombre: string; }
export interface ComplementaryService { id: number; nombre: string; }
export interface Doctor { id: number; nombre: string; cmp: string; especialidades: string[]; }
export interface AvailableSlot {
  id: number; doctorId: number | null; doctorNombre: string | null;
  servicioId?: number | null; servicioNombre?: string | null; fecha: string; hora: string;
}
export interface Appointment {
  id: number; horarioId: number; doctorId: number | null; estado: string; paciente: string;
  pacienteDni?: string; correoPaciente?: string; especialidades: string[];
  doctor: string | null; fecha: string; hora: string; motivoCancelacion?: string | null;
  servicioId?: number | null; servicio?: string | null;
}

@Injectable({ providedIn: 'root' })
export class AppointmentApiService {
  private readonly baseUrl = environment.apiBaseUrl;
  constructor(private readonly http: HttpClient) { }
  specialties(): Observable<Specialty[]> { return this.http.get<Specialty[]>(`${this.baseUrl}/especialidades`); }
  complementaryServices(): Observable<ComplementaryService[]> {
    return this.http.get<ComplementaryService[]>(`${this.baseUrl}/servicios-complementarios`);
  }
  doctors(specialtyId: number): Observable<Doctor[]> {
    return this.http.get<Doctor[]>(`${this.baseUrl}/especialidades/${specialtyId}/medicos`);
  }
  slots(doctorId: number, date: string): Observable<AvailableSlot[]> {
    const params = new HttpParams().set('fecha', date);
    return this.http.get<AvailableSlot[]>(`${this.baseUrl}/medicos/${doctorId}/horarios`, { params });
  }
  serviceSlots(serviceId: number, date: string): Observable<AvailableSlot[]> {
    const params = new HttpParams().set('fecha', date);
    return this.http.get<AvailableSlot[]>(`${this.baseUrl}/servicios-complementarios/${serviceId}/horarios`, { params });
  }
  reserve(horarioId: number, especialidadId: number | null, servicioId: number | null): Observable<Appointment> {
    const request = especialidadId ? { horarioId, especialidadId } : { horarioId, servicioId };
    return this.http.post<Appointment>(`${this.baseUrl}/citas`, request);
  }
  appointment(id: number): Observable<Appointment> {
    return this.http.get<Appointment>(`${this.baseUrl}/citas/${id}`);
  }
}
