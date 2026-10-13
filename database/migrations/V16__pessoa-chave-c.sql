-- database/migrations/V16__pessoa-chave-c.sql
-- A identidade para vincular conta ao cadastro importado passa a ser a chave C (matrícula), não o CPF.
--   chave_c = letra C (contratado) ou F (funcionário do Banco) seguida de 7 dígitos, sempre maiúscula, única.
-- Etapa 1 de 2: só adiciona a coluna e preenche quem já foi informado. O backend antigo ignora a coluna, então
-- esta migration pode ser aplicada antes do deploy. A coluna cpf sai na V17, depois que o backend novo estiver no ar.

alter table pessoa
    add column chave_c varchar(8);

alter table pessoa
    add constraint pessoa_chave_c_formato check (chave_c is null or chave_c ~ '^[CF][0-9]{7}$'),
    add constraint pessoa_chave_c_key unique (chave_c);

update pessoa set chave_c = 'C1349864' where id = 'e7bb90d9-7b9b-475e-aa32-3b215e000ee4'; -- Deusdete Dias Torres Neto
update pessoa set chave_c = 'C1349865' where id = '3c4d6a50-9c9e-4821-856d-093d8d88e6b4'; -- Ana Caroliny Santos de Sousa
update pessoa set chave_c = 'C1349844' where id = 'd504facd-9ebc-47fa-a16c-43242ae5643b'; -- Thaís Trindade Caldas
