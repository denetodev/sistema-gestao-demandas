import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { Checkbox } from 'primeng/checkbox';
import { Textarea } from 'primeng/textarea';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Message } from 'primeng/message';
import { ConfirmationService } from 'primeng/api';
import { AuthService } from '../../../core/auth/auth.service';
import { BarraPagina } from '../../../core/layout/barra-pagina/barra-pagina';
import { RelatorioService, mesAtual } from '../data-access/relatorio.service';
import type { ItemAtividade, RelatorioResumo } from '../data-access/relatorio.model';

const AVULSAS = 'Atividades avulsas (sem demanda)';

@Component({
  selector: 'app-relatorio',
  imports: [
    CurrencyPipe, DatePipe, FormsModule, Button, Select, Checkbox, Textarea, TableModule, Tag, Message,
    BarraPagina,
  ],
  templateUrl: './relatorio.html',
  styleUrl: './relatorio.scss',
})
export class RelatorioMensal {
  #service = inject(RelatorioService);
  #confirmacao = inject(ConfirmationService);
  auth = inject(AuthService);

  relatorio = this.#service.relatorio;
  resumo = this.#service.resumo;
  filtro = this.#service.filtro;

  salvando = signal(false);
  /** Edição local, ainda não salva. */
  excluidas = signal<Set<string>>(new Set());
  observacoes = signal('');
  alterado = signal(false);

  ehGestorOuAdmin = computed(() => this.auth.temPerfil('ADMIN', 'GESTOR'));

  opcoesMes = this.#gerarMeses();

