-- database/migrations/V8__pessoa-status-rejeitado.sql
-- Estado REJEITADO no status de Pessoa, suportando o fluxo de recusar
-- um cadastro pendente (PATCH /pessoas/{id}/rejeitar), distinto de
-- desativar (que serve pra quem já foi aprovado).
-- Aplicado via SQL Editor, documentado aqui de forma retroativa.

alter table pessoa drop constraint pessoa_status_check;
alter table pessoa
    add constraint pessoa_status_check
    check (status in ('ATIVO', 'INATIVO', 'AFASTADO', 'REJEITADO'));