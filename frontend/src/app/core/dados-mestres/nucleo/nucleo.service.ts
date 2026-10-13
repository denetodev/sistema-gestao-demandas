import { Injectable, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

export interface Nucleo {
  id: string;
  nome: string;
  diretoriaId: string;
  diretoriaNome: string | null;
  ativo: boolean;
}

export interface NucleoPayload {
  nome: string;
  diretoriaId: string;
}

@Injectable({ providedIn: 'root' })
export class NucleoService {
  #http = inject(HttpClient);
  listar = httpResource<Nucleo[]>(() => `${environment.apiUrl}/nucleos`);

  criar(corpo: NucleoPayload) {
    return this.#http.post<Nucleo>(`${environment.apiUrl}/nucleos`, corpo);
  }

  atualizar(id: string, corpo: NucleoPayload) {
    return this.#http.put<Nucleo>(`${environment.apiUrl}/nucleos/${id}`, corpo);
  }
}
