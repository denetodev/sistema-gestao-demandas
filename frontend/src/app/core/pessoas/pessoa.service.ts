import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Pessoa, PessoaPayload, AprovarPayload, RejeitarPayload, VinculoSugerido } from './pessoa.model';

@Injectable({ providedIn: 'root' })
export class PessoaService {
    #http = inject(HttpClient);
    #base = `${environment.apiUrl}/pessoas`;

    apenasPendentes = signal(false);

    listar = httpResource<Pessoa[]>(() =>
        this.apenasPendentes() ? `${this.#base}?pendente=true` : this.#base,
    );

    /** Todas as pessoas (sem filtro de aba): usada para achar o cadastro existente ao vincular uma conta. */
    todas = httpResource<Pessoa[]>(() => this.#base);

    /** Liga a conta pendente {pendenteId} ao cadastro que já existe; o cadastro pendente é removido. */
    vincularAPessoaExistente(pendenteId: string, pessoaExistenteId: string) {
        return this.#http.post<Pessoa>(`${this.#base}/${pendenteId}/vincular-a`, { pessoaExistenteId });
    }

    /** Resumo do cadastro importado que a conta pendente pediu para assumir pela chave C. */
    vinculoSugerido(pendenteId: string) {
        return this.#http.get<VinculoSugerido>(`${this.#base}/${pendenteId}/vinculo-sugerido`);
    }

    /** Aprova o vínculo sugerido: o login passa para o cadastro importado e a conta pendente sai. */
    confirmarVinculoSugerido(pendenteId: string) {
        return this.#http.post<Pessoa>(`${this.#base}/${pendenteId}/confirmar-vinculo`, {});
    }

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