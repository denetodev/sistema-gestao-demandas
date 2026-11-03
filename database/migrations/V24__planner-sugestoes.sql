-- Integração com o Planner da Microsoft.
-- pessoa.etiqueta_planner: o texto exato da etiqueta da pessoa no plano (ex.: "CRIATIVO - Thais", "HTML - Carol").
--   Quando o Planner coloca essa etiqueta numa tarefa, a pessoa recebe uma sugestão para aceitar ou recusar.
-- planner_sugestao: uma sugestão por tarefa do Planner e por pessoa. Nunca é recriada depois de decidida.
--   Aceitar cria a demanda (ou coloca a pessoa na que já existe com o mesmo código) e liga a sugestão a ela.

alter table pessoa
    add column etiqueta_planner varchar(80);

create unique index pessoa_etiqueta_planner_key on pessoa (lower(etiqueta_planner))
    where etiqueta_planner is not null;

create table planner_sugestao (
    id                 uuid primary key default gen_random_uuid(),
    plan_id            varchar(64)  not null,
    tarefa_id          varchar(64)  not null,
    pessoa_id          uuid         not null references pessoa(id) on delete cascade,
    codigo             varchar(40),
    titulo             varchar(500) not null,
    bucket             varchar(120),
    etiqueta           varchar(80)  not null,
    criada_no_planner  timestamptz,
    status             varchar(10)  not null default 'PENDENTE' check (status in ('PENDENTE', 'ACEITA', 'RECUSADA')),
    demanda_id         uuid references demanda(id) on delete set null,
    decidida_em        timestamptz,
    created_at         timestamptz  not null default now(),
    unique (tarefa_id, pessoa_id)
);

create index idx_planner_sugestao_pessoa_status on planner_sugestao (pessoa_id, status);
create index idx_planner_sugestao_codigo on planner_sugestao (lower(codigo));

alter table planner_sugestao enable row level security;
