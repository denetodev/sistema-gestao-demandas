-- Nova camada da hierarquia: diretoria > núcleo > equipe (área) > cargo.
create table nucleo (
    id           uuid primary key default gen_random_uuid(),
    diretoria_id uuid not null references diretoria (id),
    nome         varchar(120) not null,
    ativo        boolean not null default true,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    unique (diretoria_id, nome)
);
alter table nucleo enable row level security;

alter table area add column nucleo_id uuid references nucleo (id);
create index idx_area_nucleo on area (nucleo_id);

-- Ponto de partida: House-CRM na diretoria CRM e Multimídia na UGR. Novos núcleos e a divisão das equipes
-- da UGR são cadastrados pela tela de Núcleos.
insert into nucleo (diretoria_id, nome)
select d.id, 'House-CRM' from diretoria d where d.sigla = 'CRM'
union all
select d.id, 'Multimídia' from diretoria d where d.sigla = 'UGR';

update area a set nucleo_id = n.id
from nucleo n
where n.diretoria_id = a.diretoria_id;

create trigger trg_nucleo_updated_at before update on nucleo
for each row execute function set_updated_at();
