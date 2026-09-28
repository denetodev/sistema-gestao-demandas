import { Injectable } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

export interface Area {
  id: string;
  nome: string;
  diretoriaId: string | null;
  diretoriaNome: string | null;
}

@Injectable({ providedIn: 'root' })
export class AreaService {
  listar = httpResource<Area[]>(() => `${environment.apiUrl}/areas`);
}
