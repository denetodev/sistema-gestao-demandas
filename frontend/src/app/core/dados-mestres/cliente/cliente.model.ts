export type TipoCliente = 'PESSOA' | 'AREA' | 'UNIDADE' | 'EXTERNO';

export interface Cliente {
  id: string;
  nome: string;
  tipo: TipoCliente;
  observacao: string | null;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ClientePayload {
  nome: string;
  tipo: TipoCliente;
  observacao: string | null;
}