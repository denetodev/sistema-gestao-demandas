import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import type { Session } from '@supabase/supabase-js';
import { AuthService } from './auth.service';
import { supabase } from './supabase-client';

function sessao(token: string): Session {
  return { access_token: token, user: { email: 'a@b.com' } } as unknown as Session;
}

describe('AuthService (voltar à aba não recarrega a pessoa)', () => {
  let emitir: (session: Session | null) => void;
  let http: HttpTestingController;

  beforeEach(async () => {
    spyOn(supabase.auth, 'getSession').and.resolveTo({ data: { session: sessao('t1') }, error: null } as never);
    spyOn(supabase.auth, 'onAuthStateChange').and.callFake(((cb: (e: string, s: Session | null) => void) => {
      emitir = (s) => cb('SIGNED_IN', s);
      return { data: { subscription: { unsubscribe() {} } } };
    }) as never);

    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const auth = TestBed.inject(AuthService);
    await auth.ready;
    TestBed.tick();
  });

  it('um Session novo do mesmo usuário (aba voltou ao foco) não refaz GET /pessoas/me', () => {
    http.expectOne((r) => r.url.endsWith('/pessoas/me')).flush({ vinculado: true, pessoa: null });
    TestBed.tick();

    emitir(sessao('t2')); // token renovado, mesmo usuário
    TestBed.tick();

    http.expectNone((r) => r.url.endsWith('/pessoas/me'));
  });
});
