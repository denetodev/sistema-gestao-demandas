import { Injectable, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Campanha, CampanhaPayload } from './campanha.model';
import type { CampanhaResumo } from './campanha-resumo.model';

@Injectable({ providedIn: 'root' })
export class CampanhaService {
  #http = inject(HttpClient);
  #base = `${environment.apiUrl}/campanhas`;

  listar = httpResource<Campanha[]>(() => this.#base);

  /** Quantas demandas, pessoas e quanto valor cada campanha reuniu (só as que têm demanda). */
  resumos = httpResource<CampanhaResumo[]>(() => `${this.#base}/resumo`);

  criar(payload: CampanhaPayload) {
    return this.#http.post<Campanha>(this.#base, payload);
  }

  atualizar(id: string, payload: CampanhaPayload) {
    return this.#http.put<Campanha>(`${this.#base}/${id}`, payload);
  }

  excluir(id: string) {
    return this.#http.delete<void>(`${this.#base}/${id}`);
  }
}