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
