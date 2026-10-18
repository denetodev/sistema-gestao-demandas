import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormsModule, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { ConfirmationService } from 'primeng/api';
import { ProjetoService } from '../../projeto/projeto.service';
import { formatarData, paraData } from '../../../../core/util/data';
import type { Campanha } from '../campanha.model';
import { CampanhaService } from '../campanha.service';
import { AuthService } from '../../../auth/auth.service';
import { BarraPagina } from '../../../layout/barra-pagina/barra-pagina';

import { Ajuda } from '../../../ajuda/ajuda/ajuda';
import { CampoBusca } from '../../../ui/campo-busca/campo-busca';
import { OpcaoPilula, Pilulas } from '../../../ui/pilulas/pilulas';
import { contemTexto, maisRecentesPrimeiro } from '../../../util/listas';

@Component({
  selector: 'app-campanha-lista',
  imports: [FormsModule, CampoBusca, Pilulas, Ajuda, ReactiveFormsModule, TableModule, Dialog, InputText, DatePicker, Select, Button, Message, DatePipe, CurrencyPipe, RouterLink, BarraPagina],
  templateUrl: './campanha-lista.html',
  styleUrl: './campanha-lista.scss',
})
export class CampanhaLista {
  #fb = inject(FormBuilder);
  #service = inject(CampanhaService);
  #confirmationService = inject(ConfirmationService);

  campanhas = this.#service.listar;
  resumos = this.#service.resumos;
  #resumoPorId = computed(() => new Map((this.resumos.value() ?? []).map((r) => [r.id, r])));
  resumo(id: string) {
    return this.#resumoPorId().get(id);
  }
  projetos = inject(ProjetoService).listar;

  // filtros da lista: as últimas cadastradas primeiro, 10 por página
  busca = signal('');
  usoFiltro = signal('');
  projetoFiltro = signal<string | null>(null);
  opcoesUso: OpcaoPilula[] = [
    { valor: '', rotulo: 'Todas' },
    { valor: 'COM', rotulo: 'Com demandas' },
    { valor: 'SEM', rotulo: 'Sem demandas' },
  ];
  opcoesProjeto = computed(() => (this.projetos.value() ?? []).map((p) => ({ valor: p.id, rotulo: p.nome })));

  filtrados = computed(() =>
    maisRecentesPrimeiro(this.campanhas.value() ?? []).filter((c) => {
      const demandas = this.resumo(c.id)?.demandas ?? 0;
      return (
        contemTexto([c.nome, c.codigo, c.projetoNome], this.busca()) &&
        (!this.projetoFiltro() || c.projetoId === this.projetoFiltro()) &&
        (!this.usoFiltro() || (this.usoFiltro() === 'COM' ? demandas > 0 : demandas === 0))
      );
    }),
  );

  filtrosAtivos = computed(() => !!(this.busca() || this.usoFiltro() || this.projetoFiltro()));

  limparFiltros() {
    this.busca.set('');
    this.usoFiltro.set('');
    this.projetoFiltro.set(null);
  }

  dialogAberto = signal(false);
  editandoId = signal<string | null>(null);
  salvando = signal(false);
  erro = signal<string | null>(null);

  form = this.#fb.nonNullable.group(
    {
      nome: ['', [Validators.required, Validators.maxLength(180), semSoEspacos]],
      projetoId: this.#fb.control<string | null>(null),
      codigo: ['', Validators.maxLength(40)],
      dataInicio: this.#fb.control<Date | null>(null),
      dataFim: this.#fb.control<Date | null>(null),
    },
    { validators: fimDepoisDoInicio },
  );

  /** O erro de uma caixa só aparece depois que a pessoa mexeu nela (ou tentou salvar). */
  mostrarErro(campo: 'nome' | 'codigo'): boolean {
    const c = this.form.controls[campo];
    return c.invalid && (c.touched || c.dirty);
  }
  auth = inject(AuthService);

  abrirNovo() {
    this.editandoId.set(null);
    this.erro.set(null);
    this.form.reset();
    this.dialogAberto.set(true);
  }

  abrirEdicao(campanha: Campanha) {
    this.editandoId.set(campanha.id);
    this.erro.set(null);
    this.form.setValue({
      nome: campanha.nome,
      projetoId: campanha.projetoId,
      codigo: campanha.codigo ?? '',
      dataInicio: paraData(campanha.dataInicio),
      dataFim: paraData(campanha.dataFim),
    });
    this.dialogAberto.set(true);
  }

  async salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    this.erro.set(null);
    const v = this.form.getRawValue();
    const payload = {
      nome: v.nome.trim(),
      projetoId: v.projetoId,
      codigo: v.codigo.trim().toUpperCase() || null,
      dataInicio: formatarData(v.dataInicio),
      dataFim: formatarData(v.dataFim),
    };
    const id = this.editandoId();
    try {
      await firstValueFrom(id ? this.#service.atualizar(id, payload) : this.#service.criar(payload));
      this.dialogAberto.set(false);
      this.campanhas.reload();
    } catch (e) {
      const mensagem = (e as { error?: { mensagem?: string } })?.error?.mensagem;
      this.erro.set(mensagem ?? 'Não foi possível salvar a campanha. Tente de novo.');
    } finally {
      this.salvando.set(false);
    }
  }

  confirmarExclusao(campanha: Campanha) {
    this.#confirmationService.confirm({
      header: 'Remover campanha',
      message: `Remover "${campanha.nome}"? Esta ação não pode ser desfeita.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Remover',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.#excluir(campanha),
    });
  }

  async #excluir(campanha: Campanha) {
    try {
      await firstValueFrom(this.#service.excluir(campanha.id));
      this.campanhas.reload();
    } catch {
      // o toast do interceptor já informou o motivo (409 = em uso)
    }
  }
}
/** Nome só com espaços conta como vazio. */
function semSoEspacos(c: AbstractControl): ValidationErrors | null {
  return typeof c.value === 'string' && c.value.length > 0 && c.value.trim() === '' ? { required: true } : null;
}

/** A data de fim não pode vir antes da de início. */
function fimDepoisDoInicio(g: AbstractControl): ValidationErrors | null {
  const inicio = g.get('dataInicio')?.value as Date | null;
  const fim = g.get('dataFim')?.value as Date | null;
  return inicio && fim && fim < inicio ? { fimAntesDoInicio: true } : null;
}
