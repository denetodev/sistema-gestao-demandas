export interface Campanha {
  id: string;
  nome: string;
  projetoId: string | null;
  projetoNome: string | null;
  codigo: string | null;
  dataInicio: string | null;
  dataFim: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CampanhaPayload {
  nome: string;
  projetoId: string | null;
  codigo: string | null;
  dataInicio: string | null;
  dataFim: string | null;
}