  opcoesPessoa = computed(() => {
    const eu = this.auth.pessoa();
    const profissionais = (this.#service.pessoas.value() ?? [])
      .filter((p) => p.status === 'ATIVO' && p.aprovadoEm && p.perfil === 'PROFISSIONAL' && p.id !== eu?.id)
      .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
      .map((p) => ({ id: p.id as string | null, nome: p.nome }));
    return [{ id: null as string | null, nome: `${eu?.nome ?? 'Eu'} (meu relatório)` }, ...profissionais];
  });

  aprovado = computed(() => this.relatorio.value()?.status === 'APROVADO');
  editavel = computed(() => !!this.relatorio.value()?.podeEditar && !this.aprovado());

  /** O que aparece no documento: aprovado vem congelado; aberto respeita a seleção local. */
  incluidas = computed<ItemAtividade[]>(() => {
    const r = this.relatorio.value();
    if (!r) return [];
    if (r.status === 'APROVADO') return r.atividades;
    const fora = this.excluidas();
    return r.atividades.filter((a) => !fora.has(a.id));
  });

  grupos = computed(() => {
    const mapa = new Map<string, ItemAtividade[]>();
    for (const a of this.incluidas()) {
      const chave = a.demandaTitulo ?? AVULSAS;
      mapa.set(chave, [...(mapa.get(chave) ?? []), a]);
    }
    return [...mapa.entries()]
      .sort(([a], [b]) => Number(a === AVULSAS) - Number(b === AVULSAS) || a.localeCompare(b, 'pt-BR'))
      .map(([titulo, itens]) => ({ titulo, itens }));
  });

  nomeMes = computed(() => this.#rotuloMes(this.filtro().mes));

  #situacoes = {
    NAO_INICIADO: { rotulo: 'Não iniciado', severidade: 'secondary' as const },
    ABERTO: { rotulo: 'Em preparação', severidade: 'warn' as const },
    APROVADO: { rotulo: 'Aprovado', severidade: 'success' as const },
  };

  situacao(linha: RelatorioResumo) {
    return this.#situacoes[linha.situacao];
  }

  constructor() {
    effect(() => {
      this.#service.carregarResumo.set(this.ehGestorOuAdmin());
    });

    // sempre que chega um relatório do servidor, a edição local recomeça dele
    effect(() => {
      const r = this.relatorio.value();
      if (!r) return;
      untracked(() => {
        this.excluidas.set(new Set(r.atividades.filter((a) => !a.incluida).map((a) => a.id)));
        this.observacoes.set(r.observacoes ?? '');
        this.alterado.set(false);
      });
    });
  }

  mudarMes(mes: string) {
    this.filtro.update((f) => ({ ...f, mes }));
  }

  mudarPessoa(pessoaId: string | null) {
    this.filtro.update((f) => ({ ...f, pessoaId }));
  }

  abrirDe(linha: RelatorioResumo) {
    const eu = this.auth.pessoa()?.id;
    this.mudarPessoa(linha.pessoaId === eu ? null : linha.pessoaId);
  }

  alternar(id: string, incluir: boolean) {
    this.excluidas.update((s) => {
      const novo = new Set(s);
      if (incluir) novo.delete(id);
      else novo.add(id);
      return novo;
    });
    this.alterado.set(true);
  }

  editarObservacoes(texto: string) {
    this.observacoes.set(texto);
    this.alterado.set(true);
  }

  async salvar(): Promise<boolean> {
    const mes = this.filtro().mes;
    this.salvando.set(true);
    try {
      await firstValueFrom(
        this.#service.salvar(mes, {
          observacoes: this.observacoes().trim() || null,
          atividadesExcluidas: [...this.excluidas()],
        }),
      );
      this.alterado.set(false);
      this.relatorio.reload();
      this.resumo.reload();
      return true;
    } catch {
      return false; // o toast do interceptor já informou o motivo
    } finally {
      this.salvando.set(false);
    }
  }

  confirmarAprovacao() {
    this.#confirmacao.confirm({
      header: 'Aprovar relatório',
      message: `Aprovar o relatório de ${this.nomeMes()}? O conteúdo fica congelado. Você pode reabrir depois, se precisar corrigir.`,
      icon: 'pi pi-check-circle',
      acceptLabel: 'Aprovar',
      rejectLabel: 'Voltar',
      accept: () => this.#aprovar(),
    });
  }

  async #aprovar() {
    if (this.alterado() && !(await this.salvar())) return;
    this.salvando.set(true);
    try {
      await firstValueFrom(this.#service.aprovar(this.filtro().mes));
      this.relatorio.reload();
      this.resumo.reload();
    } catch {
      // toast do interceptor
    } finally {
      this.salvando.set(false);
    }
  }

  confirmarReabertura() {
    this.#confirmacao.confirm({
      header: 'Reabrir relatório',
      message: `Reabrir o relatório de ${this.nomeMes()}? O conteúdo aprovado deixa de ser congelado e volta a refletir as atividades do mês.`,
      icon: 'pi pi-lock-open',
      acceptLabel: 'Reabrir',
      rejectLabel: 'Voltar',
      accept: () => this.#reabrir(),
    });
  }

  async #reabrir() {
    this.salvando.set(true);
    try {
      await firstValueFrom(this.#service.reabrir(this.filtro().mes, this.filtro().pessoaId));
      this.relatorio.reload();
      this.resumo.reload();
    } catch {
      // toast do interceptor
    } finally {
      this.salvando.set(false);
    }
  }

  /** O PDF sai do diálogo de impressão do navegador ("Salvar como PDF"); o título vira o nome do arquivo. */
  imprimir() {
    const r = this.relatorio.value();
    const tituloOriginal = document.title;
    if (r) document.title = `Relatorio ${r.mes} - ${r.pessoaNome}`;
    window.print();
    document.title = tituloOriginal;
  }

  #rotuloMes(mes: string): string {
    const [a, m] = mes.split('-').map(Number);
    return new Date(a, m - 1, 1).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
  }

  #gerarMeses() {
    const atual = mesAtual();
    const [a, m] = atual.split('-').map(Number);
    return Array.from({ length: 24 }, (_, i) => {
      const d = new Date(a, m - 1 - i, 1);
      const valor = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      return { valor, rotulo: this.#rotuloMes(valor) };
    });
  }
}
