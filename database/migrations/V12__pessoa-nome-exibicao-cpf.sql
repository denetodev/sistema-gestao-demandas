-- database/migrations/V12__pessoa-nome-exibicao-cpf.sql
-- Identidade da pessoa:
--   nome          = nome completo (como no cadastro da empresa)
--   nome_exibicao = como a pessoa quer ser chamada (ex.: "Carol" para Ana Caroliny Santos de Sousa)
--   cpf           = só dígitos, único. Evita a mesma pessoa duas vezes e religa quem sai e volta
--                   da empresa ao registro certo. A chave primária continua sendo o uuid: o CPF é
--                   dado pessoal (LGPD) e não deve virar chave estrangeira em tudo.
-- Os dois campos são opcionais por enquanto: quem já existe não tem CPF cadastrado.

alter table pessoa
    add column nome_exibicao varchar(80),
    add column cpf           varchar(11);

alter table pessoa
    add constraint pessoa_cpf_formato check (cpf is null or cpf ~ '^[0-9]{11}$'),
    add constraint pessoa_cpf_key unique (cpf);
