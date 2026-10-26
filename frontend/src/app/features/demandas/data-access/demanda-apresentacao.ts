/**
 * Como mostrar o título de uma demanda na lista: o código (BC2607CQ) vira um chip à parte quando o
 * título começa com ele, para o título ficar limpo. Se o código não está no começo, o título não muda.
 */
export function separarCodigo(titulo: string, codigo: string | null): { codigo: string | null; titulo: string } {
  const cod = codigo?.trim();
  if (!cod) return { codigo: null, titulo };
  if (!titulo.toLowerCase().startsWith(cod.toLowerCase())) return { codigo: cod, titulo };
  const resto = titulo.slice(cod.length).replace(/^[\s\-–—_:]+/, '').trim();
  return { codigo: cod, titulo: resto || titulo };
}

/** Prazo vencido numa demanda que ainda está em aberto. */
export function demandaAtrasada(d: { dataPrazo: string | null; status: string }): boolean {
  if (!d.dataPrazo || d.status === 'CONCLUIDA' || d.status === 'CANCELADA') return false;
  return new Date(d.dataPrazo + 'T23:59:59') < new Date();
}

export function severityStatus(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
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

export function severityPrioridade(prioridade: string): 'danger' | 'warn' | 'info' | 'secondary' {
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

export function iniciaisDe(nome: string): string {
  const partes = nome.trim().split(/\s+/);
  return ((partes[0]?.[0] ?? '?') + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase();
}
