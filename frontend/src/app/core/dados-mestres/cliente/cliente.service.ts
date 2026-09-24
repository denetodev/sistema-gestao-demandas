import { Injectable, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Cliente, ClientePayload } from './cliente.model';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  #http = inject(HttpClient);
  #base = `${environment.apiUrl}/clientes`;

  listar = httpResource<Cliente[]>(() => this.#base);
  
  criar(payload: ClientePayload) {
    return this.#http.post<Cliente>(this.#base, payload);
  }

  atualizar(id: string, payload: ClientePayload) {
    return this.#http.put<Cliente>(`${this.#base}/${id}`, payload);
  }

  excluir(id: string) {
    return this.#http.delete<void>(`${this.#base}/${id}`);
  }
}