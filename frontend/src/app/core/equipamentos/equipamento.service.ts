import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  CategoriaEquipamento,
  ControleEquipamento,
  Emprestimo,
  EmprestimoPayload,
  Equipamento,
  EquipamentoPayload,
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

  cancelar(id: string) {
    return this.#http.patch<Emprestimo>(`${this.#base}/emprestimos/${id}/cancelar`, {});
  }

  // ---- acesso ao módulo (gestão) ----
  definirAcessoEquipe(areaId: string, acessa: boolean) {
    return this.#http.patch(`${this.#base}/areas/${areaId}/acesso-equipamentos`, { acessa });
  }

  definirPapel(pessoaId: string, papel: string | null) {
    return this.#http.patch(`${this.#base}/pessoas/${pessoaId}/papel-equipamentos`, { papel });
  }
}
