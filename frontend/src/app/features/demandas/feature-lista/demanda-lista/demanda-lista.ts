import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SelectButton } from 'primeng/selectbutton';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { DemandaService } from '../../data-access/demanda.service';
import type { StatusDemanda, Prioridade, Demanda, EscopoDemanda } from '../../data-access/demanda.model';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { AuthService } from '../../../../core/auth/auth.service';
import { BarraPagina } from '../../../../core/layout/barra-pagina/barra-pagina';
import { DemandaForm } from '../../feature-form/demanda-form/demanda-form';
import { ConfirmationService } from 'primeng/api';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-demanda-lista',
  imports: [FormsModule, SelectButton, TableModule, Tag, DatePipe, CurrencyPipe, Button, BarraPagina, DemandaForm, RouterLink],
  templateUrl: './demanda-lista.html',
  styleUrl: './demanda-lista.scss',
})
export class DemandaLista implements OnInit {
  #service = inject(DemandaService);
  auth = inject(AuthService);
  demandas = this.#service.listar;
  filtro = this.#service.filtro;
  dialogAberto = signal(false);
  demandaEditandoId = signal<string | null>(null);
  #confirmationService = inject(ConfirmationService);

  /** Só aparecem os escopos que o perfil pode pedir (o backend também valida). */
  escopos = computed(() => {
    const opcoes: { label: string; value: EscopoDemanda }[] = [];
    if (!this.auth.temPerfil('VISUALIZADOR')) opcoes.push({ label: 'Minhas', value: 'MINHAS' });
    if (this.auth.pessoa()?.referenciaAreaId) opcoes.push({ label: 'Minha equipe', value: 'EQUIPE' });
    opcoes.push({ label: 'Minha diretoria', value: 'DIRETORIA' });
    if (this.auth.temPerfil('ADMIN', 'GESTOR')) opcoes.push({ label: 'Todas', value: 'TODAS' });
    return opcoes;
  });

  ngOnInit() {
    // o escopo guardado pode não valer para este perfil (ex.: Visualizador abre em "Minha diretoria")
    if (!this.escopos().some((o) => o.value === this.filtro().escopo)) {
      this.filtro.update((f) => ({ ...f, escopo: this.escopos()[0].value, page: 0 }));
    }
    this.demandas.reload();
  }

  mudarEscopo(escopo: EscopoDemanda | null) {
    if (!escopo) return;
    this.filtro.update((f) => ({ ...f, escopo, page: 0 }));
  }

  severityStatus(status: StatusDemanda) {
    switch (status) {
      case 'CONCLUIDA':
        return 'success';
      case 'EM_ANDAMENTO':
        return 'info';
      case 'EM_APROVACAO':
        return 'warn';
      case 'CANCELADA':
        return 'danger';
      default:
        return 'secondary'; // NAO_INICIADA
    }
  }

  severityPrioridade(prioridade: Prioridade) {
    switch (prioridade) {
      case 'URGENTE':
        return 'danger';
      case 'ALTA':
        return 'warn';
      case 'NORMAL':
        return 'info';
      default:
        return 'secondary'; // BAIXA
    }
  }

  abrirNovo() {
    this.demandaEditandoId.set(null);
    this.dialogAberto.set(true);
  }

  abrirEdicao(demanda: Demanda) {
    this.demandaEditandoId.set(demanda.id);
    this.dialogAberto.set(true);
  }

  confirmarExclusao(demanda: Demanda) {
    this.#confirmationService.confirm({
      header: 'Cancelar demanda',
      message: `Cancelar "${demanda.titulo}"? A demanda deixa de aparecer como ativa, mas o histórico é mantido.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Cancelar demanda',
      rejectLabel: 'Voltar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.#excluir(demanda),
    });
  }

  async #excluir(demanda: Demanda) {
    try {
      await firstValueFrom(this.#service.excluir(demanda.id));
      this.demandas.reload();
    } catch {
      // o toast do interceptor já informou o motivo
    }
  }

  aoCarregar(evento: TableLazyLoadEvent) {
    const size = evento.rows ?? 20;
    const page = Math.floor((evento.first ?? 0) / size);
    const sort = evento.sortField
      ? `${evento.sortField},${evento.sortOrder === -1 ? 'desc' : 'asc'}`
      : undefined;

    this.filtro.update((f) => ({ ...f, page, size, sort }));
  }
}
