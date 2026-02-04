import type { Role } from './role.model';

export interface User {
  id?: string | number;
  name?: string;
  email?: string;
  role?: Role;
}
