-- database/migrations/V10__habilitar-rls.sql
-- Liga Row Level Security em todas as tabelas do schema public, sem policies.
--
-- Motivo: o Supabase expõe o schema public por PostgREST. Com RLS desligado, quem tem a
-- anon key (que vai no bundle do frontend) lê e grava essas tabelas direto, sem passar
-- pela API Spring, que é onde a segurança do SGD vive.
--
-- Efeito: os papéis anon e authenticated (usados pelo supabase-js e pelo PostgREST) passam
-- a não ver nem alterar nenhuma linha. A API Spring conecta como "postgres" (dono das
-- tabelas, com BYPASSRLS), então continua funcionando sem mudança. O frontend só usa o
-- Supabase para autenticar (supabase.auth), nunca para consultar tabelas.
--
-- Se um dia o frontend precisar ler tabelas direto, crie policies específicas em vez de
-- desligar o RLS. relatorio_mensal já nasceu com RLS ligado na V9.
--
-- Reversão (se algo inesperado aparecer): alter table <tabela> disable row level security;

alter table diretoria              enable row level security;
alter table area                   enable row level security;
alter table cargo                  enable row level security;
alter table especialidade          enable row level security;
alter table pessoa                 enable row level security;
alter table pessoa_especialidade   enable row level security;
alter table pessoa_cargo_historico enable row level security;
alter table demandante             enable row level security;
alter table projeto                enable row level security;
alter table campanha               enable row level security;
alter table demanda                enable row level security;
alter table pessoa_demanda         enable row level security;
alter table tipo_atividade         enable row level security;
alter table tipo_peca              enable row level security;
alter table atividade              enable row level security;
alter table peca                   enable row level security;
alter table evidencia              enable row level security;
alter table auditoria              enable row level security;
