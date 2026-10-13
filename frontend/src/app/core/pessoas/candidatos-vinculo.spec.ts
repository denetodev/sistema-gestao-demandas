import { candidatosAVinculo } from './candidatos-vinculo';
import type { Pessoa } from './pessoa.model';

function pessoa(parcial: Partial<Pessoa>): Pessoa {
  return {
    id: 'id', nome: 'Fulano', nomeExibicao: null, cpfMascarado: null, email: null,
    diretoriaId: null, diretoriaNome: null, nucleoId: null, nucleoNome: null, areaId: null, areaNome: null, cargoId: null, cargoNome: null,
    referenciaAreaId: null, referenciaAreaNome: null, fotoUrl: null, status: 'ATIVO', perfil: 'PROFISSIONAL',
    authUserId: null, aprovadoPorId: null, aprovadoPorNome: null, aprovadoEm: '2026-10-08T22:00:00Z',
    createdAt: '2026-10-08T22:00:00Z', updatedAt: '2026-10-08T22:00:00Z',
    ...parcial,
  };
}

describe('candidatosAVinculo', () => {
  it('oferece quem esta aprovado, ativo e sem conta', () => {
    const lista = [pessoa({ id: '1', nome: 'Ana' })];
    expect(candidatosAVinculo(lista).map((c) => c.id)).toEqual(['1']);
  });

  it('nao oferece quem ja tem conta, esta pendente, inativo ou rejeitado', () => {
    const lista = [
      pessoa({ id: 'com-conta', authUserId: 'abc' }),
      pessoa({ id: 'pendente', aprovadoEm: null }),
      pessoa({ id: 'inativo', status: 'INATIVO' }),
      pessoa({ id: 'rejeitado', status: 'REJEITADO', aprovadoEm: null }),
    ];
    expect(candidatosAVinculo(lista)).toEqual([]);
  });

  it('ordena por nome (com acento) e mostra a area ao lado', () => {
    const lista = [
      pessoa({ id: '2', nome: 'Érica', areaNome: 'Design' }),
      pessoa({ id: '1', nome: 'Ana' }),
      pessoa({ id: '3', nome: 'Bruno', areaNome: 'Vídeo' }),
    ];
    expect(candidatosAVinculo(lista)).toEqual([
      { id: '1', label: 'Ana' },
      { id: '3', label: 'Bruno · Vídeo' },
      { id: '2', label: 'Érica · Design' },
    ]);
  });
});
