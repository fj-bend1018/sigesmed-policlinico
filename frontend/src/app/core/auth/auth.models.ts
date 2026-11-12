export type UserRole = 'PACIENTE' | 'RECEPCIONISTA' | 'ADMINISTRADOR';

export interface AuthSession {
  usuarioId: number;
  dni: string;
  rol: UserRole;
  portal: string;
}

export interface MessageResponse { mensaje: string; }
