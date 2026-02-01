import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const allowed = (route.data?.['roles'] ?? []) as string[];
  const role = auth.getRole();
  const redirectMap: Record<string, string> = {
    ADMIN: '/dashboard/admin',
    COORDINATOR: '/dashboard/coordinador',
    TEACHER: '/dashboard/docente',
    STUDENT: '/dashboard/estudiante',
  };

  if (!auth.isAuthenticated() || !role) return router.createUrlTree(['/login']);
  if (allowed.length === 0) return true;

  return allowed.includes(role)
    ? true
    : router.createUrlTree([redirectMap[role] ?? '/login']);
};
