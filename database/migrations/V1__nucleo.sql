create extension if not exists "pgcrypto";

create or replace function set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

create table diretoria (
    id          uuid primary key default gen_random_uuid(),
    nome        varchar(120) not null unique,
    sigla       varchar(20),
    ativo       boolean not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);
create trigger trg_diretoria_updated_at before update on diretoria
for each row execute function set_updated_at();

create table area (
    id            uuid primary key default gen_random_uuid(),
    diretoria_id  uuid not null references diretoria(id),
    nome          varchar(120) not null,
    ativo         boolean not null default true,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    unique (diretoria_id, nome)
);
create trigger trg_area_updated_at before update on area
for each row execute function set_updated_at();

create table cargo (
    id          uuid primary key default gen_random_uuid(),
    nome        varchar(120) not null unique,
    descricao   text,
    ativo       boolean not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);
create trigger trg_cargo_updated_at before update on cargo
for each row execute function set_updated_at();

create table especialidade (
    id          uuid primary key default gen_random_uuid(),
    nome        varchar(120) not null unique,
    descricao   text,
    ativo       boolean not null default true,
    created_at  timestamptz not null default now()
);

create table pessoa (
    id              uuid primary key default gen_random_uuid(),
    nome            varchar(180) not null,
    email           varchar(180) unique,
    diretoria_id    uuid not null references diretoria(id),
    area_id         uuid not null references area(id),
    cargo_id        uuid references cargo(id),
    status          varchar(20) not null default 'ATIVO'
                        check (status in ('ATIVO', 'INATIVO', 'AFASTADO')),
    auth_user_id    uuid unique, -- vínculo futuro com Supabase Auth (M3)
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);
create index idx_pessoa_area on pessoa(area_id);
create index idx_pessoa_diretoria on pessoa(diretoria_id);
create trigger trg_pessoa_updated_at before update on pessoa
for each row execute function set_updated_at();

create table pessoa_especialidade (
    pessoa_id        uuid not null references pessoa(id) on delete cascade,
    especialidade_id uuid not null references especialidade(id),
    primary key (pessoa_id, especialidade_id)
);

create table pessoa_cargo_historico (
    id            uuid primary key default gen_random_uuid(),
    pessoa_id     uuid not null references pessoa(id) on delete cascade,
    cargo_id      uuid not null references cargo(id),
    data_inicio   date not null,
    data_fim      date,
    motivo        text,
    created_at    timestamptz not null default now()
);
create index idx_pessoa_cargo_hist_pessoa on pessoa_cargo_historico(pessoa_id);

create table cliente (
    id          uuid primary key default gen_random_uuid(),
    nome        varchar(180) not null,
    tipo        varchar(20) not null
                    check (tipo in ('PESSOA', 'AREA', 'UNIDADE', 'EXTERNO')),
    observacao  text,
    ativo       boolean not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);
create trigger trg_cliente_updated_at before update on cliente
for each row execute function set_updated_at();

create table projeto (
    id            uuid primary key default gen_random_uuid(),
    nome          varchar(180) not null,
    cliente_id    uuid references cliente(id),
    diretoria_id  uuid references diretoria(id),
    descricao     text,
    status        varchar(20) not null default 'ATIVO'
                      check (status in ('ATIVO', 'PAUSADO', 'ENCERRADO')),
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);
create trigger trg_projeto_updated_at before update on projeto
for each row execute function set_updated_at();

create table campanha (
    id            uuid primary key default gen_random_uuid(),
    projeto_id    uuid references projeto(id),
    codigo        varchar(40), -- ex.: BC2606CR — NÃO é chave única
    nome          varchar(180) not null,
    data_inicio   date,
    data_fim      date,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);
create index idx_campanha_codigo on campanha(codigo);
create trigger trg_campanha_updated_at before update on campanha
for each row execute function set_updated_at();

create table demanda (
    id                uuid primary key default gen_random_uuid(),
    titulo            varchar(220) not null,
    descricao         text,
    codigo            varchar(40),
    diretoria_id      uuid not null references diretoria(id),
    cliente_id        uuid references cliente(id),
    projeto_id        uuid references projeto(id),
    campanha_id       uuid references campanha(id),
    prioridade        varchar(20) not null default 'NORMAL'
                          check (prioridade in ('BAIXA', 'NORMAL', 'ALTA', 'URGENTE')),
    status            varchar(20) not null default 'NAO_INICIADA'
                          check (status in ('NAO_INICIADA', 'EM_ANDAMENTO', 'EM_APROVACAO', 'CONCLUIDA', 'CANCELADA')),
    data_criacao      date not null default current_date,
    data_prazo        date,
    data_entrega_real date,
    valor             numeric(14,2),
    observacoes       text,
    source_system     varchar(60),
    source_sheet      varchar(60),
    source_row        integer,
    imported_at       timestamptz,
    created_by        uuid references pessoa(id),
    updated_by        uuid references pessoa(id),
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now()
);
create index idx_demanda_diretoria on demanda(diretoria_id);
create index idx_demanda_status on demanda(status);
create index idx_demanda_projeto on demanda(projeto_id);
create index idx_demanda_campanha on demanda(campanha_id);
create index idx_demanda_data_prazo on demanda(data_prazo);
create trigger trg_demanda_updated_at before update on demanda
for each row execute function set_updated_at();

create table pessoa_demanda (
    id            uuid primary key default gen_random_uuid(),
    pessoa_id     uuid not null references pessoa(id),
    demanda_id    uuid not null references demanda(id) on delete cascade,
    papel         varchar(30) not null default 'PARTICIPANTE'
                      check (papel in ('RESPONSAVEL_PRINCIPAL', 'RESPONSAVEL', 'PARTICIPANTE', 'APOIO', 'REVISOR')),
    data_entrada  date not null default current_date,
    data_saida    date,
    observacao    text,
    unique (pessoa_id, demanda_id)
);
create index idx_pessoa_demanda_demanda on pessoa_demanda(demanda_id);
create index idx_pessoa_demanda_pessoa on pessoa_demanda(pessoa_id);