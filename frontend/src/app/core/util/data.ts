/** Converte "2026-09-13" (LocalDate do backend) em Date no fuso local. */
export function paraData(valor: string | null | undefined): Date | null {
  return valor ? new Date(valor + 'T00:00:00') : null;
}

/** Converte Date em "2026-09-13" para enviar ao backend. */
export function formatarData(data: Date | null | undefined): string | null {
  if (!data) return null;
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}