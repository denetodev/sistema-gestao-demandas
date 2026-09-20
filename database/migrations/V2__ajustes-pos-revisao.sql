-- Ajustes de modelagem pós-revisão do núcleo (V1), aplicados diretamente
-- no SQL Editor do Supabase e documentados aqui de forma retroativa.

-- 1. Remove diretoria_id de pessoa: redundante, já que area já referencia
--    diretoria_id — a diretoria de uma pessoa deve ser sempre derivada
--    via join (pessoa -> area -> diretoria), evitando os dois campos
--    divergirem entre si.
alter table pessoa
    drop column diretoria_id;

-- 2. Rastreabilidade de origem: nome do arquivo fonte na migração
--    histórica (ex.: DEMANDAS_HOUSE_CRM_2026.xlsx vs.
--    DEMANDAS_HOUSE_CRMdf_2026.xlsx — necessário pra saber de qual
--    planilha cada registro importado veio).
alter table demanda
    add column source_file varchar(180);

-- 3. pessoa_demanda: permite reentrada da mesma pessoa numa demanda
--    reaberta (cenário real da operação), mas nunca duas participações
--    ativas simultâneas do mesmo par (pessoa, demanda). Substitui o
--    unique constraint original por um índice único parcial, ativo só
--    enquanto data_saida for nula.
alter table pessoa_demanda
    drop constraint pessoa_demanda_pessoa_id_demanda_id_key;

create unique index ux_pessoa_demanda_ativa
    on pessoa_demanda (pessoa_id, demanda_id)
    where data_saida is null;