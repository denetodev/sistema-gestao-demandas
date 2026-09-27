-- database/migrations/V5__demandante-referencia-auditoria.sql
-- Rename Cliente -> Demandante, suporte a auto-cadastro (Referência de
-- Equipe, aprovação de pessoa), valor de mercado em Tipo de Peça e Peça,
-- link externo em Demanda, e tabela genérica de auditoria.
-- Aplicado em lote direto no SQL Editor do Supabase, documentado aqui
-- de forma retroativa após conferência coluna por coluna do estado real
-- do banco.

-- 1. Rename cliente -> demandante
alter table cliente rename to demandante;
alter table demandante rename constraint cliente_pkey to demandante_pkey;
alter table demandante rename constraint cliente_tipo_check to demandante_tipo_check;
alter trigger trg_cliente_updated_at on demandante rename to trg_demandante_updated_at;

alter table demanda rename column cliente_id to demandante_id;
alter table demanda rename constraint demanda_cliente_id_fkey to demanda_demandante_id_fkey;
create index idx_demanda_demandante on demanda(demandante_id);

alter table projeto rename column cliente_id to demandante_id;
alter table projeto rename constraint projeto_cliente_id_fkey to projeto_demandante_id_fkey;
create index idx_projeto_demandante on projeto(demandante_id);

-- 2. Referência de Equipe: profissional que referencia uma área, não é
--    perfil nem chefia. Uma área pode ter mais de uma pessoa como
--    referência ao mesmo tempo, por isso sem índice único.
alter table pessoa add column referencia_area_id uuid references area(id);

-- 3. Auto-cadastro: a pessoa se cadastra e fica pendente até Gestor ou
--    Admin aprovar. O status (ATIVO/INATIVO/AFASTADO) permanece só para
--    ciclo de vida operacional; a aprovação é rastreada separadamente,
--    quem aprovou e quando, em vez de virar um valor novo nesse CHECK.
alter table pessoa
    add column aprovado_em timestamptz,
    add column aprovado_por uuid references pessoa(id);

-- 4. Tipo de Peça passa a pertencer a uma área (o mesmo nome pode
--    existir em áreas diferentes, ex.: Vinheta em Designers e Motion) e
--    ganha valor de referência de mercado, com base na tabela
--    Sinapro/Adegraf usada na planilha histórica.
alter table tipo_peca add column area_id uuid not null references area(id);
alter table tipo_peca add column valor_referencia numeric(12,2) not null default 0;

alter table tipo_peca drop constraint tipo_peca_nome_key;
alter table tipo_peca add constraint tipo_peca_area_id_nome_key unique (area_id, nome);

-- 5. Peça ganha quantidade, valor unitário (cópia do valor do tipo no
--    momento do lançamento, para não mudar meses já fechados), pessoa
--    produtora e data de entrega. O dashboard agrega por pessoa e mês,
--    daí os dois índices.
alter table peca
    add column quantidade integer not null default 1 check (quantidade > 0),
    add column valor_unitario numeric(12,2) not null,
    add column pessoa_id uuid references pessoa(id),
    add column data_entrega date;

create index idx_peca_pessoa on peca(pessoa_id);
create index idx_peca_data_entrega on peca(data_entrega);

-- 6. Link externo (Planner/ClickUp) como ponte simples até a integração
--    real ser construída, prevista para a Fase 5.
alter table demanda add column link_externo varchar(500);

-- 7. Auditoria genérica: campo, valor anterior, valor novo, quem alterou,
--    quando e o motivo. Uso inicial é a aprovação de cadastro (valor
--    informado pela pessoa x valor aprovado pelo Gestor/Admin), com
--    estrutura aberta o bastante para outras entidades no futuro.
create table auditoria (
    id             uuid primary key default gen_random_uuid(),
    tabela         varchar(60) not null,
    registro_id    uuid not null,
    campo          varchar(60) not null,
    valor_anterior text,
    valor_novo     text,
    alterado_por   uuid references pessoa(id),
    motivo         text,
    created_at     timestamptz not null default now()
);
create index idx_auditoria_tabela_registro on auditoria (tabela, registro_id);
