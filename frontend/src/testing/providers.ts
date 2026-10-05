import { EnvironmentProviders, Provider, computed, signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import { AuthService } from '../app/core/auth/auth.service';
import type { Perfil, PessoaMe } from '../app/core/auth/auth.model';
import { PRIMENG_PT_BR } from '../app/core/theme/primeng-pt-br';

export interface OpcoesTeste {
  perfil?: Perfil;
  referenciaAreaId?: string | null;
  /** false simula conta ainda não aprovada */
  vinculado?: boolean;
}

export function pessoaDeTeste(opcoes: OpcoesTeste = {}): PessoaMe {
  return {
    id: 'p-teste',
    nome: 'Pessoa de Teste',
    email: 'teste@exemplo.com',
    diretoriaId: 'd1',
    diretoriaNome: 'COE/CRM',
    areaId: 'a1',
    areaNome: 'Design',
    cargoId: null,
    cargoNome: null,
    referenciaAreaId: opcoes.referenciaAreaId ?? null,
    referenciaAreaNome: opcoes.referenciaAreaId ? 'Design' : null,
    status: 'ATIVO',
    perfil: opcoes.perfil ?? 'ADMIN',
    authUserId: 'auth-teste',
    aprovadoPorId: null,
    aprovadoPorNome: null,
    aprovadoEm: '2026-01-01T00:00:00Z',
    createdAt: '',
    updatedAt: '',
    fotoUrl: null,
  };
}

/** Substituto do AuthService: sem Supabase e sem rede, com o perfil escolhido pelo teste. */
export function criarAuthFalso(opcoes: OpcoesTeste = {}) {
  const pessoa = signal<PessoaMe | null>(pessoaDeTeste(opcoes));
  const vinculado = signal(opcoes.vinculado ?? true);
  return {
    session: signal(null),
    autenticado: signal(true),
    ready: Promise.resolve(),
    emailSessao: signal('teste@exemplo.com'),
    me: {
      isLoading: signal(false),
      status: signal('resolved'),
      value: signal({ vinculado: vinculado(), pessoa: pessoa() }),
      reload: () => true,
    },
    vinculado,
    pessoa,
    erroAoCarregarPessoa: signal(false),
    aguardarPessoa: () => Promise.resolve('resolved'),
    temPerfil: (...perfis: Perfil[]) => perfis.includes(pessoa()!.perfil),
    ehReferenciaDa: (areaId: string | null | undefined) => !!areaId && pessoa()!.referenciaAreaId === areaId,
    podeEditar: () => pessoa()!.perfil !== 'VISUALIZADOR',
    login: () => Promise.resolve(),
    cadastrar: () => Promise.resolve({ confirmado: true }),
    logout: () => Promise.resolve(),
    accessToken: 'token-de-teste',
    atualizarPerfil: () => {
      throw new Error('não usado nos testes');
    },
    trocarSenha: () => Promise.resolve(),
    perfil: computed(() => pessoa()!.perfil),
  };
}

/** Providers comuns dos testes de componente: HTTP simulado, rotas vazias, PrimeNG e auth falso. */
export function provideTesteApp(opcoes: OpcoesTeste = {}): (Provider | EnvironmentProviders)[] {
  return [
    provideHttpClient(),
    provideHttpClientTesting(),
    provideRouter([]),
    provideNoopAnimations(),
    providePrimeNG({ translation: PRIMENG_PT_BR }),
    MessageService,
    ConfirmationService,
    { provide: AuthService, useValue: criarAuthFalso(opcoes) },
  ];
}
