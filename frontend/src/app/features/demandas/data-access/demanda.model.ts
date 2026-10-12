export type StatusDemanda =
  | 'NAO_INICIADA'
  | 'EM_ANDAMENTO'
  | 'EM_APROVACAO'
  | 'CONCLUIDA'
  | 'CANCELADA';
export type Prioridade = 'BAIXA' | 'NORMAL' | 'ALTA' | 'URGENTE';
export type EscopoDemanda = 'MINHAS' | 'EQUIPE' | 'DIRETORIA' | 'TODAS';

export interface Demanda {
  id: string;
  titulo: string;
  descricao: string | null;
  codigo: string | null;
  diretoriaId: string;
  diretoriaNome: string;
  demandanteId: string | null;
  demandanteNome: string | null;
  projetoId: string | null;
  projetoNome: string | null;
  campanhaId: string | null;
  campanhaNome: string | null;
  prioridade: Prioridade;
  status: StatusDemanda;
  dataCriacao: string;
  dataPrazo: string | null;
  dataEntregaReal: string | null;
  valor: number | null;
  observacoes: string | null;
  linkExterno: string | null;
  createdAt: string;
  updatedAt: string;
  /** A pessoa logada pode alterar esta demanda (participa, Referência da área do time, Gestor ou Admin). */
  podeAlterar: boolean;
  valorCalculado: number | null;
}

export interface CriarDemandaPayload {
  titulo: string;
  descricao: string | null;
  diretoriaId: string;
  demandanteId: string | null;
  projetoId: string | null;
  campanhaId: string | null;
  prioridade: Prioridade;
  dataPrazo: string | null;
  observacoes: string | null;
  linkExterno: string | null;
}

export interface DemandaFiltro {
  escopo: EscopoDemanda;
  page: number;
  size: number;
  sort?: string;
}
