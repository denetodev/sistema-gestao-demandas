import { Injectable, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { Signal } from '@angular/core';
import { environment } from '../../../../environments/environment';
import {
  Atividade,
  AtividadePayload,
  Evidencia,
  EvidenciaPayload,
  Participante,
  ParticipantePayload,
  Peca,
  PecaPayload,
  PapelParticipante,
  HistoricoStatus,
  ArquivoUrl,
} from './demanda-itens.model';
import type { StatusDemanda } from './demanda.model';
import type { Pessoa } from '../../../core/pessoas/pessoa.model';

@Injectable({ providedIn: 'root' })
export class DemandaItensService {
  #http = inject(HttpClient);
  #api = environment.apiUrl;

  participantes(demandaId: Signal<string>) {
    return httpResource<Participante[]>(() => `${this.#api}/demandas/${demandaId()}/participantes`);
  }
  pecas(demandaId: Signal<string>) {
    return httpResource<Peca[]>(() => ({ url: `${this.#api}/pecas`, params: { demandaId: demandaId() } }));
  }
  atividades(demandaId: Signal<string>) {
    return httpResource<{ content: Atividade[] }>(() => ({
      url: `${this.#api}/atividades`,
      params: { demandaId: demandaId() },
    }));
  }
  historico(demandaId: Signal<string>) {
    return httpResource<HistoricoStatus[]>(() => `${this.#api}/demandas/${demandaId()}/historico`);
  }
  pessoas() {
    return httpResource<Pessoa[]>(() => `${this.#api}/pessoas`);
  }
  evidencias() {
    return httpResource<Evidencia[]>(() => `${this.#api}/evidencias`);
  }

  atualizarStatus(demandaId: string, status: StatusDemanda) {
    return this.#http.patch(`${this.#api}/demandas/${demandaId}/status`, { status });
  }

  adicionarParticipante(demandaId: string, p: ParticipantePayload) {
    return this.#http.post<Participante>(`${this.#api}/demandas/${demandaId}/participantes`, p);
  }
  atualizarPapel(demandaId: string, id: string, papel: PapelParticipante) {
    return this.#http.patch<Participante>(`${this.#api}/demandas/${demandaId}/participantes/${id}`, { papel });
  }
  removerParticipante(demandaId: string, id: string) {
    return this.#http.delete<void>(`${this.#api}/demandas/${demandaId}/participantes/${id}`);
  }

  criarPeca(p: PecaPayload) {
    return this.#http.post<Peca>(`${this.#api}/pecas`, p);
  }
  atualizarPeca(id: string, p: PecaPayload) {
    return this.#http.put<Peca>(`${this.#api}/pecas/${id}`, p);
  }
  removerPeca(id: string) {
    return this.#http.delete<void>(`${this.#api}/pecas/${id}`);
  }

  criarAtividade(p: AtividadePayload) {
    return this.#http.post<Atividade>(`${this.#api}/atividades`, p);
  }
  atualizarAtividade(id: string, p: AtividadePayload) {
    return this.#http.put<Atividade>(`${this.#api}/atividades/${id}`, p);
  }
  removerAtividade(id: string) {
    return this.#http.delete<void>(`${this.#api}/atividades/${id}`);
  }

  criarEvidencia(p: EvidenciaPayload) {
    return this.#http.post<Evidencia>(`${this.#api}/evidencias`, p);
  }
  /** Upload de imagem (JPEG/PNG até 5 MB). Link externo usa criarEvidencia com tipo LINK. */
  enviarImagem(arquivo: File, vinculo: { atividadeId: string | null; pecaId: string | null }, descricao: string | null) {
    const dados = new FormData();
    dados.append('arquivo', arquivo);
    if (vinculo.atividadeId) dados.append('atividadeId', vinculo.atividadeId);
    if (vinculo.pecaId) dados.append('pecaId', vinculo.pecaId);
    if (descricao) dados.append('descricao', descricao);
    return this.#http.post<Evidencia>(`${this.#api}/evidencias/arquivo`, dados);
  }
  urlDoArquivo(id: string) {
    return this.#http.get<ArquivoUrl>(`${this.#api}/evidencias/${id}/arquivo`);
  }
  removerEvidencia(id: string) {
    return this.#http.delete<void>(`${this.#api}/evidencias/${id}`);
  }
}
