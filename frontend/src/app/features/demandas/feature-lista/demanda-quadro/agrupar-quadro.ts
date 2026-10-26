import { Demanda, StatusDemanda } from '../../data-access/demanda.model';
import { ROTULO_STATUS } from '../../data-access/demanda-itens.model';

export type AgruparPor = 'SITUACAO' | 'NUCLEO' | 'EQUIPE' | 'PESSOA';

export interface ColunaQuadro {
  chave: string;
  rotulo: string;
  /** Texto menor ao lado do rótulo (a diretoria da equipe, por exemplo). */
  detalhe?: string;
  fotoUrl?: string | null;
  cards: Demanda[];
}

const ORDEM_STATUS: StatusDemanda[] = ['NAO_INICIADA', 'EM_ANDAMENTO', 'EM_APROVACAO', 'CONCLUIDA', 'CANCELADA'];
const SEM = '__sem__';

/**
 * Monta as colunas do quadro. Por situação são sempre as cinco, na ordem do fluxo. Por núcleo, equipe ou pessoa, uma
 * demanda aparece em cada coluna de quem está no time dela (uma vez só por coluna); sem ninguém, vai para a última.
 */
export function agruparQuadro(demandas: Demanda[], por: AgruparPor): ColunaQuadro[] {
  if (por === 'SITUACAO') {
    return ORDEM_STATUS.map((s) => ({
      chave: s,
      rotulo: ROTULO_STATUS[s],
      cards: demandas.filter((d) => d.status === s),
    }));
  }

  const colunas = new Map<string, ColunaQuadro>();
  const semColuna: ColunaQuadro = {
    chave: SEM,
    rotulo: por === 'NUCLEO' ? 'Sem núcleo' : por === 'EQUIPE' ? 'Sem equipe' : 'Sem time',
    cards: [],
  };

  for (const d of demandas) {
    const chaves = new Set<string>();
    for (const p of d.participantes ?? []) {
      let chave: string | null = null;
      let coluna: Omit<ColunaQuadro, 'cards'> | null = null;
      if (por === 'PESSOA') {
        chave = p.pessoaId;
        coluna = { chave, rotulo: p.nome, fotoUrl: p.fotoUrl };
      } else if (por === 'EQUIPE' && p.areaId) {
        chave = p.areaId;
        coluna = { chave, rotulo: p.areaNome ?? 'Equipe', detalhe: p.diretoriaNome ?? undefined };
      } else if (por === 'NUCLEO' && p.nucleoId) {
        chave = p.nucleoId;
        coluna = { chave, rotulo: p.nucleoNome ?? 'Núcleo' };
      }
      if (!chave || !coluna || chaves.has(chave)) continue;
      chaves.add(chave);
      if (!colunas.has(chave)) colunas.set(chave, { ...coluna, cards: [] });
      colunas.get(chave)!.cards.push(d);
    }
    if (chaves.size === 0) semColuna.cards.push(d);
  }

  const lista = [...colunas.values()].sort((a, b) => a.rotulo.localeCompare(b.rotulo, 'pt-BR') || (a.detalhe ?? '').localeCompare(b.detalhe ?? '', 'pt-BR'));
  return semColuna.cards.length ? [...lista, semColuna] : lista;
}
