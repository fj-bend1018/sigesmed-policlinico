import { Injectable } from '@angular/core';

interface DraftBase {
  date: string;
  slotId: number;
  time: string;
}

export interface ConsultationAppointmentDraft extends DraftBase {
  tipoAtencion?: 'CONSULTA';
  specialtyId: number;
  specialtyName: string;
  doctorId: number;
  doctorName: string;
}

export interface ServiceAppointmentDraft extends DraftBase {
  tipoAtencion: 'SERVICIO_COMPLEMENTARIO';
  serviceId: number;
  serviceName: string;
}

export type AppointmentDraft = ConsultationAppointmentDraft | ServiceAppointmentDraft;

@Injectable({ providedIn: 'root' })
export class AppointmentDraftService {
  private readonly storageKey = 'sigesmed.appointment-draft';

  save(draft: AppointmentDraft): void {
    try {
      sessionStorage.setItem(this.storageKey, JSON.stringify(draft));
    } catch { /* El flujo sigue disponible aunque el navegador bloquee el almacenamiento. */ }
  }

  read(): AppointmentDraft | null {
    try {
      const raw = sessionStorage.getItem(this.storageKey);
      if (!raw) return null;
      const draft = JSON.parse(raw) as Partial<AppointmentDraft> & Record<string, unknown>;
      const commonValid = Number.isSafeInteger(draft.slotId) && Number(draft.slotId) > 0
        && typeof draft.date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(draft.date)
        && typeof draft.time === 'string';
      const serviceValid = draft.tipoAtencion === 'SERVICIO_COMPLEMENTARIO'
        && Number.isSafeInteger(draft.serviceId) && Number(draft.serviceId) > 0
        && typeof draft.serviceName === 'string';
      const consultationValid = (draft.tipoAtencion === undefined || draft.tipoAtencion === 'CONSULTA')
        && Number.isSafeInteger(draft.specialtyId) && Number(draft.specialtyId) > 0
        && Number.isSafeInteger(draft.doctorId) && Number(draft.doctorId) > 0
        && typeof draft.specialtyName === 'string' && typeof draft.doctorName === 'string';
      const valid = commonValid && (serviceValid || consultationValid);
      if (!valid) {
        this.clear();
        return null;
      }
      return draft as AppointmentDraft;
    } catch {
      this.clear();
      return null;
    }
  }

  clear(): void {
    try { sessionStorage.removeItem(this.storageKey); } catch { /* No-op. */ }
  }
}
