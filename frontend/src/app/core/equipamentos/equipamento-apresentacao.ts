import type { Emprestimo, PapelEquipamentos, StatusEmprestimo } from './equipamento.model';

type Severidade = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

export const ROTULO_PAPEL: Record<PapelEquipamentos, string> = {
  OPERADOR_DE_ESTUDIO: 'Operador de estúdio',
  GESTOR_DE_EQUIPAMENTOS: 'Gestor de equipamentos',
};

const ROTULO_STATUS: Record<StatusEmprestimo, string> = {
  AGUARDANDO_AUTORIZACAO: 'Aguardando autorização',
  AUTORIZADO: 'Autorizado: pronto para entregar',
  RECUSADO: 'Recusado',
  RETIRADO: 'Com a pessoa',
  DEVOLVIDO: 'Devolvido',
  CANCELADO: 'Cancelado',
};

const SEVERIDADE_STATUS: Record<StatusEmprestimo, Severidade> = {
  AGUARDANDO_AUTORIZACAO: 'warn',
  AUTORIZADO: 'info',
  RECUSADO: 'secondary',
  RETIRADO: 'success',
  DEVOLVIDO: 'secondary',
  CANCELADO: 'secondary',
};

/** Atrasado ganha vez sobre a situação: é o que o gestor precisa enxergar primeiro. */
export function rotuloSituacao(e: Pick<Emprestimo, 'status' | 'atrasado'>): string {
  return e.atrasado ? 'Atrasado' : ROTULO_STATUS[e.status];
}

export function severidadeSituacao(e: Pick<Emprestimo, 'status' | 'atrasado'>): Severidade {
  return e.atrasado ? 'danger' : SEVERIDADE_STATUS[e.status];
}

/** Empréstimo externo que ainda espera decisão: vai em destaque para quem autoriza. */
export function pedeAtencao(e: Pick<Emprestimo, 'status' | 'externo' | 'atrasado'>): boolean {
  return e.atrasado || (e.externo && e.status === 'AGUARDANDO_AUTORIZACAO');
}

/** Resumo dos itens para a lista: "Câmera A7, Cabo HDMI (x3)". */
export function resumoItens(e: Pick<Emprestimo, 'itens'>): string {
  return e.itens.map((i) => (i.quantidade > 1 ? `${i.equipamentoNome} (x${i.quantidade})` : i.equipamentoNome)).join(', ');
}
