-- database/migrations/V9__relatorio-mensal.sql
-- Relatório mensal de atividades: um por pessoa e mês. A própria pessoa seleciona o
-- que entra, escreve observações e aprova; ao aprovar, o conteúdo é congelado em
-- snapshot_json (o relatório assinado não muda se atividades forem editadas depois).
-- Reabrir descarta o snapshot e volta para ABERTO.
--
-- atividades_excluidas guarda os ids (separados por vírgula) das atividades do mês que a
-- pessoa tirou do relatório. Guardar as exclusões, e não as seleções, faz atividades
-- novas do mês entrarem por padrão enquanto o relatório está aberto.

create table relatorio_mensal (
    id                   uuid primary key default gen_random_uuid(),
    pessoa_id            uuid not null references pessoa(id),
    mes                  date not null check (mes = date_trunc('month', mes)::date),
    status               varchar(20) not null default 'ABERTO'
                             check (status in ('ABERTO', 'APROVADO')),
    observacoes          text,
    atividades_excluidas text not null default '',
    snapshot_json        text,
    aprovado_em          timestamptz,
    created_at           timestamptz not null default now(),
    updated_at           timestamptz not null default now(),
    unique (pessoa_id, mes),
    check ((status = 'APROVADO') = (aprovado_em is not null and snapshot_json is not null))
);
create index idx_relatorio_mensal_mes on relatorio_mensal(mes);

-- RLS ligado e sem policies: o relatório é acessado só pela API (usuário dono do banco,
-- que ignora RLS). Sem isso, a anon key do frontend leria e gravaria a tabela via PostgREST.
alter table relatorio_mensal enable row level security;
create trigger trg_relatorio_mensal_updated_at before update on relatorio_mensal
for each row execute function set_updated_at();
