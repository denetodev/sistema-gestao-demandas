import { Injectable, signal, computed } from '@angular/core';
import { httpResource } from '@angular/common/http';
import type { Session } from '@supabase/supabase-js';
import { supabase } from './supabase-client';
import { environment } from '../../../environments/environment';
import { PessoaMeResponse } from './auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  #session = signal<Session | null>(null);
  session = this.#session.asReadonly();
  autenticado = computed(() => this.#session() !== null);

  /** resolve depois que o Supabase confirmou se já existia sessão salva */
  ready: Promise<void>;

  me = httpResource<PessoaMeResponse>(() =>
    this.#session() ? `${environment.apiUrl}/pessoas/me` : undefined,
  );

  vinculado = computed(() => this.me.value()?.vinculado ?? false);
  pessoa = computed(() => this.me.value()?.pessoa ?? null);

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
