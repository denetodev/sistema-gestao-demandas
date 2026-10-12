import type { Perfil } from './auth.model';

export interface PessoaParaItem {
  id: string;
  perfil: Perfil;
  referenciaAreaId: string | null;
}

/**
 * Atividade ou peça de alguém: quem pode mexer. O dono, Gestor, Admin ou quem é Referência de equipe
 * (o backend confere se o item é da área dela). Visualizador nunca.
 * Só decide se o botão aparece; a barreira de verdade é o backend.
 */
export function podeMexerNoItem(pessoa: PessoaParaItem | null, pessoaIdDoItem: string | null): boolean {
  if (!pessoa || pessoa.perfil === 'VISUALIZADOR') return false;
  if (pessoa.perfil === 'ADMIN' || pessoa.perfil === 'GESTOR' || !!pessoa.referenciaAreaId) return true;
  return !!pessoaIdDoItem && pessoaIdDoItem === pessoa.id;
}
