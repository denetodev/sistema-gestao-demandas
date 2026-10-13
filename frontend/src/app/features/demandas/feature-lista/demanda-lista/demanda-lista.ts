import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { ConfirmationService } from 'primeng/api';
import { firstValueFrom } from 'rxjs';
import { DemandaService } from '../../data-access/demanda.service';
import {
  Demanda,
  DemandaFiltro,
  EscopoDemanda,
  Prioridade,
  ROTULO_PRIORIDADE,
  StatusDemanda,
} from '../../data-access/demanda.model';
import { ROTULO_STATUS } from '../../data-access/demanda-itens.model';
import { separarCodigo } from '../../data-access/demanda-apresentacao';
import { AuthService } from '../../../../core/auth/auth.service';
import { NucleoService } from '../../../../core/dados-mestres/nucleo/nucleo.service';
import { AreaService } from '../../../../core/dados-mestres/area/area.service';
import { DiretoriaService } from '../../../../core/dados-mestres/diretoria/diretoria.service';
import { BarraPagina } from '../../../../core/layout/barra-pagina/barra-pagina';
import { Ajuda } from '../../../../core/ajuda/ajuda/ajuda';
import { CampoBusca } from '../../../../core/ui/campo-busca/campo-busca';
import { OpcaoPilula, Pilulas } from '../../../../core/ui/pilulas/pilulas';
import { DemandaForm } from '../../feature-form/demanda-form/demanda-form';

const STATUS_VALIDOS = Object.keys(ROTULO_STATUS) as StatusDemanda[];

@Component({
  selector: 'app-demanda-lista',
  imports: [
    Ajuda, FormsModule, TableModule, Tag, Select, Tooltip, DatePipe, CurrencyPipe, Button, BarraPagina,
    DemandaForm, RouterLink, Pilulas, CampoBusca,
  ],
  templateUrl: './demanda-lista.html',
  styleUrl: './demanda-lista.scss',
})
export class DemandaLista {
  #service = inject(DemandaService);
  #router = inject(Router);
  #route = inject(ActivatedRoute);
  #confirmationService = inject(ConfirmationService);

  auth = inject(AuthService);
  demandas = this.#service.listar;
  filtro = this.#service.filtro;
  dialogAberto = signal(false);
  demandaEditandoId = signal<string | null>(null);

  #diretorias = inject(DiretoriaService).listar;
  #nucleos = inject(NucleoService).listar;
  #areas = inject(AreaService).listar;

  rotuloStatus: Record<string, string> = ROTULO_STATUS;
  rotuloPrioridade: Record<string, string> = ROTULO_PRIORIDADE;

  ehGestao = computed(() => this.auth.temPerfil('ADMIN', 'GESTOR'));

  /** Os botões de escopo mostram o nome real da equipe e da diretoria da pessoa. */
  opcoesEscopo = computed<OpcaoPilula[]>(() => {
    const p = this.auth.pessoa();
    const opcoes: OpcaoPilula[] = [];
    if (!this.auth.temPerfil('VISUALIZADOR')) opcoes.push({ valor: 'MINHAS', rotulo: 'Minhas' });
    if (p?.referenciaAreaId || p?.areaId) {
      opcoes.push({ valor: 'EQUIPE', rotulo: 'Minha equipe', detalhe: p.referenciaAreaNome ?? p.areaNome });
    }
    if (p?.nucleoId) opcoes.push({ valor: 'NUCLEO', rotulo: 'Meu núcleo', detalhe: p.nucleoNome });
    opcoes.push({ valor: 'DIRETORIA', rotulo: 'Minha diretoria', detalhe: p?.diretoriaNome });
    if (this.ehGestao()) opcoes.push({ valor: 'TODAS', rotulo: 'Todas' });
    return opcoes;
  });

