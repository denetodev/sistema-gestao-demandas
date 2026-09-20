export type Perfil = 'ADMIN' | 'GESTOR' | 'PROFISSIONAL' | 'VISUALIZADOR';

export interface PessoaMe {
  id: string;
  nome: string;
  email: string | null;
  perfil: Perfil;
  areaId: string | null;
  areaNome: string | null;
  diretoriaId: string | null;
  diretoriaNome: string | null;
  status: string;
  authUserId: string;
}

export interface PessoaMeResponse {
  vinculado: boolean;
  pessoa: PessoaMe | null;
}
