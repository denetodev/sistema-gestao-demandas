import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Demanda, CriarDemandaPayload, EscopoDemanda, DemandaFiltro } from './demanda.model';
import { Pagina } from '../../../core/http/pagina.model';

@Injectable({ providedIn: 'root' })
export class DemandaService {
  #http = inject(HttpClient);
  #base = `${environment.apiUrl}/demandas`;

  filtro = signal<DemandaFiltro>({ escopo: 'MINHAS', page: 0, size: 20 });

  listar = httpResource<Pagina<Demanda>>(() => {
    const f = this.filtro();
    return {
      url: this.#base,
      params: {
        escopo: f.escopo,
        page: f.page,
        size: f.size,
        ...(f.sort ? { sort: f.sort } : {}),
      },
    };
  });

  buscarPorId(id: string) {
    return this.#http.get<Demanda>(`${this.#base}/${id}`);
  }

  criar(payload: CriarDemandaPayload) {
    return this.#http.post<Demanda>(this.#base, payload);
  }

  atualizar(id: string, payload: CriarDemandaPayload) {
    return this.#http.put<Demanda>(`${this.#base}/${id}`, payload);
  }

  excluir(id: string) {
    return this.#http.delete<void>(`${this.#base}/${id}`);
  }
}
