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
  previstaDevolucao: string;
  demandaId: string | null;
  atividadeId: string | null;
  itens: { equipamentoId: string; quantidade: number | null }[];
  termoAceito: boolean;
}

export interface Termo {
  versao: number;
  texto: string;
}
