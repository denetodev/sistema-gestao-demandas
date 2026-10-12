-- Data em que a pessoa foi inativada, para o relatório mensal listar só quem estava ativo no mês.
alter table pessoa add column inativado_em date;

-- Pessoas já inativas: melhor estimativa disponível é a última atualização do cadastro.
update pessoa set inativado_em = updated_at::date where status = 'INATIVO';
