-- database/migrations/V15__pessoa-vinculo-sugerido.sql
-- Vínculo de conta nova ao cadastro importado, pelo CPF.
--
-- Quando alguém cria conta e informa o CPF de uma pessoa que já existe sem login (importada da planilha),
-- o sistema NÃO grava o login nesse cadastro. Cria uma conta pendente normal (sem aprovação, sem histórico,
-- sem nenhuma permissão) e anota aqui qual cadastro foi sugerido. Só a aprovação de um Gestor ou Admin
-- move o login para o cadastro importado (e apaga a conta pendente). Veja PessoaService.confirmarVinculo.
alter table pessoa
    add column vinculo_sugerido_id uuid references pessoa (id) on delete set null;

create index idx_pessoa_vinculo_sugerido on pessoa (vinculo_sugerido_id) where vinculo_sugerido_id is not null;
