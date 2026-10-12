import { contemTexto, maisRecentesPrimeiro, normalizar } from './listas';

describe('listas', () => {
  describe('normalizar', () => {
    it('tira acentos, maiusculas e espacos das pontas', () => {
      expect(normalizar('  Captação AGRO ')).toBe('captacao agro');
      expect(normalizar('Ação')).toBe('acao');
    });

    it('trata nulo e indefinido como texto vazio', () => {
      expect(normalizar(null)).toBe('');
      expect(normalizar(undefined)).toBe('');
    });
  });

  describe('contemTexto', () => {
    it('acha sem se importar com acento ou caixa', () => {
      expect(contemTexto(['Campanha Plano Safra'], 'SAFRA')).toBeTrue();
      expect(contemTexto(['Redação'], 'redacao')).toBeTrue();
    });

    it('olha todos os campos e ignora os vazios', () => {
      expect(contemTexto([null, 'BC2607CQ', undefined], 'bc26')).toBeTrue();
      expect(contemTexto([null, undefined], 'x')).toBeFalse();
    });

    it('busca vazia deixa tudo passar', () => {
      expect(contemTexto(['qualquer'], '')).toBeTrue();
      expect(contemTexto([], '   ')).toBeTrue();
    });

    it('nao acha o que nao esta la', () => {
      expect(contemTexto(['Plano Safra'], 'livelo')).toBeFalse();
    });
  });

  describe('maisRecentesPrimeiro', () => {
    it('ordena pela data de cadastro, da mais nova para a mais antiga, sem alterar a lista original', () => {
      const lista = [
        { id: 'a', createdAt: '2026-01-01T10:00:00Z' },
        { id: 'c', createdAt: '2026-03-01T10:00:00Z' },
        { id: 'b', createdAt: '2026-02-01T10:00:00Z' },
      ];
      expect(maisRecentesPrimeiro(lista).map((x) => x.id)).toEqual(['c', 'b', 'a']);
      expect(lista.map((x) => x.id)).toEqual(['a', 'c', 'b']);
    });
  });
});
