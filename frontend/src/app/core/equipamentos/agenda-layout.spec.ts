import { dias, inicioDoDia, posicaoAgora, posicionar, somarDias } from './agenda-layout';
import type { BlocoAgenda } from './equipamento.model';

describe('layout da agenda (Gantt)', () => {
  // janela de 10 dias: 20/10 a 30/10 (horário local)
  const de = new Date(2026, 9, 20);
  const ate = new Date(2026, 9, 30);
  const agora = new Date(2026, 9, 22, 12);

  const bloco = (parcial: Omit<Partial<BlocoAgenda>, 'inicio' | 'fim'> & { inicio: Date; fim: Date }): BlocoAgenda =>
    ({
      emprestimoId: Math.random().toString(),
      retiranteNome: 'João', status: 'AUTORIZADO', devolvido: false, atrasado: false, externo: false, localUso: null, quantidade: 1,
      ...parcial, inicio: parcial.inicio.toISOString(), fim: parcial.fim.toISOString(),
    }) as BlocoAgenda;

  it('posiciona a barra em % da janela', () => {
    const { itens } = posicionar([bloco({ inicio: new Date(2026, 9, 22), fim: new Date(2026, 9, 25) })], de, ate, agora);
    expect(itens.length).toBe(1);
    expect(itens[0].esquerda).toBeCloseTo(20, 1); // 2 de 10 dias
    expect(itens[0].largura).toBeCloseTo(30, 1); // 3 de 10 dias
    expect(itens[0].faixa).toBe(0);
  });

  it('apara o que cruza a borda e descarta o que está fora da janela', () => {
    const { itens } = posicionar(
      [
        bloco({ retiranteNome: 'começa antes', inicio: new Date(2026, 9, 18), fim: new Date(2026, 9, 22) }),
        bloco({ retiranteNome: 'acaba depois', inicio: new Date(2026, 9, 28), fim: new Date(2026, 10, 5) }),
        bloco({ retiranteNome: 'fora', inicio: new Date(2026, 10, 10), fim: new Date(2026, 10, 12) }),
      ],
      de, ate, agora,
    );
    expect(itens.map((i) => i.bloco.retiranteNome)).toEqual(['começa antes', 'acaba depois']);
    expect(itens[0].esquerda).toBe(0);
    expect(itens[0].largura).toBeCloseTo(20, 1);
    expect(itens[1].esquerda + itens[1].largura).toBeLessThanOrEqual(100.0001);
  });

  it('empilha períodos que se sobrepõem em faixas e reaproveita a faixa livre', () => {
    const { itens, faixas } = posicionar(
      [
        bloco({ retiranteNome: 'A', inicio: new Date(2026, 9, 21), fim: new Date(2026, 9, 24) }),
        bloco({ retiranteNome: 'B', inicio: new Date(2026, 9, 22), fim: new Date(2026, 9, 23) }),
        bloco({ retiranteNome: 'C', inicio: new Date(2026, 9, 25), fim: new Date(2026, 9, 26) }),
      ],
      de, ate, agora,
    );
    const faixaDe = (n: string) => itens.find((i) => i.bloco.retiranteNome === n)!.faixa;
    expect(faixaDe('A')).toBe(0);
    expect(faixaDe('B')).toBe(1); // sobrepõe A
    expect(faixaDe('C')).toBe(0); // A já acabou
    expect(faixas).toBe(2);
  });

  it('atrasado ganha a faixa de atraso da devolução prevista até agora', () => {
    const { itens } = posicionar(
      [bloco({ status: 'RETIRADO', atrasado: true, inicio: new Date(2026, 9, 20), fim: new Date(2026, 9, 21) })],
      de, ate, agora,
    );
    expect(itens[0].largura).toBeCloseTo(10, 1); // até a devolução prevista
    expect(itens[0].atrasoEsquerda).toBeCloseTo(10, 1);
    expect(itens[0].atrasoLargura).toBeCloseTo(15, 1); // de 21/10 a 22/10 12h
  });

  it('um período curtinho continua com largura mínima para dar para clicar', () => {
    const { itens } = posicionar([bloco({ inicio: new Date(2026, 9, 22, 10), fim: new Date(2026, 9, 22, 10, 5) })], de, ate, agora);
    expect(itens[0].largura).toBeGreaterThanOrEqual(0.7);
  });

  it('uma coluna por dia, com fim de semana e hoje marcados', () => {
    const cols = dias(de, 10, agora);
    expect(cols.length).toBe(10);
    expect(cols[0].esquerda).toBe(0);
    expect(cols[0].largura).toBeCloseTo(10, 1);
    expect(cols[2].hoje).toBeTrue(); // 22/10
    expect(cols.filter((c) => c.hoje).length).toBe(1);
    expect(cols.find((c) => c.rotulo === '24/10')!.fimDeSemana).toBeTrue(); // sábado
    expect(cols[0].rotulo).toBe('20/10');
  });

  it('marca o agora em % só quando está dentro da janela', () => {
    expect(posicaoAgora(de, ate, agora)).toBeCloseTo(25, 1);
    expect(posicaoAgora(de, ate, new Date(2026, 8, 1))).toBeNull();
  });

  it('somar dias e começo do dia usam o calendário local', () => {
    expect(somarDias(new Date(2026, 9, 31), 1).getDate()).toBe(1);
    expect(inicioDoDia(new Date(2026, 9, 22, 15, 30)).getHours()).toBe(0);
  });
});
