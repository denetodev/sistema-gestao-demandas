import type { StatusDemanda } from '../../../features/demandas/data-access/demanda.model';

/** O que uma campanha reuniu. Demandas canceladas contam no total e no status, não em pessoas, valor, peças e atividades. */
export interface CampanhaResumo {
  id: string;
  nome: string;
  codigo: string | null;
  demandas: number;
  demandasAtivas: number;
  pessoas: number;
  valor: number;
  pecas: number;
  atividades: number;
  porStatus: Record<StatusDemanda, number>;
  primeiraDemanda: string | null;
  ultimaDemanda: string | null;
}

export interface PessoaNaCampanha {
  pessoaId: string;
  nome: string;
  demandas: number;
  atividades: number;
  pecas: number;
  valor: number;
}

export interface DemandaNaCampanha {
  id: string;
  titulo: string;
  status: StatusDemanda;
  dataCriacao: string | null;
  valor: number;
}

export interface CampanhaDetalhe {
  resumo: CampanhaResumo;
  pessoas: PessoaNaCampanha[];
  demandas: DemandaNaCampanha[];
}
