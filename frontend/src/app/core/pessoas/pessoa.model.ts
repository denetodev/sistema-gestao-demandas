import type { Perfil } from '../auth/auth.model';

export type StatusPessoa = 'ATIVO' | 'INATIVO' | 'AFASTADO' | 'REJEITADO';

export interface Pessoa {
  id: string;
  nome: string;
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
  fotoUrl: string | null;
  status: StatusPessoa;
  perfil: Perfil;
  authUserId: string | null;
  aprovadoPorId: string | null;
  aprovadoPorNome: string | null;
  aprovadoEm: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PessoaPayload {
  nome: string;
  email: string | null;
  areaId: string;
  cargoId: string | null;
  status: StatusPessoa | null;
  perfil: Perfil | null;
}

export interface AprovarPayload {
  perfil: Perfil;
  areaId: string | null;
  cargoId: string | null;
  referenciaAreaId: string | null;
}

export interface RejeitarPayload {
  motivo: string | null;
}