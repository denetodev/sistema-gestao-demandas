import { formatarData, paraData } from './data';

describe('util de data', () => {
  it('paraData lê a data no fuso local, sem escorregar de dia', () => {
    const d = paraData('2026-03-31')!;
    expect(d.getFullYear()).toBe(2026);
    expect(d.getMonth()).toBe(2);
    expect(d.getDate()).toBe(31);
  });

  it('paraData devolve nulo para vazio', () => {
    expect(paraData(null)).toBeNull();
    expect(paraData(undefined)).toBeNull();
    expect(paraData('')).toBeNull();
  });

  it('formatarData completa mês e dia com zero à esquerda', () => {
    expect(formatarData(new Date(2026, 0, 5))).toBe('2026-01-05');
    expect(formatarData(null)).toBeNull();
  });

  it('ida e volta preserva o dia', () => {
    expect(formatarData(paraData('2026-12-01'))).toBe('2026-12-01');
  });
});
