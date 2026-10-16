import type { BlocoAgenda } from './equipamento.model';

const DIA_MS = 24 * 60 * 60 * 1000;

export interface BlocoPosicionado {
  bloco: BlocoAgenda;
  /** Posição e largura em % da janela. */
  esquerda: number;
  largura: number;
  /** Faixa vermelha depois da devolução prevista: o equipamento segue com a pessoa até agora. */
  atrasoEsquerda: number;
  atrasoLargura: number;
  /** Linha dentro do equipamento: períodos que se sobrepõem (lote, ou devolvido e reserva) ficam empilhados. */
  faixa: number;
}

export interface Dia {
  data: Date;
  esquerda: number;
  largura: number;
  rotulo: string;
  diaSemana: string;
  fimDeSemana: boolean;
  hoje: boolean;
}

function pct(ms: number, de: number, total: number): number {
  return ((ms - de) / total) * 100;
}

/** Começo do dia local. */
export function inicioDoDia(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

export function somarDias(d: Date, dias: number): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() + dias, d.getHours(), d.getMinutes());
}

/** Um dia por coluna da janela, com a posição em % para desenhar a grade. */
export function dias(de: Date, quantidade: number, agora: Date): Dia[] {
  const inicioJanela = inicioDoDia(de);
  const fimJanela = somarDias(inicioJanela, quantidade);
  const total = fimJanela.getTime() - inicioJanela.getTime();
  const hoje = inicioDoDia(agora).getTime();
  const semana = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'];
  return Array.from({ length: quantidade }, (_, i) => {
    const data = somarDias(inicioJanela, i);
    const proximo = somarDias(inicioJanela, i + 1);
    return {
      data,
      esquerda: pct(data.getTime(), inicioJanela.getTime(), total),
      largura: pct(proximo.getTime(), data.getTime() + (inicioJanela.getTime() - data.getTime()), total) - pct(data.getTime(), inicioJanela.getTime(), total),
      rotulo: `${String(data.getDate()).padStart(2, '0')}/${String(data.getMonth() + 1).padStart(2, '0')}`,
      diaSemana: semana[data.getDay()],
      fimDeSemana: data.getDay() === 0 || data.getDay() === 6,
      hoje: data.getTime() === hoje,
    };
  });
}

/** Posição do "agora" em % da janela; nulo se estiver fora dela. */
export function posicaoAgora(de: Date, ate: Date, agora: Date): number | null {
  if (agora < de || agora > ate) return null;
  return pct(agora.getTime(), de.getTime(), ate.getTime() - de.getTime());
}

/**
 * Calcula onde cada período aparece na janela [de, ate] e em qual faixa, para os sobrepostos não ficarem
 * um em cima do outro. Períodos fora da janela são cortados; os que cruzam a borda são aparados.
 */
export function posicionar(blocos: BlocoAgenda[], de: Date, ate: Date, agora: Date): { itens: BlocoPosicionado[]; faixas: number } {
  const inicioJ = de.getTime();
  const total = ate.getTime() - inicioJ;
  const MINIMO = 0.7; // período curtinho continua clicável

  const ordenados = [...blocos].sort((a, b) => a.inicio.localeCompare(b.inicio));
  const fimDasFaixas: number[] = [];
  const itens: BlocoPosicionado[] = [];

  for (const bloco of ordenados) {
    const ini = new Date(bloco.inicio).getTime();
    const fimPrevisto = new Date(bloco.fim).getTime();
    const fimEfetivo = bloco.atrasado ? Math.max(fimPrevisto, agora.getTime()) : fimPrevisto;

    const visIni = Math.max(ini, inicioJ);
    const visFim = Math.min(fimEfetivo, ate.getTime());
    if (visFim <= inicioJ || visIni >= ate.getTime()) continue;

    // normal: até a devolução prevista; atraso: da devolução prevista até agora
    const normalFim = Math.min(fimPrevisto, ate.getTime());
    const largura = Math.max(MINIMO, pct(normalFim, visIni, total));
    const temAtraso = bloco.atrasado && fimEfetivo > fimPrevisto;
    const atrasoIni = Math.max(fimPrevisto, inicioJ);

    let faixa = fimDasFaixas.findIndex((f) => f <= ini);
    if (faixa < 0) {
      faixa = fimDasFaixas.length;
      fimDasFaixas.push(0);
    }
    fimDasFaixas[faixa] = fimEfetivo;

    itens.push({
      bloco,
      esquerda: pct(visIni, inicioJ, total),
      largura: normalFim > visIni ? largura : 0,
      atrasoEsquerda: temAtraso ? pct(atrasoIni, inicioJ, total) : 0,
      atrasoLargura: temAtraso ? Math.max(MINIMO, pct(visFim, atrasoIni, total)) : 0,
      faixa,
    });
  }
  return { itens, faixas: Math.max(1, fimDasFaixas.length) };
}

export const DIA = DIA_MS;
