import { Injectable, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { alfabetica } from '../../util/listas';

export interface Area {
  id: string;
  nome: string;
  diretoriaId: string | null;
  diretoriaNome: string | null;
  nucleoId: string | null;
  nucleoNome: string | null;
  ativo: boolean;
}

@Injectable({ providedIn: 'root' })
export class AreaService {
  #http = inject(HttpClient);
  listar = httpResource<Area[]>(() => `${environment.apiUrl}/areas`, { parse: alfabetica<Area>((x) => x.nome) });

  criar(corpo: { nome: string; diretoriaId: string; nucleoId: string | null }) {
    return this.#http.post<Area>(`${environment.apiUrl}/areas`, corpo);
  }

  desativar(id: string) {
    return this.#http.delete<void>(`${environment.apiUrl}/areas/${id}`);
  }

  atualizar(id: string, corpo: { nome: string; diretoriaId: string; nucleoId: string | null }) {
    return this.#http.put<Area>(`${environment.apiUrl}/areas/${id}`, corpo);
  }
}
