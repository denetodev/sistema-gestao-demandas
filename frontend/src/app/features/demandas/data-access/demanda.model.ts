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
  createdAt: string;
  updatedAt: string;
  valorCalculado: number | null;
}

export interface CriarDemandaPayload {
  titulo: string;
  diretoriaId: string;
  prioridade: Prioridade;
  dataPrazo: string | null;
  valor: number | null;
  observacoes: string | null;
}

export interface DemandaFiltro {
  escopo: EscopoDemanda;
  page: number;
  size: number;
  sort?: string;
}
