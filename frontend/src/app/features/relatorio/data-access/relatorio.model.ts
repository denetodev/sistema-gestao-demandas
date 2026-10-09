export type StatusRelatorio = 'ABERTO' | 'APROVADO';

export interface ItemAtividade {
  id: string;
  data: string;
  demandaId: string | null;
  demandaTitulo: string | null;
  tipo: string;
  descricao: string | null;
  incluida: boolean;
}

export interface ItemPeca {
  id: string;
  demandaTitulo: string;
  nome: string;
  tipo: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
}

export interface Relatorio {
  pessoaId: string;
  pessoaNome: string;
  diretoria: string | null;
  area: string | null;
  cargo: string | null;
  /** yyyy-MM */
  mes: string;
  status: StatusRelatorio;
  aprovadoEm: string | null;
  observacoes: string | null;
  atividades: ItemAtividade[];
  pecas: ItemPeca[];
  valorReferencia: number;
  podeEditar: boolean;
  podeReabrir: boolean;
}

export interface SalvarRelatorioPayload {
  observacoes: string | null;
  atividadesExcluidas: string[];
}

export type SituacaoRelatorio = 'NAO_INICIADO' | StatusRelatorio;

export interface RelatorioResumo {
  pessoaId: string;
  nome: string;
  area: string | null;
  situacao: SituacaoRelatorio;
  atividadesNoMes: number;
  aprovadoEm: string | null;
}

export interface RelatorioFiltro {
  mes: string;
  /** null = o relatório de quem está logado */
  pessoaId: string | null;
}
