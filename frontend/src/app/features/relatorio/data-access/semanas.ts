export interface Semana {
  numero: number;
  /** yyyy-MM-dd */
  de: string;
  ate: string;
}

const iso = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

const soFimDeSemana = (de: Date, ate: Date) => {
  for (const d = new Date(de); d <= ate; d.setDate(d.getDate() + 1)) {
    if (d.getDay() !== 0 && d.getDay() !== 6) return false;
  }
  return true;
};

/**
 * As semanas do relatório (Semana 1, 2...), de segunda a domingo e cortadas nas pontas do mês.
 * Mesma regra do backend (SemanasDoMes): um pedaço só de sábado e domingo se junta à semana vizinha.
 */
export function semanasDoMes(mes: string): Semana[] {
  const [ano, m] = mes.split('-').map(Number);
  const fimDoMes = new Date(ano, m, 0);
  const blocos: { de: Date; ate: Date }[] = [];
  let inicio = new Date(ano, m - 1, 1);
  while (inicio <= fimDoMes) {
    const domingo = new Date(inicio);
    domingo.setDate(domingo.getDate() + ((7 - domingo.getDay()) % 7));
    const fim = domingo > fimDoMes ? fimDoMes : domingo;
    blocos.push({ de: inicio, ate: fim });
    inicio = new Date(fim);
    inicio.setDate(inicio.getDate() + 1);
  }
  if (blocos.length > 1 && soFimDeSemana(blocos[0].de, blocos[0].ate)) {
    blocos[1].de = blocos[0].de;
    blocos.shift();
  }
  if (blocos.length > 1 && soFimDeSemana(blocos[blocos.length - 1].de, blocos[blocos.length - 1].ate)) {
    blocos[blocos.length - 2].ate = blocos[blocos.length - 1].ate;
    blocos.pop();
  }
  return blocos.map((b, i) => ({ numero: i + 1, de: iso(b.de), ate: iso(b.ate) }));
}

/** A semana que contém o dia (yyyy-MM-dd); fora do mês cai na última. */
export function semanaDaData(semanas: Semana[], dia: string): Semana {
  return semanas.find((s) => dia >= s.de && dia <= s.ate) ?? semanas[semanas.length - 1];
}
