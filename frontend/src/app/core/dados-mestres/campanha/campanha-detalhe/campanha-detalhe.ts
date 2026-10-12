import { Component, computed, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { httpResource } from '@angular/common/http';
import { map } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Message } from 'primeng/message';
import { environment } from '../../../../../environments/environment';
import { BarraPagina } from '../../../layout/barra-pagina/barra-pagina';
import { Ajuda } from '../../../ajuda/ajuda/ajuda';
import { ROTULO_STATUS } from '../../../../features/demandas/data-access/demanda-itens.model';
import type { StatusDemanda } from '../../../../features/demandas/data-access/demanda.model';
import type { CampanhaDetalhe } from '../campanha-resumo.model';

/** Uma campanha vista como um todo: quantas pessoas, demandas e quanto valor ela reuniu. */
@Component({
  selector: 'app-campanha-detalhe',
  imports: [Ajuda, RouterLink, CurrencyPipe, DatePipe, TableModule, Tag, Message, BarraPagina],
  templateUrl: './campanha-detalhe.html',
  styleUrl: './campanha-detalhe.scss',
})
export class CampanhaDetalheComponent {
  #route = inject(ActivatedRoute);

  id = toSignal(this.#route.paramMap.pipe(map((p) => p.get('id') ?? '')), { initialValue: '' });
  detalhe = httpResource<CampanhaDetalhe>(() => `${environment.apiUrl}/campanhas/${this.id()}/resumo`);

  rotuloStatus: Record<string, string> = ROTULO_STATUS;
  statusEmOrdem = Object.keys(ROTULO_STATUS) as StatusDemanda[];

  /** Só os status que aparecem na campanha, para a faixa de situação não encher de zeros. */
  situacao = computed(() => {
    const r = this.detalhe.value()?.resumo;
    if (!r) return [];
    return this.statusEmOrdem
      .filter((s) => (r.porStatus[s] ?? 0) > 0)
      .map((s) => ({ status: s, rotulo: ROTULO_STATUS[s], quantidade: r.porStatus[s] }));
  });

  severidade(status: StatusDemanda) {
    switch (status) {
      case 'CONCLUIDA': return 'success';
      case 'EM_ANDAMENTO': return 'info';
      case 'EM_APROVACAO': return 'warn';
      case 'CANCELADA': return 'danger';
      default: return 'secondary';
    }
  }
}
