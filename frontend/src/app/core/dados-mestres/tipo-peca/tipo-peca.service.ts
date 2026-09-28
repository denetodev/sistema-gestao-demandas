import { Injectable, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { TipoPeca, TipoPecaPayload } from './tipo-peca.model';

@Injectable({ providedIn: 'root' })
export class TipoPecaService {
  #http = inject(HttpClient);
  #base = `${environment.apiUrl}/tipos-peca`;

  listar = httpResource<TipoPeca[]>(() => this.#base);

  criar(payload: TipoPecaPayload) {
    return this.#http.post<TipoPeca>(this.#base, payload);
  }

  atualizar(id: string, payload: TipoPecaPayload) {
    return this.#http.put<TipoPeca>(`${this.#base}/${id}`, payload);
  }

  excluir(id: string) {
    return this.#http.delete<void>(`${this.#base}/${id}`);
  }
}
