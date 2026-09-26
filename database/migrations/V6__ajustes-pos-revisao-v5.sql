-- database/migrations/V6__ajustes-pos-revisao-v5.sql
-- Ajustes identificados na revisão do V5, após conferência coluna por
-- coluna do estado real do banco.

-- 1. demandante.diretoria_id nunca chegou a ser criado no lote do V5,
--    só projeto já tinha esse campo. Nullable, no mesmo padrão de
--    projeto: filtro de conveniência na tela (mostrar só demandantes da
--    diretoria em que a demanda está sendo criada), não regra rígida.
alter table demandante add column diretoria_id uuid references diretoria(id);

-- 2. pessoa.perfil tinha virado text em algum momento (provavelmente
--    editado direto no Table Editor), destoando do padrão varchar(N)
--    usado no resto do schema para colunas equivalentes (status, tipo,
--    prioridade). Os 4 valores válidos têm no máximo 12 caracteres,
--    então a conversão de volta é segura, sem risco de truncar dado.
alter table pessoa alter column perfil type varchar(20);
