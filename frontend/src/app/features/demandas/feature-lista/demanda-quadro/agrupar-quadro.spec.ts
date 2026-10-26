import { Demanda, ParticipanteResumo, StatusDemanda } from '../../data-access/demanda.model';
import { agruparQuadro } from './agrupar-quadro';

function pessoa(nome: string, extra: Partial<ParticipanteResumo> = {}): ParticipanteResumo {
  return { pessoaId: nome, nome, fotoUrl: null, ...extra };
}

function demanda(id: string, status: StatusDemanda, participantes: ParticipanteResumo[]): Demanda {
  return { id, status, participantes } as Demanda;
}

describe('agruparQuadro', () => {
  const ana = pessoa('Ana', { areaId: 'a1', areaNome: 'Design', diretoriaNome: 'COE/CRM', nucleoId: 'n1', nucleoNome: 'House-CRM' });
  const bia = pessoa('Bia', { areaId: 'a2', areaNome: 'HTML', diretoriaNome: 'COE/CRM', nucleoId: 'n1', nucleoNome: 'House-CRM' });
  const caio = pessoa('Caio', { areaId: 'a3', areaNome: 'Design', diretoriaNome: 'UGR' });

  it('por situação são sempre as cinco colunas, na ordem do fluxo', () => {
    const cols = agruparQuadro([demanda('1', 'CONCLUIDA', []), demanda('2', 'NAO_INICIADA', [])], 'SITUACAO');
    expect(cols.map((c) => c.chave)).toEqual(['NAO_INICIADA', 'EM_ANDAMENTO', 'EM_APROVACAO', 'CONCLUIDA', 'CANCELADA']);
    expect(cols[0].cards.map((d) => d.id)).toEqual(['2']);
    expect(cols[3].cards.map((d) => d.id)).toEqual(['1']);
    expect(cols[1].cards).toEqual([]);
  });

  it('por pessoa, a demanda aparece na coluna de cada pessoa do time; sem time vai para a última', () => {
    const cols = agruparQuadro([demanda('1', 'EM_ANDAMENTO', [ana, bia]), demanda('2', 'EM_ANDAMENTO', [])], 'PESSOA');
    expect(cols.map((c) => c.rotulo)).toEqual(['Ana', 'Bia', 'Sem time']);
    expect(cols[0].cards.map((d) => d.id)).toEqual(['1']);
    expect(cols[1].cards.map((d) => d.id)).toEqual(['1']);
    expect(cols[2].cards.map((d) => d.id)).toEqual(['2']);
  });

  it('por núcleo, duas pessoas do mesmo núcleo não repetem o card; quem não tem núcleo cai em "Sem núcleo"', () => {
    const cols = agruparQuadro([demanda('1', 'EM_ANDAMENTO', [ana, bia]), demanda('2', 'EM_ANDAMENTO', [caio])], 'NUCLEO');
    expect(cols.map((c) => c.rotulo)).toEqual(['House-CRM', 'Sem núcleo']);
    expect(cols[0].cards.map((d) => d.id)).toEqual(['1']);
    expect(cols[1].cards.map((d) => d.id)).toEqual(['2']);
  });

  it('por equipe, o mesmo nome em diretorias diferentes vira colunas separadas, com a diretoria ao lado', () => {
    const cols = agruparQuadro([demanda('1', 'EM_ANDAMENTO', [ana, caio])], 'EQUIPE');
    expect(cols.map((c) => `${c.rotulo} ${c.detalhe}`)).toEqual(['Design COE/CRM', 'Design UGR']);
    expect(cols.every((c) => c.cards.length === 1)).toBe(true);
  });
});
