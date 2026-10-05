export type PapelParticipante =
  | 'RESPONSAVEL_PRINCIPAL'
  | 'RESPONSAVEL'
  | 'PARTICIPANTE'
  | 'APOIO'
  | 'REVISOR';

export type TipoEvidencia =
  | 'IMAGEM'
  | 'SCREENSHOT'
  | 'HTML'
  | 'PDF'
  | 'VIDEO'
  | 'ARQUIVO'
  | 'LINK'
  | 'CODIGO'
  | 'MOCKUP'
  | 'CAPTURA_SISTEMA'
  | 'OBSERVACAO'
  | 'CONFIRMACAO_MANUAL';

export interface Participante {
  id: string;
  pessoaId: string;
  pessoaNome: string;
  papel: PapelParticipante;
  dataEntrada: string | null;
  dataSaida: string | null;
  observacao: string | null;
}

export interface ParticipantePayload {
  pessoaId: string;
  papel: PapelParticipante | null;
}

export interface Peca {
  id: string;
  demandaId: string;
  demandaTitulo: string;
  tipoPecaId: string;
  tipoPecaNome: string;
  nome: string;
  descricao: string | null;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
  pessoaId: string | null;
  pessoaNome: string | null;
  dataEntrega: string | null;
}

export interface PecaPayload {
  demandaId: string;
  tipoPecaId: string;
  nome: string;
  descricao: string | null;
  quantidade: number | null;
  pessoaId: string | null;
  dataEntrega: string | null;
}

export interface Atividade {
  id: string;
  demandaId: string;
  tipoAtividadeId: string;
  tipoAtividadeNome: string;
  pessoaId: string | null;
  pessoaNome: string | null;
  descricao: string | null;
  dataRealizacao: string | null;
}

export interface AtividadePayload {
  demandaId: string;
  tipoAtividadeId: string;
  pessoaId: string | null;
  descricao: string | null;
  dataRealizacao: string | null;
}

export interface Evidencia {
  id: string;
  atividadeId: string | null;
  pecaId: string | null;
  tipo: TipoEvidencia;
  conteudo: string | null;
  descricao: string | null;
  /** Preenchidos só em imagem enviada. */
  arquivoMime: string | null;
  arquivoTamanho: number | null;
  createdAt: string;
}

export interface ArquivoUrl {
  url: string;
  validadeSegundos: number;
}

export interface EvidenciaPayload {
  atividadeId: string | null;
  pecaId: string | null;
  tipo: TipoEvidencia;
  conteudo: string | null;
  descricao: string | null;
}

export interface HistoricoStatus {
  data: string;
  de: string | null;
  para: string | null;
  porId: string | null;
  porNome: string | null;
  motivo: string | null;
}

export const ROTULO_PAPEL: Record<PapelParticipante, string> = {
  RESPONSAVEL_PRINCIPAL: 'Responsável principal',
  RESPONSAVEL: 'Responsável',
  PARTICIPANTE: 'Participante',
  APOIO: 'Apoio',
  REVISOR: 'Revisor',
};

export const ROTULO_STATUS = {
  NAO_INICIADA: 'Não iniciada',
  EM_ANDAMENTO: 'Em andamento',
  EM_APROVACAO: 'Em aprovação',
  CONCLUIDA: 'Concluída',
  CANCELADA: 'Cancelada',
} as const;
