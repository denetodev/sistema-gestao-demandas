/** Texto sem acento, em minúsculas e sem espaços nas pontas: base para buscar sem se importar com a grafia. */
export function normalizar(texto: string | null | undefined): string {
  return (texto ?? '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
}

/**
 * Algum dos campos contém o texto buscado (sem diferenciar maiúsculas nem acentos).
 * Busca vazia deixa tudo passar.
 */
export function contemTexto(campos: (string | null | undefined)[], busca: string): boolean {
  const procurado = normalizar(busca);
  if (!procurado) return true;
  return campos.some((campo) => normalizar(campo).includes(procurado));
}

/** As últimas cadastradas primeiro (a lista que a pessoa vê ao abrir a tela). */
export function maisRecentesPrimeiro<T extends { createdAt: string }>(itens: T[]): T[] {
  return [...itens].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

/** Ordem alfabética em português (ignora acento e caixa; números em ordem natural), sem alterar a lista original. */
export function emOrdemAlfabetica<T>(itens: readonly T[], rotulo: (item: T) => string | null | undefined): T[] {
  const colador = new Intl.Collator('pt-BR', { sensitivity: 'base', numeric: true });
  return [...itens].sort((a, b) => colador.compare(rotulo(a) ?? '', rotulo(b) ?? ''));
}

/** Para `httpResource(..., { parse })`: entrega a lista já em ordem alfabética, e todo seletor que a usa herda a ordem. */
export function alfabetica<T>(rotulo: (item: T) => string | null | undefined) {
  return (bruto: unknown): T[] => emOrdemAlfabetica(bruto as T[], rotulo);
}
