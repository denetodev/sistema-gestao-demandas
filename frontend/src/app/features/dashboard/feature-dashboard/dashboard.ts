import { Component, computed, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { Checkbox } from 'primeng/checkbox';
import { DatePicker } from 'primeng/datepicker';
import { TableModule } from 'primeng/table';
import { Message } from 'primeng/message';
import { AuthService } from '../../../core/auth/auth.service';
import { BarraPagina } from '../../../core/layout/barra-pagina/barra-pagina';
import { DiretoriaService } from '../../../core/dados-mestres/diretoria/diretoria.service';
import { DemandaService } from '../../demandas/data-access/demanda.service';
import type { EscopoDemanda, StatusDemanda } from '../../demandas/data-access/demanda.model';
import { ROTULO_STATUS } from '../../demandas/data-access/demanda-itens.model';
import { DashboardService } from '../data-access/dashboard.service';
import { MapaCalor } from '../ui-mapa-calor/mapa-calor';

const STATUS: StatusDemanda[] = ['NAO_INICIADA', 'EM_ANDAMENTO', 'EM_APROVACAO', 'CONCLUIDA', 'CANCELADA'];

@Component({
  selector: 'app-dashboard',
  imports: [
    CurrencyPipe, FormsModule, Button, Select, Checkbox, DatePicker, TableModule, Message, BarraPagina, MapaCalor,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  auth = inject(AuthService);
  #service = inject(DashboardService);
  #demandas = inject(DemandaService);
  #router = inject(Router);

  dashboard = this.#service.dashboard;
  filtro = this.#service.filtro;
  diretorias = inject(DiretoriaService).listar;

  status = STATUS;
  rotuloStatus = ROTULO_STATUS;

  ehGestorOuAdmin = computed(() => this.auth.temPerfil('ADMIN', 'GESTOR'));
  ehVisualizador = computed(() => this.auth.temPerfil('VISUALIZADOR'));
  ehReferencia = computed(
    () => this.auth.temPerfil('PROFISSIONAL') && !!this.auth.pessoa()?.referenciaAreaId,
  );

  opcoesDiretoria = computed(() => [
    { id: null, nome: 'Todas as diretorias' },
    ...(this.diretorias.value() ?? []),
  ]);

  opcoesEquipe = computed(() => {
    const areaId = this.auth.pessoa()?.referenciaAreaId;
    const colegas = (this.#service.pessoas.value() ?? [])
      .filter((p) => p.areaId === areaId && p.status === 'ATIVO' && p.aprovadoEm && p.perfil === 'PROFISSIONAL')
      .map((p) => ({ id: p.id as string | null, nome: p.nome }));
    return [{ id: null, nome: `Equipe ${this.auth.pessoa()?.referenciaAreaNome ?? ''} (agregado)` }, ...colegas];
  });

  mes = computed(() => {
    const m = this.filtro().mes;
    if (!m) return new Date();
    const [a, mm] = m.split('-').map(Number);
    return new Date(a, mm - 1, 1);
  });

  mesAtual = computed(() => {
    const [a, m] = (this.dashboard.value()?.mes ?? '').split('-').map(Number);
    return a ? new Date(a, m - 1, 1) : new Date();
  });

  nomeMes = computed(() =>
    this.mesAtual().toLocaleDateString('pt-BR', { month: 'long' }),
  );
  ano = computed(() => this.mesAtual().getFullYear());

  mudarPessoa(pessoaId: string | null) {
    this.filtro.update((f) => ({ ...f, pessoaId }));
  }

  mudarDiretoria(diretoriaId: string | null) {
    this.filtro.update((f) => ({ ...f, diretoriaId }));
  }

  mudarPorPessoa(porPessoa: boolean) {
    this.filtro.update((f) => ({ ...f, porPessoa }));
  }

  mudarMes(data: Date | null) {
    const mes = data
      ? `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}`
      : null;
    this.filtro.update((f) => ({ ...f, mes }));
  }

  irParaPessoas() {
    this.#router.navigateByUrl('/pessoas');
  }

  irParaDemandas(escopo: EscopoDemanda) {
    this.#demandas.filtro.update((f) => ({ ...f, escopo, page: 0 }));
    this.#router.navigateByUrl('/demandas');
  }
}
