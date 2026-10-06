import { cpfValido, formatarCpf, somenteDigitos } from './cpf';

describe('cpf', () => {
  it('aceita CPF com dígitos verificadores corretos, com ou sem pontuação', () => {
    expect(cpfValido('52998224725')).toBeTrue();
    expect(cpfValido('529.982.247-25')).toBeTrue();
  });

  it('rejeita dígito errado, sequência repetida e tamanho errado', () => {
    expect(cpfValido('52998224726')).toBeFalse();
    expect(cpfValido('111.111.111-11')).toBeFalse();
    expect(cpfValido('5299822472')).toBeFalse();
    expect(cpfValido(null)).toBeFalse();
  });

  it('formata enquanto digita', () => {
    expect(formatarCpf('5299')).toBe('529.9');
    expect(formatarCpf('529982247')).toBe('529.982.247');
    expect(formatarCpf('52998224725')).toBe('529.982.247-25');
    expect(somenteDigitos('529.982.247-25')).toBe('52998224725');
  });
});
