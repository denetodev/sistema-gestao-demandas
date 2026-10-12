import { filtrarCampanhas } from './opcoes-campanha';

describe('filtrarCampanhas', () => {
  const semProjeto = { id: 'c1', projetoId: null };
  const doProjetoA = { id: 'c2', projetoId: 'A' };
  const doProjetoB = { id: 'c3', projetoId: 'B' };
  const todas = [semProjeto, doProjetoA, doProjetoB];

  it('sem projeto escolhido, oferece todas as campanhas (inclusive as sem projeto)', () => {
    expect(filtrarCampanhas(todas, null)).toEqual(todas);
  });

  it('campanha sem projeto continua disponivel quando um projeto e escolhido', () => {
    expect(filtrarCampanhas(todas, 'A')).toContain(semProjeto);
  });

  it('com projeto escolhido, oferece as dele e esconde as de outros projetos', () => {
    expect(filtrarCampanhas(todas, 'A')).toEqual([semProjeto, doProjetoA]);
  });

  it('lista vazia continua vazia', () => {
    expect(filtrarCampanhas([], 'A')).toEqual([]);
  });
});
