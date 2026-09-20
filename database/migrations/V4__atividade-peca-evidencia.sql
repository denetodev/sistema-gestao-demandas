-- database/migrations/V4__atividade-peca-evidencia.sql
-- Frente C: entidades de domínio Atividade, Peça, Evidência.
-- Aplicado via SQL Editor do Supabase, documentado aqui no mesmo lote
-- de commit que introduz o código Java correspondente.

create table tipo_atividade (
    id          uuid primary key default gen_random_uuid(),
    nome        varchar(120) not null unique,
    descricao   text,
    ativo       boolean not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);
create trigger trg_tipo_atividade_updated_at before update on tipo_atividade
for each row execute function set_updated_at();

create table tipo_peca (
    id          uuid primary key default gen_random_uuid(),
    nome        varchar(120) not null unique,
    descricao   text,
    ativo       boolean not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);
create trigger trg_tipo_peca_updated_at before update on tipo_peca
for each row execute function set_updated_at();

create table atividade (
    id                uuid primary key default gen_random_uuid(),
    demanda_id        uuid references demanda(id),
    tipo_atividade_id uuid not null references tipo_atividade(id),
    pessoa_id         uuid references pessoa(id),
    descricao         text,
    data_realizacao   date not null default current_date,
    created_by        uuid references pessoa(id),
    updated_by        uuid references pessoa(id),
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now()
);
create index idx_atividade_demanda on atividade(demanda_id);
create index idx_atividade_pessoa on atividade(pessoa_id);
create index idx_atividade_data on atividade(data_realizacao);
create trigger trg_atividade_updated_at before update on atividade
for each row execute function set_updated_at();

create table peca (
    id           uuid primary key default gen_random_uuid(),
    demanda_id   uuid not null references demanda(id),
    tipo_peca_id uuid not null references tipo_peca(id),
    nome         varchar(180) not null,
    descricao    text,
    created_by   uuid references pessoa(id),
    updated_by   uuid references pessoa(id),
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);
create index idx_peca_demanda on peca(demanda_id);
create trigger trg_peca_updated_at before update on peca
for each row execute function set_updated_at();

create table evidencia (
    id           uuid primary key default gen_random_uuid(),
    atividade_id uuid references atividade(id) on delete cascade,
    peca_id      uuid references peca(id) on delete cascade,
    tipo         varchar(30) not null
                     check (tipo in ('IMAGEM','SCREENSHOT','HTML','PDF','VIDEO','ARQUIVO','LINK','CODIGO','MOCKUP','CAPTURA_SISTEMA','OBSERVACAO','CONFIRMACAO_MANUAL')),
    conteudo     text,
    descricao    text,
    created_by   uuid references pessoa(id),
    created_at   timestamptz not null default now(),
    check (atividade_id is not null or peca_id is not null)
);
create index idx_evidencia_atividade on evidencia(atividade_id);
create index idx_evidencia_peca on evidencia(peca_id);