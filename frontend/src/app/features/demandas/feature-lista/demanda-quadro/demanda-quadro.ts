import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { Avatar } from 'primeng/avatar';
import { Tag } from 'primeng/tag';
import { Demanda, ROTULO_PRIORIDADE } from '../../data-access/demanda.model';
import { ROTULO_STATUS } from '../../data-access/demanda-itens.model';
import { demandaAtrasada, iniciaisDe, separarCodigo, severityPrioridade, severityStatus } from '../../data-access/demanda-apresentacao';
import { AgruparPor, agruparQuadro } from './agrupar-quadro';

/**
 * Quadro das demandas, no estilo do Planner: uma coluna por situação, núcleo, equipe ou pessoa, e um card por demanda.
 * Clicar no card abre a demanda.
 */
@Component({
  selector: 'app-demanda-quadro',
  imports: [CurrencyPipe, DatePipe, Avatar, Tag],
  templateUrl: './demanda-quadro.html',
  styleUrl: './demanda-quadro.scss',
})
export class DemandaQuadro {
  demandas = input.required<Demanda[]>();
  agruparPor = input.required<AgruparPor>();
  carregando = input(false);
  abrir = output<Demanda>();

  colunas = computed(() => agruparQuadro(this.demandas(), this.agruparPor()));

  rotuloStatus: Record<string, string> = ROTULO_STATUS;
  rotuloPrioridade: Record<string, string> = ROTULO_PRIORIDADE;
  severityStatus = severityStatus;
  severityPrioridade = severityPrioridade;
  atrasada = demandaAtrasada;
  iniciaisDe = iniciaisDe;

  readonly maxRostos = 4;

  apresentar(d: Demanda) {
    return separarCodigo(d.titulo, d.codigo);
  }

  rostos(d: Demanda) {
    return (d.participantes ?? []).slice(0, this.maxRostos);
  }

  restantes(d: Demanda): number {
    return Math.max(0, (d.participantes ?? []).length - this.maxRostos);
  }

  tecla(evento: Event, d: Demanda) {
    evento.preventDefault();
    this.abrir.emit(d);
  }
}
