import { Injectable, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Demandante, DemandantePayload } from './demandante.model';

@Injectable({ providedIn: 'root' })
export class DemandanteService {
  #http = inject(HttpClient);
  #base = `${environment.apiUrl}/demandantes`;

  listar = httpResource<Demandante[]>(() => this.#base);

  criar(payload: DemandantePayload) {
    return this.#http.post<Demandante>(this.#base, payload);
  }

  atualizar(id: string, payload: DemandantePayload) {
    return this.#http.put<Demandante>(`${this.#base}/${id}`, payload);
  }

  excluir(id: string) {
    return this.#http.delete<void>(`${this.#base}/${id}`);
  }
}
