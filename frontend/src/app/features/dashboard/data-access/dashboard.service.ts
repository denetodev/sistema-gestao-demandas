import { Injectable, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import type { Pessoa } from '../../../core/pessoas/pessoa.model';
import type { Dashboard, DashboardFiltro } from './dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  filtro = signal<DashboardFiltro>({ pessoaId: null, diretoriaId: null, porPessoa: false, mes: null });

  /** A visão (minha, equipe, diretoria) é decidida pelo backend conforme o perfil. */
  dashboard = httpResource<Dashboard>(() => {
    const f = this.filtro();
    return {
      url: `${environment.apiUrl}/dashboard`,
      params: {
        ...(f.pessoaId ? { pessoaId: f.pessoaId } : {}),
        ...(f.diretoriaId ? { diretoriaId: f.diretoriaId } : {}),
        ...(f.porPessoa ? { porPessoa: true } : {}),
        ...(f.mes ? { mes: f.mes } : {}),
      },
    };
  });

  /** Colegas para o seletor da Referência de Equipe. */
  pessoas = httpResource<Pessoa[]>(() => `${environment.apiUrl}/pessoas`);
}
