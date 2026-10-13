-- database/migrations/V17__pessoa-remove-cpf.sql
-- Etapa 2 de 2 da troca do CPF pela chave C (V16). O backend novo já não usa a coluna, então ela sai,
-- junto com as constraints de formato e unicidade. Apaga o CPF de quem o tinha informado.

alter table pessoa
    drop constraint if exists pessoa_cpf_formato,
    drop constraint if exists pessoa_cpf_key,
    drop column cpf;
