import { Injectable, inject } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

export interface Diretoria {
  id: string;
  nome: string;
}

@Injectable({ providedIn: 'root' })
export class DiretoriaService {
  listar = httpResource<Diretoria[]>(() => `${environment.apiUrl}/diretorias`);
}
