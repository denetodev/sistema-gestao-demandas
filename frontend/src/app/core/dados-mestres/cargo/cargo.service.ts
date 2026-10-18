import { Injectable, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
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
    #http = inject(HttpClient);
    listar = httpResource<Cargo[]>(() => `${environment.apiUrl}/cargos`);

    criar(corpo: { nome: string; descricao: string | null }) {
        return this.#http.post<Cargo>(`${environment.apiUrl}/cargos`, corpo);
    }

    atualizar(id: string, corpo: { nome: string; descricao: string | null }) {
        return this.#http.put<Cargo>(`${environment.apiUrl}/cargos/${id}`, corpo);
    }

    desativar(id: string) {
        return this.#http.delete<void>(`${environment.apiUrl}/cargos/${id}`);
    }
}