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

export type ModeloRelatorio = 'DESIGNER' | 'HTML' | 'PADRAO';

export type TipoEvidencia =
  | 'IMAGEM' | 'SCREENSHOT' | 'HTML' | 'PDF' | 'VIDEO' | 'ARQUIVO' | 'LINK' | 'CODIGO'
  | 'MOCKUP' | 'CAPTURA_SISTEMA' | 'OBSERVACAO' | 'CONFIRMACAO_MANUAL';

export interface ItemEvidencia {
  id: string;
  atividadeId: string | null;
  pecaId: string | null;
  tipo: TipoEvidencia;
  descricao: string | null;
  link: string | null;
  temArquivo: boolean;
  arquivoMime: string | null;
}

export interface ResumoPeca {
  tipo: string;
  quantidade: number;
}

export interface ItemDemanda {
  id: string;
  codigo: string | null;
  titulo: string;
  descricao: string | null;
  link: string | null;
  demandante: string | null;
  diretoria: string | null;
  campanha: string | null;
  data: string | null;
  atividadeIds: string[];
  quantidadePecas: number;
  valor: number;
  pecas: ResumoPeca[];
  naturezas: string[];
  evidencias: ItemEvidencia[];
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
  /** Ausente nos relatórios aprovados antes dos modelos por função. */
  modelo?: ModeloRelatorio | null;
  demandas?: ItemDemanda[] | null;
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
