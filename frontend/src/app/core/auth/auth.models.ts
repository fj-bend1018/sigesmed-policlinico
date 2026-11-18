export type UserRole = 'PACIENTE' | 'RECEPCIONISTA' | 'ADMINISTRADOR' | 'DOCTOR';

export interface AuthSession {
  usuarioId: number;
  dni: string;
  rol: UserRole;
  portal: string;
  nombre?: string;
}

export interface MessageResponse { mensaje: string; }
