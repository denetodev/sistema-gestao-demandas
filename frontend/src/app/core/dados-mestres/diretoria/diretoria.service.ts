import { Injectable, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { alfabetica } from '../../util/listas';

export interface Diretoria {
  id: string;
  nome: string;
  sigla?: string | null;
  ativo?: boolean;
}

@Injectable({ providedIn: 'root' })
export class DiretoriaService {
  #http = inject(HttpClient);
  listar = httpResource<Diretoria[]>(() => `${environment.apiUrl}/diretorias`, { parse: alfabetica<Diretoria>((x) => x.nome) });

  criar(corpo: { nome: string; sigla: string | null }) {
    return this.#http.post<Diretoria>(`${environment.apiUrl}/diretorias`, corpo);
  }
}
