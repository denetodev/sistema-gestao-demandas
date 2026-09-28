export interface TipoPeca {
  id: string;
  nome: string;
  descricao: string | null;
  areaId: string;
  areaNome: string;
  valorReferencia: number;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TipoPecaPayload {
  nome: string;
  descricao: string | null;
  areaId: string;
  valorReferencia: number;
}
