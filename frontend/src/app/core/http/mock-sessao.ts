import type { Session } from '@supabase/supabase-js';

// MOCK TEMPORÁRIO (remover): rede do BB bloqueia o Supabase, então não há login real.
// Com MOCK_SESSAO = true o AuthService nasce autenticado com esta sessão falsa.
// Em casa, com a API real, trocar para false (ou remover junto com o mock.interceptor).
export const MOCK_SESSAO = true;

export const SESSAO_FALSA = {
  access_token: 'mock-token',
  refresh_token: 'mock-refresh',
  expires_in: 3600,
  token_type: 'bearer',
  user: { id: 'auth-1', email: 'neto@exemplo.com' },
} as unknown as Session;
