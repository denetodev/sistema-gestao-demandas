// Só para o computador do trabalho: a rede do BB bloqueia o Supabase.
// Uso: npx ng serve -c mock   (sessão falsa + mock.interceptor com dados de mentira)
export const environment = {
  production: false,
  usarMock: true,
  apiUrl: '/api',
  supabaseUrl: 'https://sctjksvryeboakgjmnqg.supabase.co',
  supabaseAnonKey: 'sb_publishable_zNJz_TFR1fdDXtizjfyV-A_PkDEEk6B',
};
