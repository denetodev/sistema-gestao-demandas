import { Component, computed, effect, ElementRef, input, viewChild } from '@angular/core';
import type { DiaAtividade } from '../data-access/dashboard.model';

interface Celula {
  data: string;
  quantidade: number;
  nivel: 0 | 1 | 2 | 3 | 4;
  /** posição na grade: coluna = semana, linha = dia da semana (domingo = 0) */
  coluna: number;
  linha: number;
}

const MESES = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];

function iso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Atividade diária dos últimos 12 meses, no estilo do calendário do GitHub. */
@Component({
  selector: 'app-mapa-calor',
  templateUrl: './mapa-calor.html',
  styleUrl: './mapa-calor.scss',
})
export class MapaCalor {
  dias = input.required<DiaAtividade[]>();

  private mapa = viewChild<ElementRef<HTMLElement>>('mapa');

  constructor() {
    // em telas estreitas a grade rola; abre mostrando os meses mais recentes
    effect(() => {
      this.grade();
      const el = this.mapa()?.nativeElement;
      if (el) setTimeout(() => (el.scrollLeft = el.scrollWidth));
    });
  }

  grade = computed(() => {
    const porData = new Map(this.dias().map((d) => [d.data, d.quantidade]));
    const max = Math.max(1, ...porData.values());

    const fim = new Date();
    fim.setHours(12, 0, 0, 0);
    const inicio = new Date(fim);
    inicio.setDate(inicio.getDate() - 364);
    inicio.setDate(inicio.getDate() - inicio.getDay()); // alinha no domingo

    const celulas: Celula[] = [];
    const meses: { rotulo: string; coluna: number }[] = [];
    let coluna = 0;
    let mesAnterior = -1;
    let total = 0;

    for (const d = new Date(inicio); d <= fim; d.setDate(d.getDate() + 1)) {
      if (d.getDay() === 0 && celulas.length) coluna++;
      if (d.getDay() === 0 && d.getMonth() !== mesAnterior) {
        meses.push({ rotulo: MESES[d.getMonth()], coluna: coluna + 1 });
        mesAnterior = d.getMonth();
      }
      const data = iso(d);
      const quantidade = porData.get(data) ?? 0;
      total += quantidade;
      const q = quantidade / max;
      const nivel = quantidade === 0 ? 0 : q <= 0.25 ? 1 : q <= 0.5 ? 2 : q <= 0.75 ? 3 : 4;
      celulas.push({ data, quantidade, nivel, coluna: coluna + 1, linha: d.getDay() + 1 });
    }
    return { celulas, meses, total, colunas: coluna + 1 };
  });

  titulo(c: Celula): string {
    const [a, m, d] = c.data.split('-');
    return `${d}/${m}/${a}: ${c.quantidade} atividade(s)`;
  }
}
