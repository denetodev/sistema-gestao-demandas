import { chaveCValida, normalizarChaveC } from './chave-c';

describe('chave C', () => {
  it('aceita C ou F seguida de 7 dígitos, em maiúscula ou minúscula', () => {
    expect(chaveCValida('C1349864')).toBeTrue();
    expect(chaveCValida('c1349844')).toBeTrue();
    expect(chaveCValida('F0000001')).toBeTrue();
  });

  it('rejeita outra letra, tamanho errado e CPF', () => {
    expect(chaveCValida('X1349864')).toBeFalse();
    expect(chaveCValida('C134986')).toBeFalse();
    expect(chaveCValida('C13498645')).toBeFalse();
    expect(chaveCValida('529.982.247-25')).toBeFalse();
    expect(chaveCValida('')).toBeFalse();
  });

  it('normaliza espaços e maiúscula', () => {
    expect(normalizarChaveC('  c1349844 ')).toBe('C1349844');
  });
});
