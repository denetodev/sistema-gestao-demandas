-- Notificações da pessoa (sininho no topo).
-- pessoa.notificacoes_vistas_em: até quando a pessoa já viu os avisos de "te adicionaram numa demanda".
-- pessoa_demanda.adicionado_por e adicionado_em: quem colocou a pessoa no time e quando. Linhas antigas (importadas)
-- ficam nulas e nunca geram aviso.
alter table pessoa
    add column notificacoes_vistas_em timestamptz;

alter table pessoa_demanda
    add column adicionado_por uuid references pessoa (id) on delete set null,
    add column adicionado_em  timestamptz;
