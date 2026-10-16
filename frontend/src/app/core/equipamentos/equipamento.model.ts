export type PapelEquipamentos = 'OPERADOR_DE_ESTUDIO' | 'GESTOR_DE_EQUIPAMENTOS';
export type ControleEquipamento = 'INDIVIDUAL' | 'QUANTIDADE';
export type StatusEmprestimo =
  | 'AGUARDANDO_AUTORIZACAO'
  | 'AUTORIZADO'
  | 'RECUSADO'
  | 'RETIRADO'
  | 'DEVOLVIDO'
  | 'CANCELADO';
export type GrupoEmprestimos = 'ATIVOS' | 'HISTORICO';

export interface CategoriaEquipamento {
  id: string;
  nome: string;
  controle: ControleEquipamento;
  ativo: boolean;
}

export interface UsoAtual {
  emprestimoId: string;
  retiranteNome: string;
  quantidade: number;
  retiradaPrevista: string;
  previstaDevolucao: string;
  status: StatusEmprestimo;
  atrasado: boolean;
  externo: boolean;
  localUso: string | null;
}

export interface Equipamento {
  id: string;
  categoriaId: string;
  categoriaNome: string;
  controle: ControleEquipamento;
  nome: string;
  patrimonio: string | null;
  numeroSerie: string | null;
  quantidadeTotal: number;
  quantidadeDisponivel: number;
  localizacao: string | null;
  observacoes: string | null;
  ativo: boolean;
  emUso: UsoAtual[];
}

export interface EquipamentoPayload {
  categoriaId: string;
  nome: string;
  patrimonio: string | null;
  numeroSerie: string | null;
  quantidadeTotal: number | null;
  localizacao: string | null;
  observacoes: string | null;
}

export interface ItemEmprestimo {
  id: string;
  equipamentoId: string;
  equipamentoNome: string;
  patrimonio: string | null;
  categoriaNome: string;
  quantidade: number;
  devolvidoEm: string | null;
}

export interface Emprestimo {
  id: string;
  status: StatusEmprestimo;
  atrasado: boolean;
  externo: boolean;
  localUso: string | null;
  retiranteNome: string;
  retiranteChaveC: string | null;
  retiranteContato: string | null;
  retirantePessoaId: string | null;
  finalidade: string | null;
  retiradaPrevista: string;
  previstaDevolucao: string;
  demandaId: string | null;
  demandaTitulo: string | null;
  atividadeId: string | null;
  registradoPorId: string;
  registradoPorNome: string;
  termoVersao: number;
  termoAceitoEm: string;
  autorizadoPorNome: string | null;
  autorizadoEm: string | null;
  motivoRecusa: string | null;
  retiradoPorNome: string | null;
  retiradoEm: string | null;
  devolvidoPorNome: string | null;
  devolvidoEm: string | null;
  observacoesDevolucao: string | null;
  itens: ItemEmprestimo[];
  createdAt: string;
}

export interface EmprestimoPayload {
  retiranteNome: string;
  retiranteChaveC: string | null;
  retiranteContato: string | null;
  retirantePessoaId: string | null;
  finalidade: string | null;
  externo: boolean;
  localUso: string | null;
  /** Nulo significa retirar agora; uma data futura reserva o equipamento. */
  retiradaPrevista: string | null;
  previstaDevolucao: string;
  demandaId: string | null;
  atividadeId: string | null;
  itens: { equipamentoId: string; quantidade: number | null }[];
  termoAceito: boolean;
}

export type MomentoFoto = 'SAIDA' | 'DEVOLUCAO';

/** url é assinada e vence em poucos minutos (bucket privado): recarregue a lista quando precisar. */
export interface FotoEmprestimo {
  id: string;
  momento: MomentoFoto;
  url: string;
  validadeSegundos: number;
  tamanho: number;
  enviadaPorNome: string;
  createdAt: string;
}

export interface Disponibilidade {
  equipamentoId: string;
  /** Quanto está livre durante TODO o período consultado (o pior momento manda). */
  quantidadeDisponivel: number;
}

export interface BlocoAgenda {
  emprestimoId: string;
  retiranteNome: string;
  status: StatusEmprestimo;
  devolvido: boolean;
  atrasado: boolean;
  externo: boolean;
  localUso: string | null;
  quantidade: number;
  inicio: string;
  /** Devolução prevista (ou a real, se já devolveu). Atrasado segue com a pessoa até "agora". */
  fim: string;
}

export interface LinhaAgenda {
  equipamentoId: string;
  nome: string;
  patrimonio: string | null;
  categoriaId: string;
  categoriaNome: string;
  controle: ControleEquipamento;
  quantidadeTotal: number;
  blocos: BlocoAgenda[];
}

export interface Agenda {
  de: string;
  ate: string;
  agora: string;
  equipamentos: LinhaAgenda[];
}

export interface ResumoEmprestimos {
  aguardandoAutorizacao: number;
  aguardandoExterno: number;
  atrasados: number;
}

export interface Termo {
  versao: number;
  texto: string;
}
