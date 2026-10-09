import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { httpResource } from '@angular/common/http';
import { firstValueFrom, map } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { Select } from 'primeng/select';
import { Message } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { environment } from '../../../../../environments/environment';
import { AuthService } from '../../../../core/auth/auth.service';
import { BarraPagina } from '../../../../core/layout/barra-pagina/barra-pagina';
import { DemandaService } from '../../data-access/demanda.service';
import type { Demanda, StatusDemanda, Prioridade } from '../../data-access/demanda.model';
import { DemandaItensService } from '../../data-access/demanda-itens.service';
import { ROTULO_STATUS } from '../../data-access/demanda-itens.model';
import { DemandaForm } from '../../feature-form/demanda-form/demanda-form';
import { SecaoParticipantes } from '../secao-participantes/secao-participantes';
import { SecaoPecas } from '../secao-pecas/secao-pecas';
import { SecaoAtividades } from '../secao-atividades/secao-atividades';
import { SecaoEvidencias } from '../secao-evidencias/secao-evidencias';

@Component({
  selector: 'app-demanda-detalhe',
  imports: [
    RouterLink, CurrencyPipe, DatePipe, FormsModule, TableModule, Button, Tag, Select, Message, BarraPagina, DemandaForm,
    SecaoParticipantes, SecaoPecas, SecaoAtividades, SecaoEvidencias,
  ],
  templateUrl: './demanda-detalhe.html',
  styleUrl: './demanda-detalhe.scss',
})
export class DemandaDetalhe {
  #route = inject(ActivatedRoute);
  #itens = inject(DemandaItensService);
  #demandas = inject(DemandaService);
  auth = inject(AuthService);

  id = toSignal(this.#route.paramMap.pipe(map((p) => p.get('id') ?? '')), { initialValue: '' });

  demanda = httpResource<Demanda>(() => `${environment.apiUrl}/demandas/${this.id()}`);
  participantes = this.#itens.participantes(this.id);
  pecas = this.#itens.pecas(this.id);
  atividades = this.#itens.atividades(this.id);
  evidencias = this.#itens.evidencias();
  historico = this.#itens.historico(this.id);

  dialogAberto = signal(false);

  cancelada = computed(() => this.demanda.value()?.status === 'CANCELADA');
  editavel = computed(() => this.auth.podeEditar() && !this.cancelada());
  atividadesLista = computed(() => this.atividades.value()?.content ?? []);

  opcoesStatus = (Object.keys(ROTULO_STATUS) as StatusDemanda[]).map((v) => ({
    value: v,
    label: ROTULO_STATUS[v],
  }));
  rotuloStatus: Record<string, string> = ROTULO_STATUS;

  atrasada = computed(() => {
    const d = this.demanda.value();
    if (!d?.dataPrazo || d.status === 'CONCLUIDA' || d.status === 'CANCELADA') return false;
    return new Date(d.dataPrazo + 'T23:59:59') < new Date();
  });

  severityPrioridade(p: Prioridade) {
    return p === 'URGENTE' ? 'danger' : p === 'ALTA' ? 'warn' : p === 'NORMAL' ? 'info' : 'secondary';
  }

  async mudarStatus(status: StatusDemanda) {
    try {
      await firstValueFrom(this.#itens.atualizarStatus(this.id(), status));
    } catch {
      // toast do interceptor
    }
    this.demanda.reload();
    this.historico.reload();
  }

  async cancelar() {
    try {
      await firstValueFrom(this.#demandas.excluir(this.id()));
      this.demanda.reload();
      this.historico.reload();
    } catch {
      // toast do interceptor
    }
  }

  recarregarTudo() {
    this.demanda.reload();
    this.participantes.reload();
    this.pecas.reload();
    this.atividades.reload();
    this.evidencias.reload();
    this.historico.reload();
  }
}
