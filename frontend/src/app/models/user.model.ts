// src/app/models/user.model.ts

export interface User {
  id: string;
  nombre: string;
  email: string;
  rol: UserRole;
  token?: string;
  totpConfigurado?: boolean;
}

export type UserRole = 'ROLE_ADMIN' | 'ROLE_MULTIFUNCIONAL';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  usuarioId: string;
  nombre: string;
  email: string;
  rol: UserRole;
  totpConfigurado: boolean;
  refreshToken?: string;
}

// ✅ AGREGAR ESTO
export interface UserInfoDTO {
  id: string;
  nombre: string;
  apellido: string;
  email: string;
  nombreCompleto: string;
}
