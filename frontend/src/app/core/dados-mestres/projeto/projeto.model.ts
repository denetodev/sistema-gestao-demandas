export type StatusProjeto = 'ATIVO' | 'PAUSADO' | 'ENCERRADO';

export interface Projeto {
  id: string;
  nome: string;
  demandanteId: string | null;
  demandanteNome: string | null;
  diretoriaId: string | null;
  diretoriaNome: string | null;
  descricao: string | null;
  status: StatusProjeto;
  createdAt: string;
  updatedAt: string;
}

export interface ProjetoPayload {
  nome: string;
  demandanteId: string | null;
  diretoriaId: string | null;
  descricao: string | null;
  status: StatusProjeto | null;
}
