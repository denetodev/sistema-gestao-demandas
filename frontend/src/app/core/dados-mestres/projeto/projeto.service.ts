import { HttpClient, httpResource } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { Projeto, ProjetoPayload } from './projeto.model';

@Injectable({ providedIn: 'root' })
export class ProjetoService {
  #http = inject(HttpClient);
  #base = `${environment.apiUrl}/projetos`;

  listar = httpResource<Projeto[]>(() => this.#base);

  criar(payload: ProjetoPayload) {
    return this.#http.post<Projeto>(this.#base, payload);
  }

  atualizar(id: string, payload: ProjetoPayload) {
    return this.#http.put<Projeto>(`${this.#base}/${id}`, payload);
  }

  excluir(id: string) {
    return this.#http.delete<void>(`${this.#base}/${id}`);
  }
}