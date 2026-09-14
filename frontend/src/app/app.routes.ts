import { Routes } from '@angular/router';

export const routes: Routes = [
    {
        path: 'demandas',
        loadComponent: () =>
            import('./features/demandas/feature-lista/demanda-lista/demanda-lista').then(m => m.DemandaLista),
    },
    { path: '', redirectTo: 'demandas', pathMatch: 'full' },
];