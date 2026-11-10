import { semanaDaData, semanasDoMes } from './semanas';

describe('semanasDoMes', () => {
  it('agosto de 2026 começa num sábado e o fim de semana entra na primeira semana útil', () => {
    const s = semanasDoMes('2026-08');
    expect(s.length).toBe(5);
    expect(s[0]).toEqual({ numero: 1, de: '2026-08-01', ate: '2026-08-09' });
    expect(s[3].de).toBe('2026-08-24');
    expect(s[4].ate).toBe('2026-08-31');
  });

  it('outubro de 2026 tem 5 semanas cortadas nas pontas do mês', () => {
    const s = semanasDoMes('2026-10');
    expect(s.length).toBe(5);
    expect(s[0]).toEqual({ numero: 1, de: '2026-10-01', ate: '2026-10-04' });
    expect(s[4]).toEqual({ numero: 5, de: '2026-10-26', ate: '2026-10-31' });
  });

  it('todo dia do mês cai em exatamente uma semana', () => {
    const s = semanasDoMes('2026-03');
    for (let d = 1; d <= 31; d++) {
      const dia = `2026-03-${String(d).padStart(2, '0')}`;
      expect(s.filter((x) => dia >= x.de && dia <= x.ate).length).toBe(1);
      expect(semanaDaData(s, dia)).toBeTruthy();
    }
  });
});
