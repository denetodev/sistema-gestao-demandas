import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import type { Perfil } from './auth.model';

export function perfilGuard(...perfisPermitidos: Perfil[]): CanActivateFn {
  return async () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    await auth.ready;

    const perfil = auth.pessoa()?.perfil;
    if (perfil && perfisPermitidos.includes(perfil)) return true;

    return router.parseUrl('/demandas');
  };
}

export const naoVisualizadorGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await auth.ready;
  return auth.podeEditar() ? true : router.parseUrl('/demandas');
};