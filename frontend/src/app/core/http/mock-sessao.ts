import type { Session } from '@supabase/supabase-js';

// Só para o computador do trabalho (rede do BB bloqueia o Supabase, então não há login real).
// Usada pelo AuthService apenas quando environment.usarMock é true (ng serve -c mock).

export const SESSAO_FALSA = {
  access_token: 'mock-token',
  refresh_token: 'mock-refresh',
  expires_in: 3600,
  token_type: 'bearer',
  user: { id: 'auth-1', email: 'neto@exemplo.com' },
} as unknown as Session;
