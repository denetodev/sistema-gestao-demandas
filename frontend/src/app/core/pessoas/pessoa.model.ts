import type { Perfil } from '../auth/auth.model';
import type { PapelEquipamentos } from '../equipamentos/equipamento.model';

export type StatusPessoa = 'ATIVO' | 'INATIVO' | 'AFASTADO' | 'REJEITADO';

export interface Pessoa {
  id: string;
  nome: string;
  nomeExibicao: string | null;
  /** Como a pessoa quer ser chamada (ou o nome completo, se não escolheu): é o que aparece para os outros. */
  nomeExibido: string;
  /** Chave C (matrícula), ex.: C1349864. */
  chaveC: string | null;
  email: string | null;
  diretoriaId: string | null;
  diretoriaNome: string | null;
  nucleoId: string | null;
  nucleoNome: string | null;
  areaId: string | null;
  areaNome: string | null;
  cargoId: string | null;
  cargoNome: string | null;
  referenciaAreaId: string | null;
  referenciaAreaNome: string | null;
  fotoUrl: string | null;
  /** Papel no módulo de equipamentos (nulo = sem acesso ao módulo, para Profissional). */
  papelEquipamentos?: PapelEquipamentos | null;
  status: StatusPessoa;
  perfil: Perfil;
  authUserId: string | null;
  aprovadoPorId: string | null;
  aprovadoPorNome: string | null;
  aprovadoEm: string | null;
  createdAt: string;
  updatedAt: string;
  /** A conta pediu para assumir um cadastro importado pela chave C (o resumo vem de /vinculo-sugerido). */
  temVinculoSugerido: boolean;
}

/** Cadastro importado que uma conta nova pediu para assumir. Só Admin e Gestor recebem. */
export interface VinculoSugerido {
  pessoaId: string;
  nome: string;
  nomeExibicao: string | null;
  equipe: string | null;
  cargo: string | null;
  chaveC: string | null;
  status: StatusPessoa;
  jaTemConta: boolean;
  demandas: number;
  atividades: number;
  pecas: number;
  nomeInformado: string;
  emailConta: string | null;
  /** Motivo de não poder confirmar agora; nulo quando pode. */
  impedimento: string | null;
}

export interface PessoaPayload {
  nome: string;
  email: string | null;
  /** Vazio mantém o atual. */
  chaveC: string | null;
  areaId: string;
  cargoId: string | null;
  status: StatusPessoa | null;
  perfil: Perfil | null;
  /** Referência de equipe dessa área; nulo remove. */
  referenciaAreaId: string | null;
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