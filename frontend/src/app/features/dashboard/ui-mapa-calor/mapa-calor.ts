import { Component, computed, effect, ElementRef, input, signal, viewChild } from '@angular/core';
import type { DiaAtividade } from '../data-access/dashboard.model';

interface Celula {
  data: string;
  quantidade: number;
  nivel: 0 | 1 | 2 | 3 | 4;
  /** posição na grade: coluna = semana, linha = dia da semana (domingo = 0) */
  coluna: number;
  linha: number;
  diaSemana: number;
}

const MESES = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
const DIAS = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado'];
const DIAS_CURTOS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

function iso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function formatar(data: string): string {
  const [a, m, d] = data.split('-');
  return `${d}/${m}/${a}`;
}

function plural(n: number, singular: string, plural: string): string {
  return `${n} ${n === 1 ? singular : plural}`;
}

/**
 * Atividade diária dos últimos 12 meses: um quadrado por dia, mais escuro quando houve mais atividades.
 * Ao lado, os números que ajudam a ler o calendário (melhor dia, sequência, ritmo da semana).
 */
@Component({
  selector: 'app-mapa-calor',
  templateUrl: './mapa-calor.html',
  styleUrl: './mapa-calor.scss',
})
export class MapaCalor {
  dias = input.required<DiaAtividade[]>();

  private mapa = viewChild<ElementRef<HTMLElement>>('mapa');

  /** O quadrado sob o mouse ou o foco: a leitura acima do calendário mostra o dia dele. */
  ativa = signal<Celula | null>(null);

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
      celulas.push({ data, quantidade, nivel, coluna: coluna + 1, linha: d.getDay() + 1, diaSemana: d.getDay() });
    }
    const colunas = coluna + 1;
    // rótulo de mês só entra se houver espaço até o próximo e até o fim da grade (senão vaza para fora)
    const mesesVisiveis = meses.filter((m, i) => {
      const proximo = meses[i + 1]?.coluna ?? colunas + 1;
      return proximo - m.coluna >= 3;
    });
    return { celulas, meses: mesesVisiveis, total, colunas };
  });

  /** Os números ao lado do calendário. */
  insights = computed(() => {
    const { celulas, total } = this.grade();
    const ativos = celulas.filter((c) => c.quantidade > 0);
    const melhor = ativos.reduce<Celula | null>((m, c) => (!m || c.quantidade > m.quantidade ? c : m), null);

    // sequência de dias úteis seguidos com atividade: fim de semana vazio não quebra a sequência
    const util = celulas.filter((c) => c.quantidade > 0 || (c.diaSemana !== 0 && c.diaSemana !== 6));
    let maior = 0;
    let corrente = 0;
    for (const c of util) {
      corrente = c.quantidade > 0 ? corrente + 1 : 0;
      maior = Math.max(maior, corrente);
    }
    // hoje ainda sem registro não zera a sequência de ontem
    let atual = 0;
    for (let i = util.length - 1; i >= 0; i--) {
      if (util[i].quantidade > 0) atual++;
      else if (!(i === util.length - 1 && atual === 0)) break;
    }

    const porMes = new Map<string, number>();
    for (const c of ativos) porMes.set(c.data.slice(0, 7), (porMes.get(c.data.slice(0, 7)) ?? 0) + c.quantidade);
    let mesTop: string | null = null;
    let mesTopQtd = 0;
    for (const [mes, qtd] of porMes) {
      if (qtd > mesTopQtd) {
        mesTop = mes;
        mesTopQtd = qtd;
      }
    }

    const porDiaSemana = [0, 0, 0, 0, 0, 0, 0];
    for (const c of ativos) porDiaSemana[c.diaSemana] += c.quantidade;
    const maxSemana = Math.max(1, ...porDiaSemana);
    const semana = [1, 2, 3, 4, 5, 6, 0].map((d) => ({
      rotulo: DIAS_CURTOS[d],
      completo: DIAS[d],
      quantidade: porDiaSemana[d],
      percentual: Math.round((porDiaSemana[d] / maxSemana) * 100),
      topo: porDiaSemana[d] === maxSemana && maxSemana > 1,
    }));

    return {
      total,
      diasAtivos: ativos.length,
      mediaPorDiaAtivo: ativos.length ? Math.round((total / ativos.length) * 10) / 10 : 0,
      melhor: melhor ? { data: formatar(melhor.data), quantidade: melhor.quantidade } : null,
      sequenciaAtual: atual,
      maiorSequencia: maior,
      mesTop: mesTop ? `${MESES[Number(mesTop.slice(5, 7)) - 1]}/${mesTop.slice(0, 4)}` : null,
      mesTopQtd,
      semana,
    };
  });

  /** Soma de atividades em cada um dos últimos 12 meses (o mês corrente é parcial). */
  porMes = computed(() => {
    const soma = new Map<string, number>();
    for (const c of this.grade().celulas) soma.set(c.data.slice(0, 7), (soma.get(c.data.slice(0, 7)) ?? 0) + c.quantidade);
    const chaves = [...soma.keys()].sort().slice(-12);
    const max = Math.max(1, ...chaves.map((k) => soma.get(k) ?? 0));
    const corrente = iso(new Date()).slice(0, 7);
    return chaves.map((k) => ({
      rotulo: MESES[Number(k.slice(5, 7)) - 1],
      completo: `${MESES[Number(k.slice(5, 7)) - 1]}/${k.slice(0, 4)}`,
      quantidade: soma.get(k) ?? 0,
      percentual: Math.round(((soma.get(k) ?? 0) / max) * 100),
      parcial: k === corrente,
    }));
  });

  /** A frase acima do calendário: o dia sob o mouse ou, sem ele, um convite para passar o mouse. */
  leitura = computed(() => {
    const c = this.ativa();
    if (!c) return 'Passe o mouse sobre um quadrado para ver o dia.';
    const dia = `${DIAS[c.diaSemana]}, ${formatar(c.data)}`;
    return c.quantidade === 0 ? `${dia}: nenhuma atividade` : `${dia}: ${plural(c.quantidade, 'atividade', 'atividades')}`;
  });

  titulo(c: Celula): string {
    return `${formatar(c.data)}: ${plural(c.quantidade, 'atividade', 'atividades')}`;
  }
}
