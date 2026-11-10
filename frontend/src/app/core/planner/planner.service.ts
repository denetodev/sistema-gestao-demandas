import { Injectable, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import type { Pessoa } from '../pessoas/pessoa.model';

/** Uma tarefa do Planner que marcou a etiqueta da pessoa e espera a decisão dela. */
export interface SugestaoPlanner {
  id: string;
  codigo: string | null;
  titulo: string;
  bucket: string | null;
  etiqueta: string;
  criadaNoPlanner: string | null;
  /** A demanda já existe no sistema com esse código: aceitar só coloca a pessoa no time. */
  demandaExistenteId: string | null;
  demandaExistenteTitulo: string | null;
}

export interface ResultadoSincronizacao {
  tarefasLidas: number;
  tarefasNaJanela: number;
  sugestoesNovas: number;
  /** Etiquetas de gente ("CRIATIVO - Fulano") que ninguém da equipe usa (podem ser de outras equipes do CRM). */
  etiquetasSemCadastro: string[];
  /** Etiquetas ou nomes que batem com mais de uma pessoa: ficaram de fora até alguém diferenciar. */
  nomesAmbiguos: string[];
}

@Injectable({ providedIn: 'root' })
export class PlannerService {
  #http = inject(HttpClient);
  #base = `${environment.apiUrl}/planner`;

  sugestoes = httpResource<SugestaoPlanner[]>(() => `${this.#base}/sugestoes`);

  aceitar(id: string) {
    return this.#http.post<{ demandaId: string }>(`${this.#base}/sugestoes/${id}/aceitar`, {});
  }

  recusar(id: string) {
    return this.#http.post<void>(`${this.#base}/sugestoes/${id}/recusar`, {});
  }

  /** Admin, Gestor e Referência de equipe: envia o .xlsx do "Exportar plano para o Excel" do Planner. */
  importar(arquivo: File) {
    const corpo = new FormData();
    corpo.append('arquivo', arquivo);
    return this.#http.post<ResultadoSincronizacao>(`${this.#base}/importar`, corpo);
  }

  /** Gestor e Admin: define (ou limpa, se vazio) a etiqueta que a pessoa usa no plano. */
  definirEtiqueta(pessoaId: string, etiqueta: string) {
    return this.#http.put<Pessoa>(`${this.#base}/pessoas/${pessoaId}/etiqueta`, { etiqueta });
  }
}
