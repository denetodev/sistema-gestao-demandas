import type { Pessoa } from './pessoa.model';

export interface CandidatoVinculo {
  id: string;
  label: string;
}

/**
 * Cadastros que podem receber uma conta pendente: já aprovados, ativos e ainda sem conta de login
 * (por exemplo, pessoas importadas da planilha). Em ordem alfabética.
 */
export function candidatosAVinculo(pessoas: Pessoa[]): CandidatoVinculo[] {
  return pessoas
    .filter((p) => !p.authUserId && p.aprovadoEm !== null && p.status === 'ATIVO')
    .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
    .map((p) => ({ id: p.id, label: p.areaNome ? `${p.nome} · ${p.areaNome}` : p.nome }));
}
