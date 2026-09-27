import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Demanda, CriarDemandaPayload, EscopoDemanda } from './demanda.model';

@Injectable({ providedIn: 'root' })
export class DemandaService {
  #http = inject(HttpClient);
  #base = `${environment.apiUrl}/demandas`;

  escopo = signal<EscopoDemanda>('MINHAS');

  listar = httpResource<Demanda[]>(() => ({
    url: this.#base,
    params: { escopo: this.escopo() },
  }));

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
