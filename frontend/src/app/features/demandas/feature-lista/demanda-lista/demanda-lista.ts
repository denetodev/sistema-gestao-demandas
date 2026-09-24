import { Component, inject, OnInit } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { DemandaService } from '../../data-access/demanda.service';
import type { StatusDemanda, Prioridade } from '../../data-access/demanda.model';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { AuthService } from '../../../../core/auth/auth.service';
@Component({
  selector: 'app-demanda-lista',
  imports: [TableModule, Tag, DatePipe, CurrencyPipe, RouterLink, Button],
  templateUrl: './demanda-lista.html',
  styleUrl: './demanda-lista.scss',
})
export class DemandaLista implements OnInit {
  #service = inject(DemandaService);
  auth = inject(AuthService);
  demandas = this.#service.listar;
  

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
}
