import { normalizar } from '../util/listas';
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

export type SituacaoFiltro = StatusEmprestimo | 'ATRASADO';

export interface FiltroEmprestimos {
  busca: string;
  situacao: SituacaoFiltro | null;
  /** true só externos; false só internos; null todos. */
  externo: boolean | null;
  /** Devolução prevista entre as duas datas (inclusive); qualquer ponta pode ficar vazia. */
  de: Date | null;
  ate: Date | null;
}

export const FILTRO_VAZIO: FiltroEmprestimos = { busca: '', situacao: null, externo: null, de: null, ate: null };

export function filtroAtivo(f: FiltroEmprestimos): boolean {
  return !!(f.busca.trim() || f.situacao || f.externo !== null || f.de || f.ate);
}

/** Busca por quem retirou (nome, chave, contato), equipamento (nome, patrimônio), local, finalidade e demanda. */
export function filtrarEmprestimos(lista: Emprestimo[], f: FiltroEmprestimos): Emprestimo[] {
  const procurado = normalizar(f.busca);
  const inicio = f.de ? new Date(f.de.getFullYear(), f.de.getMonth(), f.de.getDate()).getTime() : null;
  const fim = f.ate ? new Date(f.ate.getFullYear(), f.ate.getMonth(), f.ate.getDate(), 23, 59, 59, 999).getTime() : null;
  return lista.filter((e) => {
    if (procurado) {
      const campos = [
        e.retiranteNome, e.retiranteChaveC, e.retiranteContato, e.localUso, e.finalidade, e.demandaTitulo,
        ...e.itens.flatMap((i) => [i.equipamentoNome, i.patrimonio, i.categoriaNome]),
      ];
      if (!campos.some((c) => normalizar(c).includes(procurado))) return false;
    }
    if (f.situacao === 'ATRASADO' ? !e.atrasado : f.situacao && e.status !== f.situacao) return false;
    if (f.externo !== null && e.externo !== f.externo) return false;
    const prevista = new Date(e.previstaDevolucao).getTime();
    if (inicio !== null && prevista < inicio) return false;
    if (fim !== null && prevista > fim) return false;
    return true;
  });
}
