import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, firstValueFrom } from 'rxjs';
import type { Session } from '@supabase/supabase-js';
import { supabase } from './supabase-client';
import { environment } from '../../../environments/environment';
import { MOCK_SESSAO, SESSAO_FALSA } from '../http/mock-sessao';
import { PessoaMe, PessoaMeResponse, Perfil } from './auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  #session = signal<Session | null>(null);
  session = this.#session.asReadonly();
  autenticado = computed(() => this.#session() !== null);
  ready: Promise<void>;
  #http = inject(HttpClient);
  emailSessao = computed(() => this.#session()?.user.email ?? null);

  me = httpResource<PessoaMeResponse>(() =>
    this.#session() ? `${environment.apiUrl}/pessoas/me` : undefined,
  );

  #statusMe$ = toObservable(this.me.status);

  /** Espera /pessoas/me terminar de carregar (guards de perfil precisam do perfil). */
  aguardarPessoa(): Promise<unknown> {
    return firstValueFrom(this.#statusMe$.pipe(filter((s) => s === 'resolved' || s === 'local' || s === 'error')));
  }

  vinculado = computed(() => {
    if (this.me.status() === 'error') return false;
    return this.me.value()?.vinculado ?? false;
  });

  pessoa = computed<PessoaMe | null>(() => {
    if (this.me.status() === 'error') return null;
    return this.me.value()?.pessoa ?? null;
  });

  erroAoCarregarPessoa = computed(() => this.me.status() === 'error');

  temPerfil(...perfis: Perfil[]): boolean {
    const perfil = this.pessoa()?.perfil;
    return perfil ? perfis.includes(perfil) : false;
  }

  ehReferenciaDa(areaId: string | null | undefined): boolean {
    const ref = this.pessoa()?.referenciaAreaId;
    return !!ref && !!areaId && ref === areaId;
  }

  /** Todo perfil vinculado exceto VISUALIZADOR. */
  podeEditar(): boolean {
    const perfil = this.pessoa()?.perfil;
    return !!perfil && perfil !== 'VISUALIZADOR';
  }

  constructor() {
    if (MOCK_SESSAO) {
      this.#session.set(SESSAO_FALSA);
      this.ready = Promise.resolve();
      return;
    }
    this.ready = supabase.auth.getSession().then(({ data }) => {
      this.#session.set(data.session);
    });
    supabase.auth.onAuthStateChange((_event, session) => {
      this.#session.set(session);
    });
  }

  async login(email: string, password: string) {
    const { error } = await supabase.auth.signInWithPassword({ email, password });
    if (error) throw error;
  }

  async cadastrar(email: string, password: string): Promise<{ confirmado: boolean }> {
    const { data, error } = await supabase.auth.signUp({ email, password });
    if (error) throw error;
    return { confirmado: data.session !== null };
  }

  async logout() {
    await supabase.auth.signOut();
  }

  get accessToken(): string | null {
    return this.#session()?.access_token ?? null;
  }

  atualizarPerfil(payload: { nomeExibicao: string | null; fotoUrl: string | null }) {
    return this.#http.put<PessoaMe>(`${environment.apiUrl}/pessoas/me`, payload);
  }

  /** Cria o cadastro de pessoa da conta logada; fica aguardando aprovação de Gestor/Admin. */
  completarCadastro(payload: { nome: string; cpf: string; nomeExibicao: string | null; areaId: string; cargoId: string | null }) {
    return this.#http.post<PessoaMe>(`${environment.apiUrl}/pessoas/auto-cadastro`, payload);
  }

  async trocarSenha(novaSenha: string) {
    const { error } = await supabase.auth.updateUser({ password: novaSenha });
    if (error) throw error;
  }
}
