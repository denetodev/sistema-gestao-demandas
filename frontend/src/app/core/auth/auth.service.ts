import { Injectable, signal, computed } from '@angular/core';
import { httpResource } from '@angular/common/http';
import type { Session } from '@supabase/supabase-js';
import { supabase } from './supabase-client';
import { environment } from '../../../environments/environment';
import { PessoaMe, PessoaMeResponse, Perfil } from './auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  #session = signal<Session | null>(null);
  session = this.#session.asReadonly();
  autenticado = computed(() => this.#session() !== null);
  ready: Promise<void>;

  me = httpResource<PessoaMeResponse>(() =>
    this.#session() ? `${environment.apiUrl}/pessoas/me` : undefined,
  );


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

  /** Todo perfil vinculado exceto VISUALIZADOR. */
  podeEditar(): boolean {
    const perfil = this.pessoa()?.perfil;
    return !!perfil && perfil !== 'VISUALIZADOR';
  }

  constructor() {
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
}