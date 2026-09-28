import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { naoVisualizadorGuard, perfilGuard } from './core/auth/perfil.guard';

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
        path: 'meu-perfil',
        loadComponent: () => import('./features/meu-perfil/meu-perfil').then((m) => m.MeuPerfil),
      },
      {
        path: 'demandas',
        loadComponent: () =>
          import('./features/demandas/feature-lista/demanda-lista/demanda-lista').then(
            (m) => m.DemandaLista,
          ),
      },
      { path: '', redirectTo: 'demandas', pathMatch: 'full' },

      {
        path: 'demandantes',
        canActivate: [perfilGuard('ADMIN', 'GESTOR')],
        loadComponent: () =>
          import('./core/dados-mestres/demandante/demandante-lista/demandante-lista').then(
            (m) => m.DemandanteLista,
          ),
      },
      {
        path: 'campanhas',
        loadComponent: () =>
          import('./core/dados-mestres/campanha/campanha-lista/campanha-lista').then(
            (m) => m.CampanhaLista,
          ),
      },
      {
        path: 'projetos',
        loadComponent: () =>
          import('./core/dados-mestres/projeto/projeto-lista/projeto-lista').then(
            (m) => m.ProjetoLista,
          ),
      },
    ],
  },
];
