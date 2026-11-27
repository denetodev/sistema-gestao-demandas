import { Injectable } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { alfabetica } from '../../util/listas';

export interface TipoAtividade {
  id: string;
  nome: string;
  descricao: string | null;
  ativo: boolean;
}

@Injectable({ providedIn: 'root' })
export class TipoAtividadeService {
  listar = httpResource<TipoAtividade[]>(() => `${environment.apiUrl}/tipos-atividade`, { parse: alfabetica<TipoAtividade>((x) => x.nome) });
}
