import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import type { Perfil } from './auth.model';

function acessoNegado(router: Router, de: string) {
  return router.createUrlTree(['/acesso-negado'], { queryParams: { de } });
}

export function perfilGuard(...perfisPermitidos: Perfil[]): CanActivateFn {
  return async (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    await auth.ready;
    await auth.aguardarPessoa();

    const perfil = auth.pessoa()?.perfil;
    if (perfil && perfisPermitidos.includes(perfil)) return true;

    return acessoNegado(router, state.url);
  };
}

export const naoVisualizadorGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await auth.ready;
  await auth.aguardarPessoa();
  return auth.podeEditar() ? true : acessoNegado(router, state.url);
};