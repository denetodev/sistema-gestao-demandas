import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { DemandaService } from '../../data-access/demanda.service';
import type { StatusDemanda, Prioridade, Demanda } from '../../data-access/demanda.model';
import { Button } from 'primeng/button';
import { AuthService } from '../../../../core/auth/auth.service';
import { BarraPagina } from '../../../../core/layout/barra-pagina/barra-pagina';
import { DemandaForm } from '../../feature-form/demanda-form/demanda-form';
import { ConfirmationService } from 'primeng/api';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-demanda-lista',
  imports: [TableModule, Tag, DatePipe, CurrencyPipe, Button, BarraPagina, DemandaForm],
  templateUrl: './demanda-lista.html',
  styleUrl: './demanda-lista.scss',
})
export class DemandaLista implements OnInit {
  #service = inject(DemandaService);
  auth = inject(AuthService);
  demandas = this.#service.listar;
  dialogAberto = signal(false);
  demandaEditandoId = signal<string | null>(null);
  #confirmationService = inject(ConfirmationService);


  ngOnInit() {
    this.demandas.reload();
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
}
