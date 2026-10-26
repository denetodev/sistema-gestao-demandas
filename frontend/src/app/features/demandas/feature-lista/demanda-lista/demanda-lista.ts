import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Paginator, PaginatorState } from 'primeng/paginator';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Avatar } from 'primeng/avatar';
import { AvatarGroup } from 'primeng/avatargroup';
import { Tooltip } from 'primeng/tooltip';
import { ConfirmationService } from 'primeng/api';
import { firstValueFrom } from 'rxjs';
import { DemandaService } from '../../data-access/demanda.service';
import {
  Demanda,
  DemandaFiltro,
  EscopoDemanda,
  ROTULO_PRIORIDADE,
  StatusDemanda,
} from '../../data-access/demanda.model';
import { ROTULO_STATUS } from '../../data-access/demanda-itens.model';
import { demandaAtrasada, iniciaisDe, separarCodigo, severityPrioridade, severityStatus } from '../../data-access/demanda-apresentacao';
import { AuthService } from '../../../../core/auth/auth.service';
import { NucleoService } from '../../../../core/dados-mestres/nucleo/nucleo.service';
import { AreaService } from '../../../../core/dados-mestres/area/area.service';
import { DiretoriaService } from '../../../../core/dados-mestres/diretoria/diretoria.service';
import { BarraPagina } from '../../../../core/layout/barra-pagina/barra-pagina';
import { Ajuda } from '../../../../core/ajuda/ajuda/ajuda';
import { CampoBusca } from '../../../../core/ui/campo-busca/campo-busca';
import { OpcaoPilula, Pilulas } from '../../../../core/ui/pilulas/pilulas';
import { DemandaQuadro } from '../demanda-quadro/demanda-quadro';
import { AgruparPor } from '../demanda-quadro/agrupar-quadro';
import { DemandaForm } from '../../feature-form/demanda-form/demanda-form';

const STATUS_VALIDOS = Object.keys(ROTULO_STATUS) as StatusDemanda[];

@Component({
  selector: 'app-demanda-lista',
  imports: [
    Ajuda, FormsModule, TableModule, Tag, Select, Tooltip, Avatar, AvatarGroup, DatePipe, CurrencyPipe, Button, BarraPagina,
    DemandaForm, RouterLink, Pilulas, CampoBusca, DemandaQuadro, Paginator,
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

  opcoesVisao: OpcaoPilula[] = [
    { valor: 'lista', rotulo: 'Lista' },
    { valor: 'quadro', rotulo: 'Quadro' },
  ];
  opcoesAgrupar: OpcaoPilula[] = [
    { valor: 'SITUACAO', rotulo: 'Situação' },
    { valor: 'NUCLEO', rotulo: 'Núcleo' },
    { valor: 'EQUIPE', rotulo: 'Equipe' },
    { valor: 'PESSOA', rotulo: 'Pessoa' },
  ];
  /** O quadro mostra de uma vez o máximo que a API entrega por página. */
  readonly #tamanhoQuadro = 100;
  readonly #tamanhoLista = 10;
  visao = signal<'lista' | 'quadro'>('lista');
  agrupar = signal<AgruparPor>('SITUACAO');

  iniciaisDe = iniciaisDe;
  atrasada = demandaAtrasada;
  severityStatus = severityStatus;
  severityPrioridade = severityPrioridade;

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

  /** Quantos rostos aparecem; o resto vira o "+N". */
  readonly maxRostos = 4;

  rostos(d: Demanda) {
    return (d.participantes ?? []).slice(0, this.maxRostos);
  }

  restantes(d: Demanda): number {
    return Math.max(0, (d.participantes ?? []).length - this.maxRostos);
  }

  /** Todos os nomes, para o texto que aparece ao passar o mouse. */
  nomesDoTime(d: Demanda): string {
    return (d.participantes ?? []).map((p) => p.nome).join('\n');
  }

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
    const visao = p.get('visao') === 'quadro' ? 'quadro' : 'lista';
    const agrupar = this.opcoesAgrupar.find((o) => o.valor === p.get('agrupar'))?.valor ?? 'SITUACAO';
    this.visao.set(visao);
    this.agrupar.set(agrupar as AgruparPor);
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
    const tamanho = visao === 'quadro' ? this.#tamanhoQuadro : atual.size === this.#tamanhoQuadro ? this.#tamanhoLista : atual.size;
    if (!igual || tamanho !== atual.size) this.filtro.update((f) => ({ ...f, ...novo, size: tamanho, page: igual && tamanho === atual.size ? f.page : 0 }));
  }

  mudarVisao(valor: string) {
    this.#navegar(this.filtro(), { visao: valor === 'quadro' ? 'quadro' : 'lista' });
  }

  mudarAgrupar(valor: string) {
    this.#navegar(this.filtro(), { agrupar: valor as AgruparPor });
  }

  abrirDemanda(d: Demanda) {
    this.#router.navigate(['/demandas', d.id]);
  }

  /** A linha inteira abre a demanda, menos o que já tem ação própria (link, botões de editar e cancelar). */
  aoClicarLinha(evento: Event, d: Demanda) {
    if ((evento.target as HTMLElement).closest('a, button, .p-button')) return;
    this.abrirDemanda(d);
  }

  aoMudarPaginaQuadro(e: PaginatorState) {
    const page = e.page ?? 0;
    this.filtro.update((f) => (f.page === page ? f : { ...f, page }));
  }

  /** Muda um ou mais filtros: vai para a URL, que por sua vez atualiza a lista. */
  mudar(parcial: Partial<Pick<DemandaFiltro, 'escopo' | 'busca' | 'status' | 'diretoriaId' | 'nucleoId' | 'areaId'>>) {
    this.#navegar({ ...this.filtro(), ...parcial }, {});
  }

  #navegar(f: DemandaFiltro, visual: { visao?: 'lista' | 'quadro'; agrupar?: AgruparPor }) {
    const visao = visual.visao ?? this.visao();
    const agrupar = visual.agrupar ?? this.agrupar();
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
        visao: visao === 'quadro' ? 'quadro' : null,
        agrupar: visao === 'quadro' && agrupar !== 'SITUACAO' ? agrupar : null,
      },
    });
  }

  limparFiltros() {
    this.mudar({ busca: '', status: '', diretoriaId: '', nucleoId: '', areaId: '' });
  }

  apresentar(d: Demanda) {
    return separarCodigo(d.titulo, d.codigo);
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
