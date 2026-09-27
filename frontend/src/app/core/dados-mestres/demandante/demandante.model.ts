export type TipoDemandante = 'PESSOA' | 'AREA' | 'UNIDADE' | 'EXTERNO';

export interface Demandante {
  id: string;
  nome: string;
  tipo: TipoDemandante;
  observacao: string | null;
  diretoriaId: string | null;
  diretoriaNome: string | null;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface DemandantePayload {
  nome: string;
  tipo: TipoDemandante;
  observacao: string | null;
  diretoriaId: string | null;
}
