import { Injectable } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

export interface Cargo {
    id: string;
    nome: string;
    descricao: string | null;
    ativo: boolean;
    createdAt: string;
    updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class CargoService {
    listar = httpResource<Cargo[]>(() => `${environment.apiUrl}/cargos`);
}