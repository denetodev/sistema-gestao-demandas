import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import type { Demanda } from '../../features/demandas/data-access/demanda.model';
import type { Pagina } from '../http/pagina.model';
import {
  CategoriaEquipamento,
  ControleEquipamento,
  Emprestimo,
  EmprestimoPayload,
  Equipamento,
  EquipamentoPayload,
  FotoEmprestimo,
  MomentoFoto,
  GrupoEmprestimos,
  Termo,
} from './equipamento.model';

@Injectable({ providedIn: 'root' })
export class EquipamentoService {
  #http = inject(HttpClient);
  #base = `${environment.apiUrl}`;

  // ---- inventário ----
  equipamentos = httpResource<Equipamento[]>(() => `${this.#base}/equipamentos`);
  categorias = httpResource<CategoriaEquipamento[]>(() => `${this.#base}/categorias-equipamento`);
  termo = httpResource<Termo>(() => `${this.#base}/equipamentos/termo`);

  criarEquipamento(payload: EquipamentoPayload) {
    return this.#http.post<Equipamento>(`${this.#base}/equipamentos`, payload);
  }

  atualizarEquipamento(id: string, payload: EquipamentoPayload) {
    return this.#http.put<Equipamento>(`${this.#base}/equipamentos/${id}`, payload);
  }

  desativarEquipamento(id: string) {
    return this.#http.delete<void>(`${this.#base}/equipamentos/${id}`);
  }

  criarCategoria(nome: string, controle: ControleEquipamento) {
    return this.#http.post<CategoriaEquipamento>(`${this.#base}/categorias-equipamento`, { nome, controle });
  }

  publicarTermo(texto: string) {
    return this.#http.post<Termo>(`${this.#base}/equipamentos/termo`, { texto });
  }

  // ---- empréstimos ----
  grupo = signal<GrupoEmprestimos>('ATIVOS');
  emprestimos = httpResource<Emprestimo[]>(() => `${this.#base}/emprestimos?grupo=${this.grupo()}`);

  registrar(payload: EmprestimoPayload) {
    return this.#http.post<Emprestimo>(`${this.#base}/emprestimos`, payload);
  }

  autorizar(id: string) {
    return this.#http.patch<Emprestimo>(`${this.#base}/emprestimos/${id}/autorizar`, {});
  }

  recusar(id: string, motivo: string) {
    return this.#http.patch<Emprestimo>(`${this.#base}/emprestimos/${id}/recusar`, { motivo });
  }

  retirar(id: string) {
    return this.#http.patch<Emprestimo>(`${this.#base}/emprestimos/${id}/retirar`, {});
  }

  devolver(id: string, itemIds: string[] | null, observacoes: string | null) {
    return this.#http.patch<Emprestimo>(`${this.#base}/emprestimos/${id}/devolver`, { itemIds, observacoes });
  }

  /** Corrige dados de um empréstimo que ainda espera autorização (equipamentos e termo não mudam). */
  editar(id: string, payload: Omit<EmprestimoPayload, 'itens' | 'termoAceito' | 'retirantePessoaId' | 'atividadeId'>) {
    return this.#http.put<Emprestimo>(`${this.#base}/emprestimos/${id}`, payload);
  }

  prorrogar(id: string, novaPrevista: string, motivo: string | null) {
    return this.#http.patch<Emprestimo>(`${this.#base}/emprestimos/${id}/prorrogar`, { novaPrevista, motivo });
  }

  cancelar(id: string) {
    return this.#http.patch<Emprestimo>(`${this.#base}/emprestimos/${id}/cancelar`, {});
  }

  // ---- fotos opcionais (saída e devolução) ----
  fotos(emprestimoId: string) {
    return this.#http.get<FotoEmprestimo[]>(`${this.#base}/emprestimos/${emprestimoId}/fotos`);
  }

  enviarFoto(emprestimoId: string, momento: MomentoFoto, arquivo: File) {
    const corpo = new FormData();
    corpo.append('arquivo', arquivo);
    return this.#http.post<FotoEmprestimo>(`${this.#base}/emprestimos/${emprestimoId}/fotos`, corpo, { params: { momento } });
  }

  removerFoto(emprestimoId: string, fotoId: string) {
    return this.#http.delete<void>(`${this.#base}/emprestimos/${emprestimoId}/fotos/${fotoId}`);
  }

  // ---- vínculo opcional com demanda ----
  /** Busca por título ou código dentro da diretoria da pessoa (leitura aberta a todo vinculado). */
  buscarDemandas(busca: string) {
    return this.#http.get<Pagina<Demanda>>(`${this.#base}/demandas`, {
      params: { escopo: 'DIRETORIA', size: 10, ...(busca ? { busca } : {}) },
    });
  }

  // ---- acesso ao módulo (gestão) ----
  definirAcessoEquipe(areaId: string, acessa: boolean) {
    return this.#http.patch(`${this.#base}/areas/${areaId}/acesso-equipamentos`, { acessa });
  }

  definirPapel(pessoaId: string, papel: string | null) {
    return this.#http.patch(`${this.#base}/pessoas/${pessoaId}/papel-equipamentos`, { papel });
  }
}
