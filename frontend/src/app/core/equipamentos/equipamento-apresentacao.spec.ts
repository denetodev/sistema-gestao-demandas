import { pedeAtencao, resumoItens, rotuloSituacao, severidadeSituacao } from './equipamento-apresentacao';

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
