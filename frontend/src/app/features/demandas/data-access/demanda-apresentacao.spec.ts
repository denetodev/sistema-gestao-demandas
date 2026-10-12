import { separarCodigo } from './demanda-apresentacao';

describe('separarCodigo', () => {
  it('tira o codigo do inicio do titulo e o devolve a parte', () => {
    expect(separarCodigo('BC2607CQ CAM #1.2 - OPF - Clientes', 'BC2607CQ'))
      .toEqual({ codigo: 'BC2607CQ', titulo: 'CAM #1.2 - OPF - Clientes' });
  });

  it('limpa o separador que vem depois do codigo', () => {
    expect(separarCodigo('BC2604FY - Conta Fácil', 'BC2604FY')).toEqual({ codigo: 'BC2604FY', titulo: 'Conta Fácil' });
    expect(separarCodigo('BC2604FY_Conta', 'BC2604FY').titulo).toBe('Conta');
  });

  it('nao diferencia maiusculas ao procurar o codigo no inicio', () => {
    expect(separarCodigo('bc2604fy - Conta', 'BC2604FY').titulo).toBe('Conta');
  });

  it('se o codigo nao esta no inicio, o titulo fica como esta e o chip aparece mesmo assim', () => {
    expect(separarCodigo('20260501_BC2512CM CAM #3', 'BC2512CM'))
      .toEqual({ codigo: 'BC2512CM', titulo: '20260501_BC2512CM CAM #3' });
  });

  it('sem codigo nao ha chip', () => {
    expect(separarCodigo('Atualizar layout', null)).toEqual({ codigo: null, titulo: 'Atualizar layout' });
    expect(separarCodigo('Atualizar layout', '  ')).toEqual({ codigo: null, titulo: 'Atualizar layout' });
  });

  it('se o titulo e so o codigo, nao fica vazio', () => {
    expect(separarCodigo('BC2607CQ', 'BC2607CQ').titulo).toBe('BC2607CQ');
  });
});
