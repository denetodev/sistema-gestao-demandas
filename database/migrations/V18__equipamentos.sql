-- database/migrations/V18__equipamentos.sql
-- Módulo de equipamentos: inventário, empréstimo (retirada e devolução), autorização e termo de responsabilidade.
--   area.acessa_equipamentos     : a equipe inteira entra no módulo (ex.: Videomaker, Operador)
--   pessoa.papel_equipamentos    : papel dentro do módulo (operador registra, gestor de equipamentos autoriza e edita o inventário)
--   categoria_equipamento.controle: INDIVIDUAL (uma peça física por registro) ou QUANTIDADE (lote)
--   emprestimo                   : todo empréstimo passa por autorização; externo (sai do prédio) ganha destaque
--   termo_equipamento            : texto curto do termo, versionado; cada empréstimo guarda a versão aceita

alter table area
    add column acessa_equipamentos boolean not null default false;

alter table pessoa
    add column papel_equipamentos varchar(30)
        check (papel_equipamentos is null or papel_equipamentos in ('OPERADOR_DE_ESTUDIO', 'GESTOR_DE_EQUIPAMENTOS'));

create table categoria_equipamento (
    id         uuid primary key default gen_random_uuid(),
    nome       varchar(80)  not null unique,
    controle   varchar(12)  not null check (controle in ('INDIVIDUAL', 'QUANTIDADE')),
    ativo      boolean      not null default true,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now()
);

create table equipamento (
    id               uuid primary key default gen_random_uuid(),
    categoria_id     uuid         not null references categoria_equipamento(id),
    nome             varchar(160) not null,
    patrimonio       varchar(60)  unique,
    numero_serie     varchar(80),
    quantidade_total integer      not null default 1 check (quantidade_total > 0),
    localizacao      varchar(120),
    observacoes      text,
    ativo            boolean      not null default true,
    created_at       timestamptz  not null default now(),
    updated_at       timestamptz  not null default now()
);
create index idx_equipamento_categoria on equipamento (categoria_id);

create table termo_equipamento (
    versao     integer primary key,
    texto      text        not null,
    criado_por uuid        references pessoa(id),
    created_at timestamptz not null default now()
);
insert into termo_equipamento (versao, texto) values
    (1, 'Declaro que recebi os equipamentos listados, sou responsável por eles e me comprometo a devolvê-los na data prevista, nas mesmas condições.');

create table emprestimo (
    id                     uuid primary key default gen_random_uuid(),
    retirante_nome         varchar(160) not null,
    retirante_chave_c      varchar(8)   check (retirante_chave_c is null or retirante_chave_c ~ '^[CF][0-9]{7}$'),
    retirante_contato      varchar(160),
    retirante_pessoa_id    uuid         references pessoa(id),
    finalidade             text,
    externo                boolean      not null default false,
    local_uso              varchar(160),
    prevista_devolucao     timestamptz  not null,
    status                 varchar(24)  not null default 'AGUARDANDO_AUTORIZACAO'
        check (status in ('AGUARDANDO_AUTORIZACAO', 'AUTORIZADO', 'RECUSADO', 'RETIRADO', 'DEVOLVIDO', 'CANCELADO')),
    demanda_id             uuid         references demanda(id),
    atividade_id           uuid         references atividade(id),
    registrado_por         uuid         not null references pessoa(id),
    termo_versao           integer      not null references termo_equipamento(versao),
    termo_aceito_em        timestamptz  not null default now(),
    autorizado_por         uuid         references pessoa(id),
    autorizado_em          timestamptz,
    motivo_recusa          text,
    retirado_por           uuid         references pessoa(id),
    retirado_em            timestamptz,
    devolvido_por          uuid         references pessoa(id),
    devolvido_em           timestamptz,
    observacoes_devolucao  text,
    created_at             timestamptz  not null default now(),
    updated_at             timestamptz  not null default now()
);
create index idx_emprestimo_status on emprestimo (status);
create index idx_emprestimo_prevista on emprestimo (prevista_devolucao);

create table emprestimo_item (
    id              uuid primary key default gen_random_uuid(),
    emprestimo_id   uuid    not null references emprestimo(id) on delete cascade,
    equipamento_id  uuid    not null references equipamento(id),
    quantidade      integer not null default 1 check (quantidade > 0),
    devolvido_em    timestamptz,
    unique (emprestimo_id, equipamento_id)
);
create index idx_emprestimo_item_equipamento on emprestimo_item (equipamento_id);

-- RLS ligada como nas demais tabelas (o backend acessa como dono do banco; a Data API está desligada)
alter table categoria_equipamento enable row level security;
alter table equipamento           enable row level security;
alter table termo_equipamento     enable row level security;
alter table emprestimo            enable row level security;
alter table emprestimo_item       enable row level security;
