/** O mínimo que o formulário precisa saber de uma campanha para filtrar. */
export interface CampanhaOpcao {
  id: string;
  projetoId: string | null;
}

/**
 * Campanhas oferecidas ao preencher uma demanda.
 *
 * A campanha é independente: pode existir sem projeto e uma demanda pode ter campanha sem ter projeto.
 * Sem projeto escolhido, aparecem todas. Com projeto escolhido, aparecem as dele e as que não
 * pertencem a nenhum projeto (as de outros projetos ficam de fora).
 */
export function filtrarCampanhas<T extends CampanhaOpcao>(campanhas: T[], projetoId: string | null): T[] {
  if (!projetoId) {
    return campanhas;
  }
  return campanhas.filter((c) => !c.projetoId || c.projetoId === projetoId);
}
