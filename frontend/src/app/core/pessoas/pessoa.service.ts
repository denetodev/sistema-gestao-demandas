import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Pessoa, PessoaPayload, AprovarPayload, RejeitarPayload } from './pessoa.model';

@Injectable({ providedIn: 'root' })
export class PessoaService {
    #http = inject(HttpClient);
    #base = `${environment.apiUrl}/pessoas`;

    apenasPendentes = signal(false);

    listar = httpResource<Pessoa[]>(() =>
        this.apenasPendentes() ? `${this.#base}?pendente=true` : this.#base,
    );

    atualizar(id: string, payload: PessoaPayload) {
        return this.#http.put<Pessoa>(`${this.#base}/${id}`, payload);
    }

    aprovar(id: string, payload: AprovarPayload) {
        return this.#http.patch<Pessoa>(`${this.#base}/${id}/aprovar`, payload);
    }

    rejeitar(id: string, payload: RejeitarPayload) {
        return this.#http.patch<Pessoa>(`${this.#base}/${id}/rejeitar`, payload);
    }

    desativar(id: string) {
        return this.#http.delete<void>(`${this.#base}/${id}`);
    }
}