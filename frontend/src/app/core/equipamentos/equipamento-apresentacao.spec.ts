import { FILTRO_VAZIO, filtroAtivo, filtrarEmprestimos, pedeAtencao, resumoItens, rotuloSituacao, severidadeSituacao } from './equipamento-apresentacao';
import type { Emprestimo } from './equipamento.model';

describe('apresentação de empréstimos', () => {
  it('atrasado vence a situação no rótulo e na cor', () => {
    expect(rotuloSituacao({ status: 'RETIRADO', atrasado: true })).toBe('Atrasado');
    expect(severidadeSituacao({ status: 'RETIRADO', atrasado: true })).toBe('danger');
    expect(rotuloSituacao({ status: 'RETIRADO', atrasado: false })).toBe('Com a pessoa');
  });

  it('externo aguardando autorização e atrasado pedem atenção; o resto não', () => {
    expect(pedeAtencao({ status: 'AGUARDANDO_AUTORIZACAO', externo: true, atrasado: false })).toBeTrue();
    expect(pedeAtencao({ status: 'RETIRADO', externo: false, atrasado: true })).toBeTrue();
    expect(pedeAtencao({ status: 'AGUARDANDO_AUTORIZACAO', externo: false, atrasado: false })).toBeFalse();
    expect(pedeAtencao({ status: 'RETIRADO', externo: true, atrasado: false })).toBeFalse();
  });

  it('resume os itens mostrando a quantidade só quando passa de um', () => {
    const itens = [
      { equipamentoNome: 'Câmera A7', quantidade: 1 },
      { equipamentoNome: 'Cabo HDMI', quantidade: 3 },
    ] as never;
    expect(resumoItens({ itens })).toBe('Câmera A7, Cabo HDMI (x3)');
  });
});

describe('filtro de empréstimos', () => {
  const emp = (parcial: Partial<Emprestimo>): Emprestimo =>
    ({
      status: 'RETIRADO', atrasado: false, externo: false, retiranteNome: 'João da Silva', retiranteChaveC: 'F1234567',
      retiranteContato: null, localUso: null, finalidade: null, demandaTitulo: null,
      previstaDevolucao: '2026-10-20T15:00:00.000Z', itens: [{ equipamentoNome: 'Câmera A7', patrimonio: 'PAT-1', categoriaNome: 'Câmeras' }],
      ...parcial,
    }) as Emprestimo;

  const lista = [
    emp({ retiranteNome: 'João da Silva' }),
    emp({ retiranteNome: 'Maria Souza', retiranteChaveC: 'C7654321', externo: true, status: 'AGUARDANDO_AUTORIZACAO', itens: [{ equipamentoNome: 'Tripé', patrimonio: null, categoriaNome: 'Suportes' }] as never }),
    emp({ retiranteNome: 'Pedro', atrasado: true, previstaDevolucao: '2026-10-01T15:00:00.000Z' }),
  ];

  it('sem filtro devolve tudo e não conta como ativo', () => {
    expect(filtrarEmprestimos(lista, FILTRO_VAZIO).length).toBe(3);
    expect(filtroAtivo(FILTRO_VAZIO)).toBeFalse();
  });

  it('busca por pessoa, chave, equipamento ou patrimônio, sem diferenciar acento nem maiúscula', () => {
    expect(filtrarEmprestimos(lista, { ...FILTRO_VAZIO, busca: 'joao' }).map((e) => e.retiranteNome)).toEqual(['João da Silva']);
    expect(filtrarEmprestimos(lista, { ...FILTRO_VAZIO, busca: 'c7654321' }).length).toBe(1);
    expect(filtrarEmprestimos(lista, { ...FILTRO_VAZIO, busca: 'tripe' }).length).toBe(1);
    expect(filtrarEmprestimos(lista, { ...FILTRO_VAZIO, busca: 'pat-1' }).length).toBe(2);
  });

  it('filtra por situação (inclusive atrasado) e por interno ou externo', () => {
    expect(filtrarEmprestimos(lista, { ...FILTRO_VAZIO, situacao: 'ATRASADO' }).map((e) => e.retiranteNome)).toEqual(['Pedro']);
    expect(filtrarEmprestimos(lista, { ...FILTRO_VAZIO, situacao: 'AGUARDANDO_AUTORIZACAO' }).length).toBe(1);
    expect(filtrarEmprestimos(lista, { ...FILTRO_VAZIO, externo: true }).length).toBe(1);
    expect(filtrarEmprestimos(lista, { ...FILTRO_VAZIO, externo: false }).length).toBe(2);
  });

  it('filtra pela devolução prevista, incluindo o último dia', () => {
    const f = { ...FILTRO_VAZIO, de: new Date(2026, 9, 20), ate: new Date(2026, 9, 20) };
    expect(filtrarEmprestimos(lista, f).length).toBe(2);
    expect(filtrarEmprestimos(lista, { ...FILTRO_VAZIO, ate: new Date(2026, 9, 5) }).map((e) => e.retiranteNome)).toEqual(['Pedro']);
    expect(filtroAtivo(f)).toBeTrue();
  });
});
