import { podeMexerNoItem, PessoaParaItem } from './pode-mexer-no-item';

describe('podeMexerNoItem', () => {
  const profissional: PessoaParaItem = { id: 'eu', perfil: 'PROFISSIONAL', referenciaAreaId: null };

  it('o dono mexe no proprio item', () => {
    expect(podeMexerNoItem(profissional, 'eu')).toBeTrue();
  });

  it('profissional comum nao mexe no item de outra pessoa nem em item sem dono', () => {
    expect(podeMexerNoItem(profissional, 'outra')).toBeFalse();
    expect(podeMexerNoItem(profissional, null)).toBeFalse();
  });

  it('Gestor e Admin mexem em qualquer item', () => {
    expect(podeMexerNoItem({ ...profissional, perfil: 'GESTOR' }, 'outra')).toBeTrue();
    expect(podeMexerNoItem({ ...profissional, perfil: 'ADMIN' }, 'outra')).toBeTrue();
  });

  it('Referencia de equipe ve os botoes nos itens dos colegas (o backend confere a area)', () => {
    expect(podeMexerNoItem({ ...profissional, referenciaAreaId: 'a1' }, 'outra')).toBeTrue();
  });

  it('Visualizador e ninguem logado nunca', () => {
    expect(podeMexerNoItem({ ...profissional, perfil: 'VISUALIZADOR' }, 'eu')).toBeFalse();
    expect(podeMexerNoItem(null, 'eu')).toBeFalse();
  });
});
