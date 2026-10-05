import type { StatusDemanda } from '../../demandas/data-access/demanda.model';

export type VisaoDashboard = 'MINHA' | 'EQUIPE' | 'DIRETORIA';

export interface DashboardResumo {
  pessoas: number;
  demandasPorStatus: Record<StatusDemanda, number>;
  demandasAtrasadas: number;
  atividadesNoMes: number;
  valorNoMes: number;
  valorNoAno: number;
}

export interface DiaAtividade {
  data: string;
  quantidade: number;
}

export interface LinhaArea {
  areaId: string;
  area: string;
  pessoas: number;
  demandasAtivas: number;
  atividadesNoMes: number;
  demandasAtrasadas: number;
  valorNoMes: number;
}

export interface LinhaPessoa {
  pessoaId: string;
  nome: string;
  area: string | null;
  diretoria: string | null;
  demandasAtivas: number;
  atividadesNoMes: number;
  valorNoMes: number;
}

export interface Dashboard {
  visao: VisaoDashboard;
  titulo: string;
  mes: string;
  resumo: DashboardResumo;
  calendario: DiaAtividade[];
  porArea: LinhaArea[];
  porPessoa: LinhaPessoa[];
}

export interface DashboardFiltro {
  pessoaId: string | null;
  diretoriaId: string | null;
  porPessoa: boolean;
  /** yyyy-MM; null = mês atual */
  mes: string | null;
}
