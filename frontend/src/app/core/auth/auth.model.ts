export type Perfil = 'ADMIN' | 'GESTOR' | 'PROFISSIONAL' | 'VISUALIZADOR';

export interface PessoaMe {
  id: string;
  nome: string;
  /** Como a pessoa quer ser chamada; nulo = usa o nome completo. */
  nomeExibicao: string | null;
  email: string | null;
  diretoriaId: string | null;
  diretoriaNome: string | null;
  areaId: string | null;
  areaNome: string | null;
  cargoId: string | null;
  cargoNome: string | null;
  referenciaAreaId: string | null;
  referenciaAreaNome: string | null;
  status: string;
  perfil: Perfil;
  authUserId: string;
  aprovadoPorId: string | null;
  aprovadoPorNome: string | null;
  aprovadoEm: string | null;
  createdAt: string;
  updatedAt: string;
  fotoUrl: string | null;
}

export interface PessoaMeResponse {
  vinculado: boolean;
  pessoa: PessoaMe | null;
}
