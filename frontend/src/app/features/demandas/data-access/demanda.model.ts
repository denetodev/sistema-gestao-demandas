export type StatusDemanda =
  | 'NAO_INICIADA'
  | 'EM_ANDAMENTO'
  | 'EM_APROVACAO'
  | 'CONCLUIDA'
  | 'CANCELADA';
export type Prioridade = 'BAIXA' | 'NORMAL' | 'ALTA' | 'URGENTE';
export type EscopoDemanda = 'MINHAS' | 'EQUIPE' | 'NUCLEO' | 'DIRETORIA' | 'TODAS';

export interface ParticipanteResumo {
  /** Equipe e núcleo da pessoa, para agrupar o quadro; ausentes nas respostas antigas e no mock. */
  areaId?: string | null;
  areaNome?: string | null;
  diretoriaNome?: string | null;
  nucleoId?: string | null;
  nucleoNome?: string | null;
  pessoaId: string;
  nome: string;
  fotoUrl: string | null;
}

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
  /** Quem está no time agora; o Responsável principal vem primeiro. */
  participantes: ParticipanteResumo[];
  valorCalculado: number | null;
}

export interface CriarDemandaPayload {
  titulo: string;
  codigo: string | null;
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

/** Rótulos para mostrar na tela (o valor guardado continua em maiúsculas, como a API usa). */
export const ROTULO_PRIORIDADE: Record<Prioridade, string> = {
  BAIXA: 'Baixa',
  NORMAL: 'Normal',
  ALTA: 'Alta',
  URGENTE: 'Urgente',
};

/** Filtros da lista de demandas. Texto vazio significa "sem este filtro". */
export interface DemandaFiltro {
  escopo: EscopoDemanda;
  busca: string;
  status: StatusDemanda | '';
  /** Diretoria da demanda. */
  diretoriaId: string;
  /** Núcleo (alguém ativo de uma equipe desse núcleo está no time). */
  nucleoId: string;
  /** Equipe (a área de alguém ativo do time). */
  areaId: string;
  page: number;
  size: number;
  sort?: string;
}

export const FILTRO_PADRAO: DemandaFiltro = {
  escopo: 'MINHAS',
  busca: '',
  status: '',
  diretoriaId: '',
  nucleoId: '',
  areaId: '',
  page: 0,
  size: 10,
};
