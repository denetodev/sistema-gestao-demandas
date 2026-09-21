import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'cadastro',
    loadComponent: () => import('./features/auth/cadastro/cadastro').then((m) => m.Cadastro),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./core/layout/app-shell/app-shell').then((m) => m.AppShell),
    children: [
      {
        path: 'demandas',
        loadComponent: () =>
          import('./features/demandas/feature-lista/demanda-lista/demanda-lista').then(
            (m) => m.DemandaLista,
          ),
      },
      { path: '', redirectTo: 'demandas', pathMatch: 'full' },
      {
        path: 'demandas/nova',
        loadComponent: () =>
          import('./features/demandas/feature-form/demanda-form/demanda-form').then(
            (m) => m.DemandaForm,
          ),
      },
    ],
  },
];
