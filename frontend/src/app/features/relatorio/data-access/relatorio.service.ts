import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import type { Pessoa } from '../../../core/pessoas/pessoa.model';
import type {
  Relatorio,
  RelatorioFiltro,
  RelatorioResumo,
  SalvarRelatorioPayload,
} from './relatorio.model';

export function mesAtual(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

@Injectable({ providedIn: 'root' })
export class RelatorioService {
  #http = inject(HttpClient);
  #base = `${environment.apiUrl}/relatorios`;

  filtro = signal<RelatorioFiltro>({ mes: mesAtual(), pessoaId: null });
  /** Só Gestor/Admin carregam o resumo da equipe. */
  carregarResumo = signal(false);

  relatorio = httpResource<Relatorio>(() => {
    const f = this.filtro();
    return {
      url: `${this.#base}/${f.mes}`,
      params: { ...(f.pessoaId ? { pessoaId: f.pessoaId } : {}) },
    };
  });

  resumo = httpResource<RelatorioResumo[]>(() =>
    this.carregarResumo() ? { url: `${this.#base}/resumo`, params: { mes: this.filtro().mes } } : undefined,
  );

  pessoas = httpResource<Pessoa[]>(() =>
    this.carregarResumo() ? `${environment.apiUrl}/pessoas` : undefined,
  );

  salvar(mes: string, payload: SalvarRelatorioPayload) {
    return this.#http.put<Relatorio>(`${this.#base}/${mes}`, payload);
  }

  /** DOCX do relatório (o que está salvo no servidor). */
  baixarDocx(mes: string, pessoaId: string | null) {
    return this.#http.get(`${this.#base}/${mes}/docx`, {
      params: { ...(pessoaId ? { pessoaId } : {}) },
      responseType: 'blob',
    });
  }

  aprovar(mes: string) {
    return this.#http.post<Relatorio>(`${this.#base}/${mes}/aprovar`, {});
  }

  reabrir(mes: string, pessoaId: string | null) {
    return this.#http.post<Relatorio>(`${this.#base}/${mes}/reabrir`, {}, {
      params: pessoaId ? { pessoaId } : {},
    });
  }
}