  opcoesStatus = STATUS_VALIDOS.map((valor) => ({ valor, rotulo: ROTULO_STATUS[valor] }));
  opcoesDiretoria = computed(() => (this.#diretorias.value() ?? []).map((d) => ({ valor: d.id, rotulo: d.nome })));
  opcoesNucleo = computed(() =>
    (this.#nucleos.value() ?? []).map((n) => ({
      valor: n.id,
      rotulo: n.diretoriaNome ? `${n.nome} · ${n.diretoriaNome}` : n.nome,
    })),
  );
  /** O mesmo nome de equipe existe em diretorias diferentes (Design em CRM e em UGR), então a diretoria vai junto. */
  opcoesEquipe = computed(() =>
    (this.#areas.value() ?? []).map((a) => ({
      valor: a.id,
      rotulo: a.diretoriaNome ? `${a.nome} · ${a.diretoriaNome}` : a.nome,
    })),
  );

  filtrosAtivos = computed(() => {
    const f = this.filtro();
    return !!(f.busca || f.status || f.diretoriaId || f.nucleoId || f.areaId);
  });

  constructor() {
    // A URL manda nos filtros: busca do topo, atalhos do dashboard e links compartilhados chegam por ela.
    this.#route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((p) => this.#aplicarUrl(p));
  }

  #escopoPadrao(): EscopoDemanda {
    return (this.opcoesEscopo()[0]?.valor ?? 'DIRETORIA') as EscopoDemanda;
  }

  #aplicarUrl(p: ParamMap) {
    const permitidos = this.opcoesEscopo().map((o) => o.valor);
    const doUrl = p.get('escopo');
    const status = p.get('status') as StatusDemanda | null;
    const novo = {
      escopo: (doUrl && permitidos.includes(doUrl) ? doUrl : this.#escopoPadrao()) as EscopoDemanda,
      busca: p.get('busca') ?? '',
      status: status && STATUS_VALIDOS.includes(status) ? status : ('' as const),
      diretoriaId: p.get('diretoria') ?? '',
      nucleoId: p.get('nucleo') ?? '',
      areaId: p.get('equipe') ?? '',
    };
    const atual = this.filtro();
    const igual = (Object.keys(novo) as (keyof typeof novo)[]).every((k) => atual[k] === novo[k]);
    if (!igual) this.filtro.update((f) => ({ ...f, ...novo, page: 0 }));
  }

  /** Muda um ou mais filtros: vai para a URL, que por sua vez atualiza a lista. */
  mudar(parcial: Partial<Pick<DemandaFiltro, 'escopo' | 'busca' | 'status' | 'diretoriaId' | 'nucleoId' | 'areaId'>>) {
    const f = { ...this.filtro(), ...parcial };
    this.#router.navigate([], {
      relativeTo: this.#route,
      replaceUrl: true,
      queryParams: {
        escopo: f.escopo !== this.#escopoPadrao() ? f.escopo : null,
        busca: f.busca || null,
        status: f.status || null,
        diretoria: f.diretoriaId || null,
        nucleo: f.nucleoId || null,
        equipe: f.areaId || null,
      },
    });
  }

  limparFiltros() {
    this.mudar({ busca: '', status: '', diretoriaId: '', nucleoId: '', areaId: '' });
  }

  apresentar(d: Demanda) {
    return separarCodigo(d.titulo, d.codigo);
  }

  /** Prazo vencido numa demanda que ainda está em aberto. */
  atrasada(d: Demanda): boolean {
    if (!d.dataPrazo || d.status === 'CONCLUIDA' || d.status === 'CANCELADA') return false;
    return new Date(d.dataPrazo + 'T23:59:59') < new Date();
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
    const size = evento.rows ?? 10;
    const page = Math.floor((evento.first ?? 0) / size);
    const sort = evento.sortField
      ? `${evento.sortField},${evento.sortOrder === -1 ? 'desc' : 'asc'}`
      : undefined;

    this.filtro.update((f) => (f.page === page && f.size === size && f.sort === sort ? f : { ...f, page, size, sort }));
  }
}
