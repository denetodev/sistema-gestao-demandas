-- database/migrations/V19__emprestimo-foto.sql
-- Fotos opcionais do equipamento na saída e na devolução. O arquivo fica no bucket privado de evidências
-- (mesmo bucket, pasta emprestimos/); aqui só o caminho, o tipo e o tamanho.

create table emprestimo_foto (
    id               uuid primary key default gen_random_uuid(),
    emprestimo_id    uuid        not null references emprestimo(id) on delete cascade,
    momento          varchar(10) not null check (momento in ('SAIDA', 'DEVOLUCAO')),
    arquivo_path     text        not null,
    arquivo_mime     varchar(40) not null,
    arquivo_tamanho  integer     not null,
    created_by       uuid        not null references pessoa(id),
    created_at       timestamptz not null default now()
);
create index idx_emprestimo_foto_emprestimo on emprestimo_foto (emprestimo_id);

alter table emprestimo_foto enable row level security;
