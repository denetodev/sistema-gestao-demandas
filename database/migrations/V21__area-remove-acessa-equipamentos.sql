-- O acesso ao modulo de equipamentos passou a ser por papel (pessoa.papel_equipamentos). A coluna por equipe nao e mais lida
-- pelo backend desde o deploy do PR #48.
alter table area drop column if exists acessa_equipamentos;
