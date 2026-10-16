-- database/migrations/V20__emprestimo-retirada-prevista.sql
-- Reserva por período: o empréstimo passa a ter data de retirada prevista, além da devolução prevista.
-- Um equipamento só pode estar em um período por vez (individual) ou até a quantidade total (por lote).
-- Quem já existe ganha como retirada a data real da entrega, ou a do registro, quando ainda não saiu.

alter table emprestimo
    add column retirada_prevista timestamptz;

update emprestimo set retirada_prevista = coalesce(retirado_em, created_at);

-- padrão now(): o backend anterior a esta migration (que não conhece a coluna) continua registrando empréstimo
-- como retirada imediata, sem quebrar, até o deploy do novo
alter table emprestimo
    alter column retirada_prevista set default now(),
    alter column retirada_prevista set not null;

alter table emprestimo
    add constraint emprestimo_periodo_valido check (prevista_devolucao > retirada_prevista);

create index idx_emprestimo_retirada_prevista on emprestimo (retirada_prevista);
