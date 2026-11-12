import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { UserRole } from './auth.models';

export const roleGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const session = auth.currentSession();
  if (!session) return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  const roles = route.data['roles'] as UserRole[] | undefined;
  if (roles?.includes(session.rol)) return true;
  return router.parseUrl(`/${session.portal}`);
};
