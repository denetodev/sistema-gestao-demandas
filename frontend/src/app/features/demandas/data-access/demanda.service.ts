import { Injectable, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { CriarDemandaPayload, Demanda } from './demanda.model';

@Injectable({ providedIn: 'root' })
export class DemandaService {
  #http = inject(HttpClient);
  #base = `${environment.apiUrl}/demandas`;


  listar = httpResource<Demanda[]>(() => this.#base);

  criar(payload: CriarDemandaPayload) {
    return this.#http.post<Demanda>(this.#base, payload);
  }

  buscarPorId(id: string) {
    return this.#http.get<Demanda>(`${this.#base}/${id}`);
  }

  atualizar(id: string, payload: CriarDemandaPayload) {
    return this.#http.put<Demanda>(`${this.#base}/${id}`, payload);
  }
}