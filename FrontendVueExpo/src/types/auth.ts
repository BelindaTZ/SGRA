export type Role = 'STUDENT' | 'TEACHER' | 'COORDINATOR' | 'ADMIN';

export interface AuthResponse {
  token: string;
  role: Role;
  userId: number;
  username: string;
}
