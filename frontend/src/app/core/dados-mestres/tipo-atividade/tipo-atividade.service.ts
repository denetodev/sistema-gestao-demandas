import { Injectable } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

export interface TipoAtividade {
  id: string;
  nome: string;
  descricao: string | null;
  ativo: boolean;
}

@Injectable({ providedIn: 'root' })
export class TipoAtividadeService {
  listar = httpResource<TipoAtividade[]>(() => `${environment.apiUrl}/tipos-atividade`);
}
