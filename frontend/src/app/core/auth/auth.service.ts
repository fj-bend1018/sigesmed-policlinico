import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, finalize, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthSession, MessageResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'sigesmed.session';
  private readonly sessionSubject = new BehaviorSubject<AuthSession | null>(this.readSession());
  readonly session$ = this.sessionSubject.asObservable();
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) { }

  currentSession(): AuthSession | null { return this.sessionSubject.value; }

  login(identificador: string, clave: string): Observable<AuthSession> {
    return this.http.post<AuthSession>(`${this.baseUrl}/auth/login`, { identificador, clave })
      .pipe(tap(session => this.setSession(session)));
  }
  register(data: Record<string, unknown>): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.baseUrl}/auth/registro`, data);
  }
  recover(correo: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.baseUrl}/auth/recuperar`, { correo });
  }
  reset(token: string, nuevaClave: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.baseUrl}/auth/restablecer`, { token, nuevaClave });
  }
  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/logout`, {}).pipe(finalize(() => this.clearSession()));
  }
  clearSession(): void {
    sessionStorage.removeItem(this.storageKey);
    this.sessionSubject.next(null);
  }
  private setSession(session: AuthSession): void {
    sessionStorage.setItem(this.storageKey, JSON.stringify(session));
    this.sessionSubject.next(session);
  }
  private readSession(): AuthSession | null {
    try {
      const raw = sessionStorage.getItem(this.storageKey);
      if (!raw) return null;
      const session = JSON.parse(raw) as AuthSession;
      return session && ['PACIENTE', 'RECEPCIONISTA', 'ADMINISTRADOR'].includes(session.rol) ? session : null;
    } catch { return null; }
  }
}
