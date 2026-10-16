import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { equipamentosGuard, naoVisualizadorGuard, perfilGuard } from './core/auth/perfil.guard';

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
        path: 'acesso-negado',
        loadComponent: () =>
          import('./features/acesso/acesso-negado/acesso-negado').then((m) => m.AcessoNegado),
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/feature-dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'relatorio',
        canActivate: [naoVisualizadorGuard],
        loadComponent: () =>
          import('./features/relatorio/feature-relatorio/relatorio').then((m) => m.RelatorioMensal),
      },
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
      {
        path: 'demandas/:id',
        loadComponent: () =>
          import('./features/demandas/feature-detalhe/demanda-detalhe/demanda-detalhe').then(
            (m) => m.DemandaDetalhe,
          ),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

      {
        path: 'equipamentos',
        canActivate: [equipamentosGuard],
        loadComponent: () =>
          import('./core/equipamentos/emprestimo-lista/emprestimo-lista').then((m) => m.EmprestimoLista),
      },
      {
        path: 'equipamentos/agenda',
        canActivate: [equipamentosGuard],
        loadComponent: () =>
          import('./core/equipamentos/agenda/agenda-equipamentos').then((m) => m.AgendaEquipamentos),
      },
      {
        path: 'equipamentos/inventario',
        canActivate: [equipamentosGuard],
        loadComponent: () =>
          import('./core/equipamentos/inventario/inventario').then((m) => m.Inventario),
      },
      {
        path: 'demandantes',
        canActivate: [perfilGuard('ADMIN', 'GESTOR')],
        loadComponent: () =>
          import('./core/dados-mestres/demandante/demandante-lista/demandante-lista').then(
            (m) => m.DemandanteLista,
          ),
      },
      {
        path: 'estrutura',
        canActivate: [perfilGuard('ADMIN', 'GESTOR')],
        loadComponent: () =>
          import('./core/dados-mestres/estrutura/estrutura').then((m) => m.Estrutura),
      },
      {
        path: 'projetos',
        loadComponent: () =>
          import('./core/dados-mestres/projeto/projeto-lista/projeto-lista').then(
            (m) => m.ProjetoLista,
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
        path: 'campanhas/:id',
        loadComponent: () =>
          import('./core/dados-mestres/campanha/campanha-detalhe/campanha-detalhe').then(
            (m) => m.CampanhaDetalheComponent,
          ),
      },
      {
        path: 'tipos-peca',
        loadComponent: () =>
          import('./core/dados-mestres/tipo-peca/tipo-peca-lista/tipo-peca-lista').then(
            (m) => m.TipoPecaLista,
          ),
      },
      {
        path: 'pessoas',
        canActivate: [perfilGuard('ADMIN', 'GESTOR')],
        loadComponent: () =>
          import('./core/pessoas/pessoa-lista/pessoa-lista').then(m => m.PessoaLista),
      },
    ],
  },
];
