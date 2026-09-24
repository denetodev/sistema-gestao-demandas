export type StatusProjeto = 'ATIVO' | 'PAUSADO' | 'ENCERRADO';

export interface Projeto {
  id: string;
  nome: string;
  clienteId: string | null;
  clienteNome: string | null;
  diretoriaId: string | null;
  diretoriaNome: string | null;
  descricao: string | null;
  status: StatusProjeto;
  createdAt: string;
  updatedAt: string;
}

export interface ProjetoPayload {
  nome: string;
  clienteId: string | null;
  diretoriaId: string | null;
  descricao: string | null;
  status: StatusProjeto | null;
}