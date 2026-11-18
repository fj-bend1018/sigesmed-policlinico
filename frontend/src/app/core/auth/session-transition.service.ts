import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { AuthSession, UserRole } from './auth.models';

export interface SessionTransitionState {
  kind: 'login' | 'logout';
  eyebrow: string;
  title: string;
  subtitle: string;
  leaving: boolean;
}

@Injectable({ providedIn: 'root' })
export class SessionTransitionService {
  private readonly stateSubject = new BehaviorSubject<SessionTransitionState | null>(null);
  readonly state$ = this.stateSubject.asObservable();
  private timers: ReturnType<typeof setTimeout>[] = [];

  showLogin(session: AuthSession): void {
    const displayName = this.resolveDisplayName(session);
    this.show({
      kind: 'login',
      eyebrow: this.roleLabel(session.rol),
      title: `¡Bienvenido(a), ${displayName}!`,
      subtitle: 'Preparando tu panel de atención…',
      leaving: false
    }, 1_350);
  }

  showLogout(session: AuthSession | null): void {
    const displayName = session ? this.resolveDisplayName(session) : '';
    const title = displayName ? `¡Hasta pronto, ${displayName}!` : 'Cerrando sesión…';
    this.show({
      kind: 'logout',
      eyebrow: 'Cierre de sesión',
      title,
      subtitle: 'Finalizando tu sesión de forma segura…',
      leaving: false
    }, 1_100);
  }

  private show(initial: SessionTransitionState, visibleMs: number): void {
    this.clearTimers();
    this.stateSubject.next(initial);
    this.timers.push(setTimeout(() => {
      const current = this.stateSubject.value;
      if (current) this.stateSubject.next({ ...current, leaving: true });
    }, visibleMs));
    this.timers.push(setTimeout(() => {
      this.stateSubject.next(null);
    }, visibleMs + 360));
  }

  private clearTimers(): void {
    this.timers.forEach(t => clearTimeout(t));
    this.timers = [];
  }

  private resolveDisplayName(session: AuthSession): string {
    if (session.nombre && session.nombre.trim().length > 0) {
      return session.nombre.trim();
    }
    return this.roleFallbackName(session.rol);
  }

  private roleFallbackName(rol: UserRole): string {
    switch (rol) {
      case 'PACIENTE': return 'Paciente';
      case 'RECEPCIONISTA': return 'Recepción';
      case 'ADMINISTRADOR': return 'Administrador';
      case 'DOCTOR': return 'Doctor(a)';
    }
  }

  private roleLabel(rol: UserRole): string {
    switch (rol) {
      case 'PACIENTE': return 'Portal del Paciente';
      case 'RECEPCIONISTA': return 'Portal de Recepción';
      case 'ADMINISTRADOR': return 'Portal de Administración';
      case 'DOCTOR': return 'Portal del Médico';
    }
  }
}
